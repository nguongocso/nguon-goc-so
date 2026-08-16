package vn.nguongocso.auth.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.LoginAttempt;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.AnomalyReasonCode;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.auth.enums.LoginResult;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.LoginAnomalyRepository;
import vn.nguongocso.auth.repository.LoginAttemptRepository;
import vn.nguongocso.auth.service.LoginAnomalyDetectionService;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

/**
 * Triển khai service phát hiện bất thường đăng nhập.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoginAnomalyDetectionServiceImpl implements LoginAnomalyDetectionService {
    
    private static final int FAILED_LOGIN_THRESHOLD = 5;
    private static final long FAILED_LOGIN_WINDOW_MINUTES = 2;
    
    private final LoginAttemptRepository loginAttemptRepository;
    private final LoginAnomalyRepository loginAnomalyRepository;
    private final AccountLockRepository accountLockRepository;
    private final NotificationService notificationService;
    private final OrganizationUserRepository organizationUserRepository;
    
    /**
     * Ghi nhận một lần đăng nhập và kiểm tra bất thường.
     */
    @Override
    public void recordLoginAttempt(
        User user,
        String usernameInput,
        boolean isSuccess,
        String ipAddress,
        String countryCode
    ) {
        try {
            // Ghi nhận đăng nhập
            LoginAttempt attempt = LoginAttempt.builder()
                .user(user)
                .usernameInput(usernameInput)
                .result(isSuccess ? LoginResult.SUCCESS : LoginResult.FAILED)
                .ipAddress(ipAddress)
                .countryCode(countryCode)
                .isNewCountry(false)
                .createdAt(OffsetDateTime.now())
                .build();
            
            loginAttemptRepository.save(attempt);
            log.info(
                "Ghi nhận lần đăng nhập: user={}, result={}, ipAddress={}, countryCode={}",
                usernameInput,
                isSuccess ? "SUCCESS" : "FAILED",
                ipAddress,
                countryCode
            );
            
            // Nếu đăng nhập thành công, kiểm tra xem có phải quốc gia lạ không
            if (isSuccess && user != null) {
                checkUnusualCountry(user, ipAddress, countryCode);
            }
            
            // Nếu đăng nhập thất bại, kiểm tra xem có bao nhiêu lần sai liên tiếp
            if (!isSuccess && user != null) {
                checkRepeatedFailedLogin(user, ipAddress, countryCode);
            }
            
        } catch (Exception e) {
            log.error("Lỗi khi ghi nhận lần đăng nhập: username={}, ipAddress={}", 
                usernameInput, ipAddress, e);
        }
    }
    
    /**
     * Kiểm tra xem có sai mật khẩu liên tiếp ≥ 5 lần trong 2 phút không.
     */
    private void checkRepeatedFailedLogin(User user, String ipAddress, String countryCode) {
        OffsetDateTime twoMinutesAgo = OffsetDateTime.now()
            .minusMinutes(FAILED_LOGIN_WINDOW_MINUTES);
        
        List<LoginAttempt> recentFailedAttempts = loginAttemptRepository
            .findByUser_UserIdAndResultAndCreatedAtAfterOrderByCreatedAtDesc(
                user.getUserId(),
                LoginResult.FAILED,
                twoMinutesAgo
            );
        
        if (recentFailedAttempts.size() >= FAILED_LOGIN_THRESHOLD) {
            log.warn(
                "Phát hiện {} lần sai mật khẩu liên tiếp trong 2 phút. userId={}, username={}, ipAddress={}",
                recentFailedAttempts.size(),
                user.getUserId(),
                user.getUserName(),
                ipAddress
            );
            
            createAnomaly(
                user,
                AnomalyReasonCode.REPEATED_FAILED_LOGIN,
                recentFailedAttempts.size(),
                ipAddress,
                countryCode
            );
        }
    }
    
    /**
     * Kiểm tra xem đăng nhập từ quốc gia chưa từng ghi nhận SUCCESS không.
     */
    private void checkUnusualCountry(User user, String ipAddress, String countryCode) {
        if (countryCode == null) {
            return;
        }
        
        boolean existsSuccessfulLoginFromThisCountry = loginAttemptRepository
            .existsByUser_UserIdAndResultAndCountryCode(
                user.getUserId(),
                LoginResult.SUCCESS,
                countryCode
            );
        
        // Nếu chưa từng đăng nhập thành công từ quốc gia này, tạo bản ghi bất thường
        if (!existsSuccessfulLoginFromThisCountry) {
            // Kiểm tra xem này có phải lần đầu tiên hay không
            var firstLoginPage = loginAttemptRepository
                .findByUser_UserIdOrderByCreatedAtDesc(
                    user.getUserId(),
                    PageRequest.of(0, 1)
                );
            
            // Chỉ cảnh báo nếu user đã từng đăng nhập thành công trước đó
            // (tức là không phải lần đầu tiên)
            if (!firstLoginPage.isEmpty()) {
                log.warn(
                    "Phát hiện đăng nhập từ quốc gia lạ. userId={}, username={}, countryCode={}, ipAddress={}",
                    user.getUserId(),
                    user.getUserName(),
                    countryCode,
                    ipAddress
                );
                
                createAnomaly(
                    user,
                    AnomalyReasonCode.UNUSUAL_COUNTRY,
                    null,
                    ipAddress,
                    countryCode
                );
            }
        }
    }
    
    /**
     * Tạo bản ghi bất thường và gửi thông báo.
     */
    private void createAnomaly(
        User user,
        AnomalyReasonCode reasonCode,
        Integer attemptCount,
        String ipAddress,
        String countryCode
    ) {
        try {
            // Lấy organization từ OrganizationUser
            Organization organization = organizationUserRepository
                .findFirstByUser(user)
                .map(ou -> ou.getOrganization())
                .orElse(null);
            
            if (organization == null) {
                log.warn("Không thể tìm thấy organization cho user: userId={}", user.getUserId());
                return;
            }
            
            LoginAnomaly anomaly = LoginAnomaly.builder()
                .user(user)
                .organization(organization)
                .reasonCode(reasonCode)
                .attemptCount(attemptCount)
                .ipAddress(ipAddress)
                .countryCode(countryCode)
                .detectedAt(OffsetDateTime.now())
                .status(AnomalyStatus.OPEN)
                .build();
            
            anomaly = loginAnomalyRepository.save(anomaly);
            
            log.info(
                "Bản ghi bất thường đã được tạo. anomalyId={}, userId={}, reasonCode={}, organizationId={}",
                anomaly.getId(),
                user.getUserId(),
                reasonCode,
                organization.getOrganizationId()
            );
            
            // TODO: Gửi thông báo cho admin và quản lý tổ chức qua NotificationService
            // notificationService.sendLoginAnomalyNotification(anomaly);
            
        } catch (Exception e) {
            log.error(
                "Lỗi khi tạo bản ghi bất thường: userId={}, reasonCode={}",
                user.getUserId(),
                reasonCode,
                e
            );
        }
    }
    
    /**
     * Kiểm tra xem tài khoản có đang bị khóa không.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isAccountLocked(UUID userId) {
        return accountLockRepository
            .existsByUser_UserIdAndStatus(
                userId,
                AccountLockStatus.LOCKED
            );
    }
}
