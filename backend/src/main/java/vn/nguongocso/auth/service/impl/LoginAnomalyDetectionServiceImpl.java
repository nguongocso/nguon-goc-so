package vn.nguongocso.auth.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.LoginAttempt;
import vn.nguongocso.auth.entity.SuspiciousCase;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.AnomalyReasonCode;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.auth.enums.LoginResult;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.LoginAnomalyRepository;
import vn.nguongocso.auth.repository.LoginAttemptRepository;
import vn.nguongocso.auth.repository.SuspiciousCaseRepository;
import vn.nguongocso.auth.repository.UserRepository;
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
    private final SuspiciousCaseRepository suspiciousCaseRepository;
    private final AccountLockRepository accountLockRepository;
    private final UserRepository userRepository;
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
            if (user == null) {
                log.warn("Bỏ qua ghi nhận lần đăng nhập do user null. usernameInput={}, ipAddress={}", usernameInput, ipAddress);
                return;
            }

            boolean shouldCreateUnusualLocationAnomaly = false;
            if (isSuccess) {
                shouldCreateUnusualLocationAnomaly = isUnusualLocation(user, ipAddress, countryCode);
            }

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
            
            // Nếu đăng nhập thành công, kiểm tra IP hoặc quốc gia mới
            if (shouldCreateUnusualLocationAnomaly) {
                createUnusualLocationAnomaly(user, ipAddress, countryCode);
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

            // Kiểm tra xem đã có AccountLock nào ở trạng thái LOCKED chưa
            boolean alreadyLocked = accountLockRepository
                .existsByUser_UserIdAndStatus(
                    user.getUserId(),
                    AccountLockStatus.LOCKED
                );

            if (!alreadyLocked) {
                user.setStatus(UserStatus.INACTIVE);
                userRepository.save(user);

                AccountLock lock = AccountLock.builder()
                    .user(user)
                    .lockedBy(user)
                    .lockReason("REPEATED_FAILED_LOGIN")
                    .lockedAt(OffsetDateTime.now())
                    .status(AccountLockStatus.LOCKED)
                    .build();
                accountLockRepository.save(lock);
            }

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
    private boolean isUnusualLocation(
        User user,
        String ipAddress,
        String countryCode
    ) {
        long previousSuccessfulLoginCount = loginAttemptRepository.countByUser_UserIdAndResult(
            user.getUserId(),
            LoginResult.SUCCESS
        );

        if (previousSuccessfulLoginCount == 0) {
            return false;
        }

        boolean knownIp = loginAttemptRepository.existsByUser_UserIdAndResultAndIpAddress(
            user.getUserId(),
            LoginResult.SUCCESS,
            ipAddress
        );
        boolean knownCountry = countryCode != null
            && loginAttemptRepository.existsByUser_UserIdAndResultAndCountryCode(
                user.getUserId(),
                LoginResult.SUCCESS,
                countryCode
            );

        return !knownIp || (countryCode != null && !knownCountry);
    }

    private void createUnusualLocationAnomaly(User user, String ipAddress, String countryCode) {
        log.warn(
            "Phát hiện đăng nhập từ vị trí mới. userId={}, username={}, countryCode={}, ipAddress={}",
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
            if (anomaly == null) {
                log.warn(
                    "Không thể lưu bản ghi bất thường do repository trả về null. userId={}, reasonCode={}",
                    user.getUserId(),
                    reasonCode
                );
                return;
            }
            
            log.info(
                "Bản ghi bất thường đã được tạo. anomalyId={}, userId={}, reasonCode={}, organizationId={}",
                anomaly.getId(),
                user.getUserId(),
                reasonCode,
                organization.getOrganizationId()
            );

            notificationService.sendLoginAnomalyNotification(anomaly);
            triggerSuspiciousCaseIfThresholdMet(user, organization, anomaly);
            
        } catch (Exception e) {
            log.error(
                "Lỗi khi tạo bản ghi bất thường: userId={}, reasonCode={}",
                user.getUserId(),
                reasonCode,
                e
            );
        }
    }

    private void triggerSuspiciousCaseIfThresholdMet(
        User user,
        Organization organization,
        LoginAnomaly latestAnomaly
    ) {
        List<SuspiciousCase> userCases = suspiciousCaseRepository
            .findByUser_UserIdOrderByLastDetectedAtDesc(user.getUserId());

        SuspiciousCase latestCase = userCases.isEmpty() ? null : userCases.get(0);
        OffsetDateTime effectiveWindowStart = latestAnomaly.getDetectedAt().minusHours(24);

        if (latestCase != null && latestCase.getStatus() == AnomalyStatus.DISMISSED) {
            effectiveWindowStart = latestCase.getLastDetectedAt().plusSeconds(1);
        }

        List<LoginAnomaly> recentAnomalies = loginAnomalyRepository
            .findByUser_UserIdAndDetectedAtAfterOrderByDetectedAtDesc(
                user.getUserId(),
                effectiveWindowStart
            );

        if (recentAnomalies.size() < 5) {
            return;
        }

        boolean hasOpenCaseInWindow = suspiciousCaseRepository
            .existsByUser_UserIdAndStatusAndLastDetectedAtAfter(
                user.getUserId(),
                AnomalyStatus.OPEN,
                effectiveWindowStart
            );

        if (hasOpenCaseInWindow) {
            return;
        }

        SuspiciousCase suspiciousCase = SuspiciousCase.builder()
            .user(user)
            .organization(organization)
            .status(AnomalyStatus.OPEN)
            .anomalyCount(recentAnomalies.size())
            .firstDetectedAt(recentAnomalies.get(recentAnomalies.size() - 1).getDetectedAt())
            .lastDetectedAt(latestAnomaly.getDetectedAt())
            .createdAt(OffsetDateTime.now())
            .build();

        suspiciousCaseRepository.save(suspiciousCase);
        log.info(
            "Tạo case nghi vấn mới. userId={}, anomalyCount={}, firstDetectedAt={}, lastDetectedAt={}",
            user.getUserId(),
            suspiciousCase.getAnomalyCount(),
            suspiciousCase.getFirstDetectedAt(),
            suspiciousCase.getLastDetectedAt()
        );
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
