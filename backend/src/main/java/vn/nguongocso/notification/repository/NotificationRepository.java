package vn.nguongocso.notification.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.notification.entity.Notification;

/**
 * Repository thao tác Notification.
 */
public interface NotificationRepository
                extends JpaRepository<Notification, UUID> {
        /**
         * Lấy tất cả thông báo của người dùng, sắp xếp mới nhất.
         */
        Page<Notification> findByUser_UserIdOrderByCreatedAtDesc(
                        UUID userId,
                        Pageable pageable);

        /**
         * Lấy thông báo theo trạng thái đã đọc/chưa đọc.
         */
        Page<Notification> findByUser_UserIdAndIsReadOrderByCreatedAtDesc(
                        UUID userId,
                        Boolean isRead,
                        Pageable pageable);

        /**
         * Đếm số thông báo chưa đọc.
         */
        long countByUser_UserIdAndIsReadFalse(UUID userId);
}