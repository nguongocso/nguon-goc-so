package vn.nguongocso.auth.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.enums.AccountLockStatus;

/**
 * Repository quản lý các bản ghi khoá/mở khoá tài khoản.
 */
@Repository
public interface AccountLockRepository extends JpaRepository<AccountLock, UUID> {
    
    /**
     * Lấy bản ghi khoá gần nhất (theo lockedAt DESC) của một tài khoản có status nhất định.
     * Dùng để kiểm tra trạng thái khoá hiện tại của tài khoản.
     */
    Optional<AccountLock> findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
        UUID userId,
        AccountLockStatus status
    );

    /**
     * Lấy bản ghi khoá gần nhất theo thời gian, bất kể trạng thái hiện tại.
     * Dùng để xác định trạng thái khoá hiện tại sau khi mở khoá trước đó.
     */
    Optional<AccountLock> findTopByUser_UserIdOrderByLockedAtDesc(UUID userId);
    
    /**
     * Lấy lịch sử khoá/mở khoá của một tài khoản.
     */
    Page<AccountLock> findByUser_UserIdOrderByLockedAtDesc(
        UUID userId,
        Pageable pageable
    );
    
    /**
     * Kiểm tra xem tài khoản có đang bị khoá hay không.
     */
    boolean existsByUser_UserIdAndStatus(
        UUID userId,
        AccountLockStatus status
    );

    /**
     * Lấy các bản ghi khóa tạm đã hết hạn và vẫn đang ở trạng thái LOCKED.
     */
    List<AccountLock> findByStatusAndPermanentFalseAndLockUntilBefore(
        AccountLockStatus status,
        OffsetDateTime before
    );
}
