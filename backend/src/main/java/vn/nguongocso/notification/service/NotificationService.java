package vn.nguongocso.notification.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.notification.dto.response.NotificationResponse;
import vn.nguongocso.notification.dto.response.UnreadCountResponse;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.entity.Recall;

/** Dịch vụ gửi thông báo. */
public interface NotificationService {
    /** Gửi thông báo cảnh báo. */
    void sendScanAnomalyNotification(Alert alert);

    /** Gửi thông báo thu hồi lô hàng. */
    void sendShipmentRecallNotification(Recall recall);

    /**
     * Gửi thông báo chứng nhận sắp hết hạn hoặc đã hết hạn.
     */
    void sendCertificationExpiryNotification(Alert alert);

    /** Lấy danh sách thông báo của người dùng đang đăng nhập. */
    PageResponse<NotificationResponse> getNotifications(
            Boolean isRead,
            Pageable pageable);

    /**
     * Lấy số lượng thông báo chưa đọc của người dùng đang đăng nhập.
     */
    UnreadCountResponse getUnreadCount();

    /**
     * Đánh dấu một thông báo là đã đọc.
     */
    NotificationResponse markAsRead(UUID notificationId);

    /**
     * Gửi thông báo cảnh báo chung.
     */
    void sendAlert(String message);
}