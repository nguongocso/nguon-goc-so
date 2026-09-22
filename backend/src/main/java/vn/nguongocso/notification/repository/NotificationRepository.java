package vn.nguongocso.notification.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.notification.entity.Notification;

/**
 * Repository thao tác Notification.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Lấy tất cả thông báo của người dùng, ưu tiên chưa đọc lên trước rồi mới đến mới nhất.
     * Giúp số lượng chuông (đếm toàn bộ chưa đọc) khớp với danh sách hiển thị.
     */
    Page<Notification> findByUser_UserIdOrderByIsReadAscCreatedAtDesc(
            UUID userId,
            Pageable pageable);

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
     * Kiểm tra đã tồn tại thông báo cùng thực thể + tiêu đề kể từ mốc thời gian hay chưa.
     * Dùng để chống tạo trùng cảnh báo (NCL-12-CN-005: 1 lần/ngày cho hết hạn, 1 lần/giờ cho hạn mức).
     */
    boolean existsByEntityIdAndTitleAndCreatedAtAfter(
            UUID entityId,
            String title,
            LocalDateTime after);

    /**
     * Đánh dấu đã đọc toàn bộ thông báo chưa đọc của người dùng.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt "
            + "WHERE n.user.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
}
