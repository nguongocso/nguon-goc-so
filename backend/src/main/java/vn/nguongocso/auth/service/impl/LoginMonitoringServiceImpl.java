package vn.nguongocso.auth.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.dto.response.AccountLockResponse;
import vn.nguongocso.auth.dto.response.LoginAnomalyResponse;
import vn.nguongocso.auth.dto.response.LoginHistoryResponse;
import vn.nguongocso.auth.dto.response.SuspiciousCaseResponse;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.LoginAttempt;
import vn.nguongocso.auth.entity.SuspiciousCase;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.auth.enums.LoginResult;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.LoginAnomalyRepository;
import vn.nguongocso.auth.repository.LoginAttemptRepository;
import vn.nguongocso.auth.repository.SuspiciousCaseRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.AccountLockService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.LoginMonitoringService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.permission.service.PermissionChecker;

/**
 * Triển khai service giám sát đăng nhập bất thường.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoginMonitoringServiceImpl implements LoginMonitoringService {
    
    private static final String AUTH_SECURITY_RESOURCE = "auth:security";
    private static final String READ_ACTION = "READ";
    private static final String LOCK_ACTION = "LOCK";
    private static final String UNLOCK_ACTION = "UNLOCK";
    private static final String VT_01 = "VT-01"; // Admin
    private static final String VT_02 = "VT-02"; // Quản lý tổ chức
    
    private final LoginAttemptRepository loginAttemptRepository;
    private final LoginAnomalyRepository loginAnomalyRepository;
    private final SuspiciousCaseRepository suspiciousCaseRepository;
    private final AccountLockRepository accountLockRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final AccountLockService accountLockService;
    private final PermissionChecker permissionChecker;
    
    /**
     * Lấy lịch sử đăng nhập.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoginHistoryResponse> getLoginHistory(
        UUID userId,
        String result,
        UUID organizationId,
        String startDate,
        String endDate,
        Pageable pageable
    ) {
        // Lấy thông tin người dùng hiện tại
        CustomUserDetails currentUser = getCurrentUser();
        UUID currentUserId = currentUser.getUserId();
        
        // Nếu không phải admin, chỉ cho phép xem lịch sử của chính mình
        if (!RoleCode.ADMIN.equals(currentUser.getRoleCode())) {
            userId = currentUserId;
        }
        // Nếu là admin, có thể xem lịch sử của tất cả (userId có thể là null để xem tất cả)

        Page<LoginAttempt> attemptsPage;

        if (userId != null) {
            attemptsPage = loginAttemptRepository.findByUser_UserIdOrderByCreatedAtDesc(
                userId,
                pageable
            );
        } else {
            attemptsPage = loginAttemptRepository.findAll(pageable);
        }

        if (attemptsPage == null) {
            attemptsPage = new PageImpl<>(List.of(), pageable, 0L);
        }

        List<LoginHistoryResponse> items = attemptsPage.getContent().stream()
            .filter(attempt -> result == null || attempt.getResult().name().equalsIgnoreCase(result))
            .filter(attempt -> startDate == null || !attempt.getCreatedAt().toLocalDate().isBefore(LocalDate.parse(startDate)))
            .filter(attempt -> endDate == null || !attempt.getCreatedAt().toLocalDate().isAfter(LocalDate.parse(endDate)))
            .map(attempt -> {
                String roleCode = attempt.getUser() != null
                    ? organizationUserRepository.findFirstByUser(attempt.getUser())
                        .map(orgUser -> orgUser.getRole() != null ? orgUser.getRole().getCode() : null)
                        .orElse(null)
                    : null;

                return LoginHistoryResponse.builder()
                    .id(attempt.getId())
                    .userId(attempt.getUser() != null ? attempt.getUser().getUserId() : null)
                    .usernameInput(attempt.getUsernameInput())
                    .roleCode(roleCode)
                    .result(attempt.getResult().name())
                    .ipAddress(attempt.getIpAddress())
                    .countryCode(attempt.getCountryCode())
                    .isNewCountry(attempt.getIsNewCountry())
                    .createdAt(attempt.getCreatedAt())
                    .build();
            })
            .toList();

        Page<LoginHistoryResponse> page = new PageImpl<>(
            items,
            pageable,
            attemptsPage.getTotalElements()
        );

        return PageResponse.from(page, page.getContent());
    }
    
    /**
     * Lấy danh sách bất thường đăng nhập.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoginAnomalyResponse> getLoginAnomalies(
        String status,
        String reasonCode,
        UUID organizationId,
        String username,
        Pageable pageable
    ) {
        ensureAdminAccess("xem danh sách đăng nhập bất thường");

        Page<LoginAnomaly> anomaliesPage;
        PageRequest allPageRequest = PageRequest.of(0, Integer.MAX_VALUE);

        if (organizationId != null) {
            anomaliesPage = loginAnomalyRepository.findByOrganization_OrganizationIdOrderByDetectedAtDesc(
                organizationId,
                allPageRequest
            );
        } else {
            anomaliesPage = loginAnomalyRepository.findAllByOrderByDetectedAtDesc(allPageRequest);
        }

        if (anomaliesPage == null) {
            anomaliesPage = new PageImpl<>(List.of(), allPageRequest, 0L);
        }

        String normalizedUsername = username == null ? null : username.trim();

        List<LoginAnomalyResponse> filteredItems = anomaliesPage.getContent().stream()
            .filter(anomaly -> status == null || anomaly.getStatus().name().equalsIgnoreCase(status))
            .filter(anomaly -> reasonCode == null || anomaly.getReasonCode().name().equalsIgnoreCase(reasonCode))
            .filter(anomaly -> normalizedUsername == null || normalizedUsername.isBlank()
                || anomaly.getUser() != null && (
                    anomaly.getUser().getUserName() != null && anomaly.getUser().getUserName().toLowerCase().contains(normalizedUsername.toLowerCase())
                    || anomaly.getUser().getFullName() != null && anomaly.getUser().getFullName().toLowerCase().contains(normalizedUsername.toLowerCase())
                ))
            .map(anomaly -> {
                String roleCode = anomaly.getUser() != null
                    ? organizationUserRepository.findFirstByUser(anomaly.getUser())
                        .map(orgUser -> orgUser.getRole() != null ? orgUser.getRole().getCode() : null)
                        .orElse(null)
                    : null;

                boolean accountLocked = anomaly.getUser() != null && accountLockRepository
                    .existsByUser_UserIdAndStatus(
                        anomaly.getUser().getUserId(),
                        AccountLockStatus.LOCKED
                    );

                OffsetDateTime lockUntil = null;
                boolean permanentLock = false;
                if (anomaly.getUser() != null && accountLocked) {
                    AccountLock latestLock = accountLockRepository
                        .findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
                            anomaly.getUser().getUserId(),
                            AccountLockStatus.LOCKED
                        )
                        .orElse(null);

                    if (latestLock != null) {
                        lockUntil = latestLock.getLockUntil();
                        permanentLock = latestLock.isPermanent();
                    }
                }

                return LoginAnomalyResponse.builder()
                    .id(anomaly.getId())
                    .userId(anomaly.getUser().getUserId())
                    .username(anomaly.getUser().getUserName())
                    .fullName(anomaly.getUser().getFullName())
                    .roleCode(roleCode)
                    .organizationId(anomaly.getOrganization() != null ? anomaly.getOrganization().getOrganizationId() : null)
                    .organizationName(anomaly.getOrganization() != null ? anomaly.getOrganization().getName() : null)
                    .reasonCode(anomaly.getReasonCode().name())
                    .attemptCount(anomaly.getAttemptCount())
                    .ipAddress(anomaly.getIpAddress())
                    .countryCode(anomaly.getCountryCode())
                    .detectedAt(anomaly.getDetectedAt())
                    .status(anomaly.getStatus().name())
                    .accountLocked(accountLocked)
                    .lockUntil(lockUntil)
                    .permanentLock(permanentLock)
                    .notificationId(anomaly.getNotificationId())
                    .build();
            })
            .toList();

        int start = (int) Math.min(pageable.getOffset(), filteredItems.size());
        int end = Math.min(start + pageable.getPageSize(), filteredItems.size());
        List<LoginAnomalyResponse> pageItems = filteredItems.subList(start, end);

        Page<LoginAnomalyResponse> page = new PageImpl<>(
            pageItems,
            pageable,
            filteredItems.size()
        );

        return PageResponse.from(page, page.getContent());
    }
    
    /**
     * Khoá tạm tài khoản.
     */
    @Override
    public AccountLockResponse lockAccount(
        UUID accountId,
        UUID anomalyId,
        String reason
    ) {
        return lockAccount(accountId, anomalyId, reason, 0, 0, 0, false);
    }

    @Override
    public AccountLockResponse lockAccount(
        UUID accountId,
        UUID anomalyId,
        String reason,
        Integer days,
        Integer hours,
        Integer minutes,
        boolean permanent
    ) {
        ensureAdminAccess("khóa tài khoản nghi vấn");

        CustomUserDetails currentUser = getCurrentUser();
        User lockedByUser = userRepository.findById(currentUser.getUserId())
            .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));

        accountLockService.lockAccount(accountId, anomalyId, reason, lockedByUser, days, hours, minutes, permanent);

        AccountLock lock = accountLockRepository
            .findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
                accountId,
                AccountLockStatus.LOCKED
            )
            .orElseThrow(() -> new BusinessException("Không tìm thấy bản ghi khoá"));

        return toAccountLockResponse(lock);
    }
    
    /**
     * Mở khoá tài khoản.
     */
    @Override
    public AccountLockResponse unlockAccount(UUID accountId) {
        ensureAdminAccess("mở khóa tài khoản nghi vấn");

        // Lấy thông tin người dùng hiện tại
        CustomUserDetails currentUser = getCurrentUser();
        User unlockedByUser = userRepository.findById(currentUser.getUserId())
            .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
        
        // TODO: Implement permission check
        // Chỉ VT-01 hoặc VT-02 (trong tổ chức mình) mới được mở khoá
        
        // Gọi service mở khoá
        accountLockService.unlockAccount(accountId, unlockedByUser);
        
        // Lấy thông tin khoá vừa mở
        AccountLock lock = accountLockRepository
            .findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
                accountId,
                AccountLockStatus.UNLOCKED
            )
            .orElseThrow(() -> new BusinessException("Không tìm thấy bản ghi khoá"));
        
        return toAccountLockResponse(lock);
    }

    @Override
    public PageResponse<SuspiciousCaseResponse> getSuspiciousCases(
        String status,
        UUID organizationId,
        String username,
        Pageable pageable
    ) {
        ensureAdminAccess("xem danh sách tài khoản nghi vấn");

        Page<SuspiciousCase> casesPage;
        PageRequest allPageRequest = PageRequest.of(0, Integer.MAX_VALUE);

        if (organizationId != null) {
            casesPage = suspiciousCaseRepository.findByOrganization_OrganizationIdOrderByLastDetectedAtDesc(
                organizationId,
                allPageRequest
            );
        } else {
            casesPage = suspiciousCaseRepository.findAllByOrderByLastDetectedAtDesc(allPageRequest);
        }

        String normalizedUsername = username == null ? null : username.trim();

        List<SuspiciousCaseResponse> filteredItems = casesPage.getContent().stream()
            .filter(item -> status == null || status.isBlank() || item.getStatus().name().equalsIgnoreCase(status))
            .filter(item -> normalizedUsername == null || normalizedUsername.isBlank()
                || item.getUser() != null && (
                    item.getUser().getUserName() != null && item.getUser().getUserName().toLowerCase().contains(normalizedUsername.toLowerCase())
                    || item.getUser().getFullName() != null && item.getUser().getFullName().toLowerCase().contains(normalizedUsername.toLowerCase())
                ))
            .map(item -> SuspiciousCaseResponse.builder()
                .id(item.getId())
                .userId(item.getUser() != null ? item.getUser().getUserId() : null)
                .username(item.getUser() != null ? item.getUser().getUserName() : null)
                .fullName(item.getUser() != null ? item.getUser().getFullName() : null)
                .organizationId(item.getOrganization() != null ? item.getOrganization().getOrganizationId() : null)
                .organizationName(item.getOrganization() != null ? item.getOrganization().getName() : null)
                .status(item.getStatus().name())
                .anomalyCount(item.getAnomalyCount())
                .firstDetectedAt(item.getFirstDetectedAt())
                .lastDetectedAt(item.getLastDetectedAt())
                .createdAt(item.getCreatedAt())
                .resolvedAt(item.getResolvedAt())
                .build())
            .toList();

        int start = (int) Math.min(pageable.getOffset(), filteredItems.size());
        int end = Math.min(start + pageable.getPageSize(), filteredItems.size());
        List<SuspiciousCaseResponse> pageItems = filteredItems.subList(start, end);

        Page<SuspiciousCaseResponse> page = new PageImpl<>(
            pageItems,
            pageable,
            filteredItems.size()
        );

        return PageResponse.from(page, page.getContent());
    }

    @Override
    public void markUserAnomaliesResolved(UUID accountId) {
        ensureAdminAccess("đánh dấu đã giải quyết bất thường của tài khoản");

        List<LoginAnomaly> anomalies = loginAnomalyRepository
            .findByUser_UserIdOrderByDetectedAtDesc(accountId, Pageable.unpaged())
            .getContent();

        if (anomalies.isEmpty()) {
            return;
        }

        List<LoginAnomaly> activeToResolve = anomalies.stream()
            .filter(anomaly -> anomaly.getStatus() == AnomalyStatus.OPEN)
            .toList();

        if (!activeToResolve.isEmpty()) {
            activeToResolve.forEach(anomaly -> anomaly.setStatus(AnomalyStatus.DISMISSED));
            loginAnomalyRepository.saveAll(activeToResolve);
        }

        List<SuspiciousCase> casesToResolve = suspiciousCaseRepository
            .findByUser_UserIdOrderByLastDetectedAtDesc(accountId)
            .stream()
            .filter(item -> item.getStatus() == AnomalyStatus.OPEN)
            .toList();

        if (!casesToResolve.isEmpty()) {
            casesToResolve.forEach(item -> {
                item.setStatus(AnomalyStatus.DISMISSED);
                item.setResolvedAt(OffsetDateTime.now());
            });
            suspiciousCaseRepository.saveAll(casesToResolve);
        }
    }
    
    /**
     * Chuyển đổi AccountLock entity sang response DTO.
     */
    private AccountLockResponse toAccountLockResponse(AccountLock lock) {
        return AccountLockResponse.builder()
            .accountId(lock.getUser().getUserId())
            .status(lock.getStatus().toString())
            .lockedBy(lock.getLockedBy().getUserName())
            .lockedAt(lock.getLockedAt())
            .lockUntil(lock.getLockUntil())
            .permanent(lock.isPermanent())
            .unlockedBy(lock.getUnlockedBy() != null ? lock.getUnlockedBy().getUserName() : null)
            .unlockedAt(lock.getUnlockedAt())
            .reason(lock.getLockReason())
            .notificationSent(true)
            .build();
    }
    
    /**
     * Lấy thông tin người dùng hiện tại từ SecurityContext.
     */
    private CustomUserDetails getCurrentUser() {
        return (CustomUserDetails) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
    }

    private void ensureAdminAccess(String action) {
        CustomUserDetails currentUser = getCurrentUser();
        if (!RoleCode.ADMIN.equals(currentUser.getRoleCode())) {
            throw new BusinessException("Chỉ vai trò quản trị viên admin mới được " + action + ".");
        }
    }
}
