package vn.nguongocso.notification.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.notification.dto.response.NotificationResponse;
import vn.nguongocso.notification.dto.response.UnreadCountResponse;
import vn.nguongocso.trace.entity.Recall;
import vn.nguongocso.trace.entity.TraceCode;

/** Dịch vụ gửi thông báo. */
public interface NotificationService {
    /** Gửi thông báo cảnh báo. */
    void sendScanAnomalyNotification(Alert alert);

    /** Gửi thông báo thu hồi lô hàng. */
    void sendShipmentRecallNotification(Recall recall);

    /** Gửi thông báo thu hồi lô hàng cho danh sách người dùng. */
    int sendRecallNotification(String shipmentName, String reason, List<UUID> recipientIds);

    /** Gửi thông báo chứng nhận sắp hết hạn hoặc đã hết hạn. */
    void sendCertificationExpiryNotification(Alert alert);

    /** Lấy danh sách thông báo của người dùng đang đăng nhập. */
    PageResponse<NotificationResponse> getNotifications(
            Boolean isRead,
            Pageable pageable);

    /** Lấy số lượng thông báo chưa đọc của người dùng đang đăng nhập. */
    UnreadCountResponse getUnreadCount();

    /** Đánh dấu một thông báo là đã đọc. */
    NotificationResponse markAsRead(UUID notificationId);

    /** Đánh dấu tất cả thông báo chưa đọc của người dùng là đã đọc. */
    int markAllAsRead();

    /** Gửi thông báo khi một mã tem bị đánh dấu nghi vấn. */
    void sendSuspectTraceCodeNotification(TraceCode traceCode);

    /** Gửi thông báo cảnh báo chung. */
    void sendAlert(String message);

    /** Gửi thông báo khi phát hiện đăng nhập bất thường. */
    void sendLoginAnomalyNotification(LoginAnomaly anomaly);

    /** Gửi thông báo khi tài khoản bị khóa. */
    void sendAccountLockedNotification(AccountLock accountLock);

    /** Gửi thông báo khi tài khoản được mở khóa. */
    void sendAccountUnlockedNotification(AccountLock accountLock);

    /** Gửi thông báo kết quả duyệt yêu cầu cấp bổ sung dải mã truy xuất. */
    int sendCodeRangeSupplementNotification(String title, String content, List<UUID> recipientIds);

    /** Gửi thông báo khi mã tem được mở khóa sau khi xác minh. */
    void sendTraceCodeUnlockedNotification(TraceCode traceCode);

    /** Gửi thông báo vòng đời phiếu bàn giao lô hàng. */
    void sendHandoverNotification(String title, String content, UUID entityId, UUID organizationId);

    /** Gửi thông báo khi lô sản xuất có kết quả kiểm nghiệm ĐẠT. */
    void sendInspectionPassedNotification(String lotName, UUID organizationId);

    /** Gửi cảnh báo khi lô sản xuất có kết quả kiểm nghiệm KHÔNG ĐẠT. */
    void sendInspectionFailedNotification(String lotName, UUID organizationId);

    /** Gửi thông báo khi chứng nhận bị từ chối xác thực. */
    int sendCertificationRejectionNotification(Certification certification, String rejectionReason);

    /** Gửi thông báo workflow nội bộ cho yêu cầu thu hồi hàng loạt. */
    int sendBulkRecallWorkflowNotification(
            String title,
            String content,
            UUID requestId,
            String action,
            List<User> recipients);

    /** Gửi thông báo kết thúc vụ việc thu hồi cho các tổ chức thu mua. */
    int sendRecallCaseClosedNotification(String caseCode, List<UUID> recipientIds);

    /** Gửi cảnh báo hiệu lực kết quả kiểm nghiệm của lô sản xuất. */
    void sendInspectionExpiryNotification(
            Alert alert,
            ProductionLot lot,
            InspectionValidityResponse validity);

    /** Gửi thông báo cho người yêu cầu khi tệp nhật ký nền đã sẵn sàng. */
    void sendActivityLogExportReadyNotification(UUID exportJobId, UUID recipientId);
}
