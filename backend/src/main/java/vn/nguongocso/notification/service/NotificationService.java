package vn.nguongocso.notification.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.certification.entity.Certification;
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
     * Gửi thông báo thu hồi lô hàng (NCL-08-CN-008) cho danh sách người dùng.
     *
     * @param shipmentName tên lô hàng
     * @param reason  lý do thu hồi
     * @param recipientIds danh sách ID người dùng nhận thông báo
     * @return số lượng thông báo đã tạo
     */
    int sendRecallNotification(String shipmentName, String reason, List<UUID> recipientIds);

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
     * Gửi thông báo kết quả duyệt yêu cầu cấp bổ sung dải mã truy xuất
     * (NCL-04-CN-007) cho danh sách người dùng được chỉ định
     * (người tạo yêu cầu + quản lý HTX của tổ chức).
     *
     * @param title        tiêu đề thông báo
     * @param content      nội dung thông báo
     * @param recipientIds danh sách ID người dùng nhận thông báo
     * @return số lượng thông báo đã tạo
     */
    int sendCodeRangeSupplementNotification(String title, String content, List<UUID> recipientIds);

    /**
     * Gửi thông báo khi mã tem được mở khóa sau khi xác minh (NCL-08-CN-013).
     *
     * @param traceCode mã tem đã được mở khóa
     */
    void sendTraceCodeUnlockedNotification(TraceCode traceCode);

    /**
     * Gửi cảnh báo cho Quản lý hợp tác xã khi lô sản xuất có kết quả
     * kiểm nghiệm KHÔNG ĐẠT (NCL-11-CN-005, QTN-30).
     *
     * <p>
     * Người nhận là các user thuộc tổ chức của lô có permission
     * {@code notification:READ} (cùng cơ chế phân phối hiện có).
     * </p>
     *
     * @param lotName        tên lô sản xuất
     * @param organizationId tổ chức sở hữu lô sản xuất
     */
    void sendInspectionFailedNotification(String lotName, UUID organizationId);

    /**
     * Gửi thông báo khi chứng nhận bị Quản trị viên nền tảng (VT-01) từ chối xác thực (NCL-09-CN-012, QTN-34).
     *
     * @param certification   chứng nhận bị từ chối
     * @param rejectionReason lý do từ chối
     * @return số lượng thông báo đã tạo
     */
    int sendCertificationRejectionNotification(Certification certification, String rejectionReason);

    /**
     * Gửi thông báo workflow nội bộ cho yêu cầu thu hồi hàng loạt
     * (NCL-08-CN-011).
     *
     * @param title       tiêu đề thông báo
     * @param content     nội dung thông báo
     * @param requestId   ID yêu cầu thu hồi
     * @param action      hành động (CREATE/APPROVE/REJECT)
     * @param recipients  danh sách người nhận
     * @return số lượng thông báo đã tạo
     */
    int sendBulkRecallWorkflowNotification(
            String title,
            String content,
            UUID requestId,
            String action,
            List<vn.nguongocso.auth.entity.User> recipients);
}
