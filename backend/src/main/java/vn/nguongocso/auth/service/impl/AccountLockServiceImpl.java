package vn.nguongocso.auth.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.AnomalyStatus;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.LoginAnomalyRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.AccountLockService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;

/**
 * Triển khai service quản lý khoá/mở khoá tài khoản.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountLockServiceImpl implements AccountLockService {
    
    private final UserRepository userRepository;
    private final AccountLockRepository accountLockRepository;
    private final LoginAnomalyRepository loginAnomalyRepository;
    private final NotificationService notificationService;
    
    /**
     * Khoá tạm một tài khoản.
     */
    @Override
    public UUID lockAccount(
        UUID accountId,
        UUID anomalyId,
        String reason,
        User lockedBy
    ) {
        User account = userRepository.findById(accountId)
            .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));
        
        if (account.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Tài khoản không ở trạng thái hoạt động");
        }
        
        // Kiểm tra xem tài khoản đã bị khoá trước đó chưa
        boolean alreadyLocked = accountLockRepository
            .existsByUser_UserIdAndStatus(
                accountId,
                AccountLockStatus.LOCKED
            );
        
        if (alreadyLocked) {
            throw new BusinessException("Tài khoản đã bị khóa tạm trước đó");
        }
        
        // Cập nhật trạng thái tài khoản thành LOCKED
        account.setStatus(UserStatus.LOCKED);
        userRepository.save(account);
        
        // Tạo bản ghi khoá
        LoginAnomaly anomaly = null;
        if (anomalyId != null) {
            anomaly = loginAnomalyRepository.findById(anomalyId)
                .orElse(null);
            
            if (anomaly != null) {
                anomaly.setStatus(AnomalyStatus.ACCOUNT_LOCKED);
                loginAnomalyRepository.save(anomaly);
            }
        }
        
        AccountLock lock = AccountLock.builder()
            .user(account)
            .anomaly(anomaly)
            .lockedBy(lockedBy)
            .lockReason(reason)
            .lockedAt(OffsetDateTime.now())
            .status(AccountLockStatus.LOCKED)
            .build();
        
        accountLockRepository.save(lock);
        
        log.info(
            "Tài khoản đã được khóa tạm. accountId={}, lockedBy={}, reason={}",
            accountId,
            lockedBy.getUserId(),
            reason
        );
        
        // Vô hiệu hoá tất cả access token
        invalidateAllTokens(accountId);
        
        // Gửi thông báo cho các admin/quản lý tổ chức
        notificationService.sendAccountLockedNotification(lock);
        
        return accountId;
    }
    
    /**
     * Mở khóa một tài khoản.
     */
    @Override
    public UUID unlockAccount(UUID accountId, User unlockedBy) {
        User account = userRepository.findById(accountId)
            .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));
        
        if (account.getStatus() != UserStatus.LOCKED) {
            throw new BusinessException("Tài khoản hiện không ở trạng thái bị khóa tạm");
        }
        
        // Lấy bản ghi khoá gần nhất
        AccountLock lock = accountLockRepository
            .findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
                accountId,
                AccountLockStatus.LOCKED
            )
            .orElseThrow(() -> new BusinessException("Không tìm thấy bản ghi khóa cho tài khoản"));
        
        // Cập nhật trạng thái tài khoản thành ACTIVE
        account.setStatus(UserStatus.ACTIVE);
        userRepository.save(account);
        
        // Cập nhật bản ghi khoá
        lock.setStatus(AccountLockStatus.UNLOCKED);
        lock.setUnlockedBy(unlockedBy);
        lock.setUnlockedAt(OffsetDateTime.now());
        accountLockRepository.save(lock);
        
        log.info(
            "Tài khoản đã được mở khóa. accountId={}, unlockedBy={}",
            accountId,
            unlockedBy.getUserId()
        );
        
        // Gửi thông báo cho các admin/quản lý tổ chức
        notificationService.sendAccountUnlockedNotification(lock);
        
        return accountId;
    }
    
    /**
     * Vô hiệu hoá tất cả access token của một tài khoản.
     * 
     * <p>
     * Hiện tại, đây là placeholder. Cần tích hợp với JwtTokenProvider
     * hoặc cơ chế token blacklist để vô hiệu hoá tokens.
     * </p>
     */
    @Override
    public void invalidateAllTokens(UUID userId) {
        log.info("Vô hiệu hoá tất cả access token của tài khoản: userId={}", userId);
        
        // TODO: Cài đặt mechanism vô hiệu hoá token
        // - Có thể thêm userId vào blacklist
        // - Hoặc xoá từ cache token
        // - Hoặc implement token versioning
    }
}
