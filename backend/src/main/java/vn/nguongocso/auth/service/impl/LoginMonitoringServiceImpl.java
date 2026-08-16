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
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.LoginAttempt;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.LoginAnomalyRepository;
import vn.nguongocso.auth.repository.LoginAttemptRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.AccountLockService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.LoginMonitoringService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
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
    private final AccountLockRepository accountLockRepository;
    private final UserRepository userRepository;
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
        // TODO: Implement permission check và filtering
        // Hiện tại trả về danh sách rỗng
        
        Page<LoginHistoryResponse> page = new PageImpl<>(
            List.of(),
            pageable,
            0
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
        Pageable pageable
    ) {
        // TODO: Implement permission check và filtering
        // Hiện tại trả về danh sách rỗng
        
        Page<LoginAnomalyResponse> page = new PageImpl<>(
            List.of(),
            pageable,
            0
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
        // Lấy thông tin người dùng hiện tại
        CustomUserDetails currentUser = getCurrentUser();
        User lockedByUser = userRepository.findById(currentUser.getUserId())
            .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
        
        // TODO: Implement permission check
        // Chỉ VT-01 hoặc VT-02 (trong tổ chức mình) mới được khoá
        
        // Gọi service khoá
        accountLockService.lockAccount(accountId, anomalyId, reason, lockedByUser);
        
        // Lấy thông tin khoá vừa tạo
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
    
    /**
     * Chuyển đổi AccountLock entity sang response DTO.
     */
    private AccountLockResponse toAccountLockResponse(AccountLock lock) {
        return AccountLockResponse.builder()
            .accountId(lock.getUser().getUserId())
            .status(lock.getStatus().toString())
            .lockedBy(lock.getLockedBy().getUserName())
            .lockedAt(lock.getLockedAt())
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
}
