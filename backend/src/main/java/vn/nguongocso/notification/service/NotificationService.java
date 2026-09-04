package vn.nguongocso.notification.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.notification.dto.response.NotificationResponse;
import vn.nguongocso.notification.dto.response.UnreadCountResponse;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.trace.entity.Recall;
import vn.nguongocso.trace.entity.TraceCode;

/** Dịch vụ gửi thông báo. */
public interface NotificationService {
    /** Gửi thông báo cảnh báo. */
    void sendScanAnomalyNotification(Alert alert);

    /** Gửi thông báo thu hồi lô hàng. */
    void sendShipmentRecallNotification(Recall recall);

    /**
     * Gửi thông báo thu hồi lô sản xuất (NCL-08-CN-008) cho danh sách người dùng.
     *
     * @param lotName tên lô sản xuất
     * @param reason  lý do thu hồi
     * @param recipientIds danh sách ID người dùng nhận thông báo
     * @return số lượng thông báo đã tạo
     */
    int sendLotRecallNotification(String lotName, String reason, List<UUID> recipientIds);

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
     * Gửi thông báo khi một mã tem bị đánh dấu nghi vấn.
     */
    void sendSuspectTraceCodeNotification(TraceCode traceCode);

    /**
     * Gửi thông báo cảnh báo chung.
     */
    void sendAlert(String message);

    /**
     * Gửi thông báo khi phát hiện đăng nhập bất thường (NCL-01-CN-005).
     */
    void sendLoginAnomalyNotification(LoginAnomaly anomaly);

    /**
     * Gửi thông báo khi tài khoản bị khóa.
     */
    void sendAccountLockedNotification(AccountLock accountLock);

    /**
     * Gửi thông báo khi tài khoản được mở khóa.
     */
    void sendAccountUnlockedNotification(AccountLock accountLock);

    /**
     * Gửi thông báo khi mã tem được mở khóa sau khi xác minh (NCL-08-CN-013).
     *
     * @param traceCode mã tem đã được mở khóa
     */
    void sendTraceCodeUnlockedNotification(TraceCode traceCode);
}
