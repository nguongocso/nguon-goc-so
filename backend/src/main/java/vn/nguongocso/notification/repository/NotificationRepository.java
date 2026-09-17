package vn.nguongocso.notification.repository;

import java.time.LocalDateTime;
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

        /**
         * Kiểm tra đã tồn tại thông báo cùng thực thể + tiêu đề kể từ mốc thời gian
         * hay chưa. Dùng để chống tạo trùng cảnh báo (NCL-12-CN-005: 1 lần/ngày
         * cho hết hạn, 1 lần/giờ cho hạn mức).
         */
        boolean existsByEntityIdAndTitleAndCreatedAtAfter(
                        UUID entityId,
                        String title,
                        LocalDateTime after);
}