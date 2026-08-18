package vn.nguongocso.auth.service.impl;

import java.time.Duration;
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
        return lockAccount(accountId, anomalyId, reason, lockedBy, 0, 0, 0, false);
    }

    @Override
    public UUID lockAccount(
        UUID accountId,
        UUID anomalyId,
        String reason,
        User lockedBy,
        Integer days,
        Integer hours,
        Integer minutes,
        boolean permanent
    ) {
        User account = userRepository.findById(accountId)
            .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));

        if (account.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Tài khoản không ở trạng thái hoạt động");
        }

        account.setStatus(UserStatus.INACTIVE);
        userRepository.save(account);

        AccountLock activeLock = accountLockRepository
            .findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(accountId, AccountLockStatus.LOCKED)
            .orElse(null);

        if (activeLock != null) {
            OffsetDateTime now = OffsetDateTime.now();
            long elapsedSeconds = Duration.between(activeLock.getLockedAt(), now).getSeconds();

            if (!activeLock.isPermanent() && activeLock.getLockUntil() != null) {
                if (now.isBefore(activeLock.getLockUntil())) {
                    throw new BusinessException("Tài khoản đã bị khóa trước đó");
                }
                activeLock.setStatus(AccountLockStatus.UNLOCKED);
                activeLock.setUnlockedAt(now);
                accountLockRepository.save(activeLock);
                log.info("Cập nhật bản ghi khóa cũ từ thời hạn hết hạn thành UNLOCKED. accountId={}, elapsedSeconds={}", accountId, elapsedSeconds);
            } else if (activeLock.isPermanent() || activeLock.getLockUntil() == null) {
                if (activeLock.isPermanent()) {
                    throw new BusinessException("Tài khoản đang bị khóa vĩnh viễn, cần mở khóa thủ công mới được truy cập lại");
                }
                if (elapsedSeconds < 60) {
                    throw new BusinessException("Tài khoản đã bị khóa trước đó");
                }
                activeLock.setStatus(AccountLockStatus.UNLOCKED);
                activeLock.setUnlockedAt(now);
                accountLockRepository.save(activeLock);
                log.info("Cập nhật bản ghi khóa cũ từ timeout thành UNLOCKED. accountId={}, elapsedSeconds={}", accountId, elapsedSeconds);
            }
        }

        LoginAnomaly anomaly = null;
        if (anomalyId != null) {
            anomaly = loginAnomalyRepository.findById(anomalyId).orElse(null);
            if (anomaly != null) {
                // Trạng thái khóa của tài khoản thuộc AccountLockStatus, không phải AnomalyStatus.
                // Bản ghi bất thường chỉ được chuyển trạng thái khi admin đánh dấu đã giải quyết.
                loginAnomalyRepository.save(anomaly);
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime lockUntil = null;
        if (!permanent) {
            int totalMinutes = (days == null ? 0 : days) * 24 * 60
                + (hours == null ? 0 : hours) * 60
                + (minutes == null ? 0 : minutes);
            if (totalMinutes <= 0) {
                totalMinutes = 60;
            }
            lockUntil = now.plusMinutes(totalMinutes);
        }

        AccountLock lock = AccountLock.builder()
            .user(account)
            .anomaly(anomaly)
            .lockedBy(lockedBy)
            .lockReason(reason)
            .lockedAt(now)
            .lockUntil(lockUntil)
            .permanent(permanent)
            .status(AccountLockStatus.LOCKED)
            .build();

        accountLockRepository.save(lock);

        log.info(
            "Tài khoản đã được khóa. accountId={}, lockedBy={}, reason={}, permanent={}, lockUntil={}",
            accountId,
            lockedBy.getUserId(),
            reason,
            permanent,
            lockUntil
        );

        invalidateAllTokens(accountId);
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
        
        // Lấy bản ghi khóa gần nhất đang ở trạng thái LOCKED
        AccountLock lock = accountLockRepository
            .findTopByUser_UserIdOrderByLockedAtDesc(accountId)
            .filter(currentLock -> currentLock.getStatus() == AccountLockStatus.LOCKED)
            .orElseThrow(() -> new BusinessException("Tài khoản hiện không ở trạng thái bị khóa"));
        
        account.setStatus(UserStatus.ACTIVE);
        userRepository.save(account);

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
