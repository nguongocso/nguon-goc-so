package vn.nguongocso.notification.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.enums.NotificationType;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.dto.response.NotificationResponse;
import vn.nguongocso.notification.dto.response.UnreadCountResponse;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.entity.Recall;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.certification.enums.InspectionValidityStatus;
import vn.nguongocso.farm.entity.ProductionLot;

/**
 * Triển khai dịch vụ thông báo.
 *
 * <p>
 * Service chịu trách nhiệm:
 * <ul>
 * <li>Tạo và phân phối thông báo cho người dùng liên quan.</li>
 * <li>Lấy danh sách thông báo của người dùng hiện tại.</li>
 * <li>Đếm số thông báo chưa đọc.</li>
 * <li>Đánh dấu thông báo đã đọc.</li>
 * </ul>
 *
 * <p>
 * Việc xác định người nhận không còn dựa trên role code cố định
 * (VT-01, VT-02, VT-03), mà dựa trên permission: {@code notification:READ}.
 * Người dùng thuộc tổ chức của resource và có permission này sẽ được nhận thông báo.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String NOTIFICATION_RESOURCE = "notification";
    private static final String NOTIFICATION_READ_ACTION = "READ";

    private static final String NOTIFICATION_TITLE = "Cảnh báo tem quét bất thường";
    private static final String NOTIFICATION_CONTENT = "Hệ thống phát hiện mã truy xuất có dấu hiệu bị quét bất thường ở nhiều vị trí.";

    private static final String RECALL_TITLE = "Thông báo thu hồi lô hàng";

    private static final String MSG_NOTIFICATION_NOT_FOUND = "Thông báo không tồn tại.";
    private static final String MSG_NO_PERMISSION_TO_ACCESS = "Bạn không có quyền thao tác với thông báo này.";
    private static final String MSG_TRACE_CODE_NOT_FOUND = "Mã truy xuất không tồn tại.";
    private static final String MSG_CERTIFICATION_NOT_FOUND = "Chứng nhận không tồn tại.";

    private static final String SUSPECT_NOTIFICATION_TITLE = "Mã tem bị nghi vấn";
    private static final String SUSPECT_NOTIFICATION_CONTENT_FORMAT = "Mã tem %s bị đánh dấu nghi vấn (điểm: %d). Lý do: %s";

    private static final String UNLOCK_NOTIFICATION_TITLE = "Mã tem đã được mở khóa";
    private static final String UNLOCK_NOTIFICATION_CONTENT_FORMAT = "Mã tem %s đã được Quản trị viên mở khóa sau khi xác minh vào lúc %s. Kết luận: %s";

    private static final String INSPECTION_PASSED_TITLE = "Kết quả kiểm nghiệm đạt";
    private static final String INSPECTION_PASSED_CONTENT_FORMAT =
            "Lô sản xuất \"%s\" đã hoàn tất kiểm nghiệm và ĐẠT yêu cầu. "
                    + "Quản lý hợp tác xã có thể tiếp tục các bước xử lý tiếp theo.";

    private static final String INSPECTION_FAILED_TITLE = "Kết quả kiểm nghiệm không đạt";
    private static final String INSPECTION_FAILED_CONTENT_FORMAT =
            "Lô sản xuất \"%s\" có kết quả kiểm nghiệm KHÔNG ĐẠT. "
                    + "Vui lòng xử lý lô theo một trong hai hướng: loại bỏ lô hoặc tạo yêu cầu kiểm nghiệm lại.";

    private static final String INSPECTION_EXPIRING_TITLE = "Cảnh báo: Kết quả kiểm nghiệm sắp hết hiệu lực";
    private static final String INSPECTION_EXPIRING_CONTENT_FORMAT =
            "Lô sản xuất \"%s\" có kết quả kiểm nghiệm sẽ hết hiệu lực sau %d ngày (ngày hết hạn: %s). "
                    + "Vui lòng chủ động lập kế hoạch kiểm nghiệm mới.";

    private static final String INSPECTION_EXPIRED_TITLE = "Kết quả kiểm nghiệm đã hết hiệu lực";
    private static final String INSPECTION_EXPIRED_CONTENT_FORMAT =
            "Lô sản xuất \"%s\" có kết quả kiểm nghiệm đã hết hiệu lực vào ngày %s. "
                    + "Vui lòng tạo yêu cầu kiểm nghiệm mới để đảm bảo tính hợp lệ của sản phẩm.";

    private static final String LOT_RECALL_TITLE = "Thông báo thu hồi lô hàng";

    private final NotificationRepository notificationRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final vn.nguongocso.auth.repository.UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final CertificationRepository certificationRepository;
    private final PermissionChecker permissionChecker;

    @Override
    public void sendActivityLogExportReadyNotification(UUID exportJobId, UUID recipientId) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new BusinessException("Người nhận thông báo không tồn tại."));

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(NotificationType.ACTIVITY_LOG_EXPORT_READY);
        notification.setTitle("Tệp nhật ký hoạt động đã sẵn sàng");
        notification.setContent("Yêu cầu xuất nhật ký hoạt động đã hoàn tất. Bấm để tải tệp CSV.");
        notification.setEntityId(exportJobId);

        notificationRepository.save(notification);
    }

    // ===== 1. THÔNG BÁO CẢNH BÁO TEM QUÉT BẤT THƯỜNG =====

    /**
     * Gửi thông báo khi hệ thống phát hiện tem/mã truy xuất
     * có dấu hiệu quét bất thường.
     *
     * <p>
     * Người nhận được xác định dựa trên permission
     * {@code notification:READ} và tổ chức sở hữu shipment.
     * </p>
     *
     * @param alert cảnh báo bất thường
     */
    @Override
    public void sendScanAnomalyNotification(Alert alert) {
        TraceCode traceCode = traceCodeRepository
                .findById(alert.getRelatedEntityId())
                .orElseThrow(() -> new BusinessException(MSG_TRACE_CODE_NOT_FOUND));

        Shipment shipment = traceCode.getShipment();
        UUID organizationId = shipment.getOrganization().getOrganizationId();

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có người dùng có permission {}:{} để nhận "
                            + "cảnh báo quét bất thường. organizationId={}",
                    NOTIFICATION_RESOURCE,
                    NOTIFICATION_READ_ACTION,
                    organizationId);
            return;
        }

        List<Notification> notifications = recipients.stream()
                .map(this::buildScanAnomalyNotification)
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification cảnh báo quét bất thường. "
                        + "organizationId={}, traceCodeId={}",
                notifications.size(),
                organizationId,
                traceCode.getId());
    }

    /**
     * Tạo notification cảnh báo quét bất thường.
     */
    private Notification buildScanAnomalyNotification(User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.ALERT);
        notification.setTitle(NOTIFICATION_TITLE);
        notification.setContent(NOTIFICATION_CONTENT);
        notification.setIsRead(false);
        notification.setReadAt(null);

        return notification;
    }

    // ===== 2. THÔNG BÁO THU HỒI LÔ HÀNG =====

    /**
     * Gửi thông báo thu hồi lô hàng cho tất cả người dùng
     * thuộc tổ chức có permission notification:READ.
     *
     * <p>
     * Không còn giới hạn cứng ở VT-01 hoặc VT-02.
     * Nếu VT-03 được cấp notification:READ thì VT-03 cũng nhận.
     * </p>
     *
     * @param recall thông tin thu hồi
     */
    @Override
    public void sendShipmentRecallNotification(Recall recall) {
        Shipment shipment = recall.getShipment();
        UUID organizationId = shipment.getOrganization().getOrganizationId();

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có người dùng có permission {}:{} để nhận "
                            + "thông báo thu hồi. organizationId={}, shipmentId={}",
                    NOTIFICATION_RESOURCE,
                    NOTIFICATION_READ_ACTION,
                    organizationId,
                    shipment.getId());
            return;
        }

        List<Notification> notifications = recipients.stream()
                .map(user -> buildRecallNotification(recall, user))
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification thu hồi lô hàng. "
                        + "organizationId={}, shipmentId={}",
                notifications.size(),
                organizationId,
                shipment.getId());
    }

    /**
     * Tạo notification thu hồi lô hàng.
     */
    private Notification buildRecallNotification(Recall recall, User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.ALERT);
        notification.setTitle(RECALL_TITLE);
        notification.setContent(
                "Lô hàng \""
                        + recall.getShipment().getName()
                        + "\" đã bị thu hồi. Lý do: "
                        + recall.getReason());
        notification.setIsRead(false);
        notification.setReadAt(null);

        return notification;
    }

    // ===== 3. THÔNG BÁO THU HỒI LÔ SẢN XUẤT (NCL-08-CN-008) =====

    /**
     * Gửi thông báo thu hồi lô sản xuất cho danh sách người dùng được chỉ định.
     *
     * @param shipmentName tên lô hàng
     * @param reason       lý do thu hồi
     * @param recipientIds danh sách ID người dùng nhận thông báo
     * @return số lượng thông báo đã tạo
     */
    @Override
    public int sendRecallNotification(String shipmentName, String reason, List<UUID> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            log.warn("Không có người dùng để nhận thông báo thu hồi lô hàng. shipmentName={}", shipmentName);
            return 0;
        }

        List<User> recipients = userRepository.findAllById(recipientIds);

        if (recipients.isEmpty()) {
            return 0;
        }

        String content = "Lô hàng \""
                + shipmentName
                + "\" đã bị thu hồi. Lý do: "
                + (reason == null ? "Không rõ" : reason);

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ALERT);
                    notification.setTitle(LOT_RECALL_TITLE);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification thu hồi lô hàng. shipmentName={}",
                notifications.size(),
                shipmentName);

        return notifications.size();
    }

    // ===== 4. THÔNG BÁO CHỨNG NHẬN SẮP HẾT HẠN / HẾT HẠN =====

    /**
     * Gửi thông báo khi chứng nhận sắp hết hạn hoặc đã hết hạn.
     *
     * @param alert cảnh báo chứng nhận
     */
    @Override
    public void sendCertificationExpiryNotification(Alert alert) {
        Certification certification = certificationRepository
                .findById(alert.getRelatedEntityId())
                .orElseThrow(() -> new BusinessException(MSG_CERTIFICATION_NOT_FOUND));

        UUID organizationId = certification.getOrganization().getOrganizationId();

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có người dùng có permission {}:{} để nhận "
                            + "thông báo chứng nhận. organizationId={}, certificationId={}",
                    NOTIFICATION_RESOURCE,
                    NOTIFICATION_READ_ACTION,
                    organizationId,
                    certification.getId());
            return;
        }

        List<Notification> notifications = recipients.stream()
                .map(user -> buildCertificationExpiryNotification(alert, certification, user))
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification chứng nhận. "
                        + "organizationId={}, certificationId={}",
                notifications.size(),
                organizationId,
                certification.getId());
    }

    /**
     * Tạo notification chứng nhận sắp hết hạn / đã hết hạn.
     */
    private Notification buildCertificationExpiryNotification(
            Alert alert,
            Certification certification,
            User user) {

        boolean expired = alert.getType() == AlertType.CERT_EXPIRED;

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.ALERT);

        if (expired) {
            notification.setTitle("Chứng nhận đã hết hạn");
            notification.setContent(
                    "Chứng nhận \""
                            + certification.getName()
                            + "\" ("
                            + certification.getCode()
                            + ") đã hết hạn vào ngày "
                            + certification.getExpiryDate()
                            + ".");
        } else {
            notification.setTitle("Chứng nhận sắp hết hạn");
            notification.setContent(
                    "Chứng nhận \""
                            + certification.getName()
                            + "\" ("
                            + certification.getCode()
                            + ") sẽ hết hạn vào ngày "
                            + certification.getExpiryDate()
                            + ".");
        }

        notification.setIsRead(false);
        notification.setReadAt(null);

        return notification;
    }

    // ===== 5. XÁC ĐỊNH NGƯỜI NHẬN THEO PERMISSION =====

    /**
     * Lấy danh sách người dùng thuộc tổ chức có permission
     * notification:READ.
     *
     * <p>
     * Không kiểm tra role code trực tiếp. Ví dụ:
     * <ul>
     * <li>VT-01 + notification:READ → nhận</li>
     * <li>VT-02 + notification:READ → nhận</li>
     * <li>VT-03 + notification:READ → nhận</li>
     * <li>Role mới + notification:READ → nhận</li>
     * </ul>
     * </p>
     *
     * @param organizationId tổ chức sở hữu resource phát sinh sự kiện
     * @return danh sách user nhận notification
     */
    private List<User> getNotificationRecipients(UUID organizationId) {
        return organizationUserRepository.findUsersByPermission(
                organizationId,
                NOTIFICATION_RESOURCE,
                NOTIFICATION_READ_ACTION);
    }

    private List<User> getLoginSecurityRecipients(User targetUser) {
        return targetUser == null ? List.of() : List.of(targetUser);
    }

    // ===== 6. LẤY DANH SÁCH THÔNG BÁO =====

    /**
     * Lấy danh sách thông báo của người dùng hiện tại.
     *
     * <p>
     * Nếu isRead == null: lấy tất cả thông báo.
     * Nếu isRead != null: lọc theo trạng thái đọc.
     * </p>
     *
     * @param isRead   trạng thái đọc, có thể null
     * @param pageable thông tin phân trang
     * @return danh sách thông báo
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(
            Boolean isRead,
            Pageable pageable) {

        permissionChecker.check(
                NOTIFICATION_RESOURCE,
                NOTIFICATION_READ_ACTION);

        CustomUserDetails currentUser = getCurrentUser();

        Page<Notification> page;

        if (isRead == null) {
            page = notificationRepository
                    .findByUser_UserIdOrderByCreatedAtDesc(
                            currentUser.getUserId(),
                            pageable);
        } else {
            page = notificationRepository
                    .findByUser_UserIdAndIsReadOrderByCreatedAtDesc(
                            currentUser.getUserId(),
                            isRead,
                            pageable);
        }

        List<NotificationResponse> items = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, items);
    }

    /**
     * Chuyển Notification entity sang response DTO.
     */
    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .entityId(notification.getEntityId())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    // ===== 7. ĐẾM THÔNG BÁO CHƯA ĐỌC =====

    /**
     * Đếm số lượng thông báo chưa đọc của người dùng hiện tại.
     */
    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        permissionChecker.check(
                NOTIFICATION_RESOURCE,
                NOTIFICATION_READ_ACTION);

        CustomUserDetails currentUser = getCurrentUser();

        long unreadCount = notificationRepository
                .countByUser_UserIdAndIsReadFalse(currentUser.getUserId());

        return UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
    }

    // ===== 8. ĐÁNH DẤU ĐÃ ĐỌC =====

    /**
     * Đánh dấu một notification là đã đọc.
     *
     * <p>
     * Quy tắc:
     * <ul>
     * <li>Notification không tồn tại → BusinessException.</li>
     * <li>Notification không thuộc user hiện tại → BusinessException.</li>
     * <li>Nếu chưa đọc → cập nhật isRead=true và readAt.</li>
     * <li>Nếu đã đọc → không thay đổi readAt.</li>
     * </ul>
     * </p>
     *
     * @param notificationId ID notification
     * @return notification sau khi cập nhật
     */
    @Override
    public NotificationResponse markAsRead(UUID notificationId) {
        permissionChecker.check(
                NOTIFICATION_RESOURCE,
                NOTIFICATION_READ_ACTION);

        CustomUserDetails currentUser = getCurrentUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(MSG_NOTIFICATION_NOT_FOUND));

        if (!notification.getUser()
                .getUserId()
                .equals(currentUser.getUserId())) {
            throw new BusinessException(MSG_NO_PERMISSION_TO_ACCESS);
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    // ===== 9. LẤY USER HIỆN TẠI =====

    /**
     * Lấy thông tin user hiện tại từ SecurityContext.
     */
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return (CustomUserDetails) authentication.getPrincipal();
    }

    // ===== 10. THÔNG BÁO MÃ TEM NGHI VẤN =====

    /**
     * Gửi thông báo khi một mã tem bị đánh dấu nghi vấn.
     *
     * @param traceCode mã tem bị nghi vấn
     */
    @Override
    public void sendSuspectTraceCodeNotification(TraceCode traceCode) {
        Shipment shipment = traceCode.getShipment();
        UUID organizationId = shipment.getOrganization().getOrganizationId();

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có người dùng có permission {}:{} để nhận "
                            + "thông báo mã tem nghi vấn. organizationId={}",
                    NOTIFICATION_RESOURCE,
                    NOTIFICATION_READ_ACTION,
                    organizationId);
            return;
        }

        String content = String.format(
                SUSPECT_NOTIFICATION_CONTENT_FORMAT,
                traceCode.getCodeValue(),
                traceCode.getSuspicionScore(),
                traceCode.getSuspicionReason() != null
                        ? traceCode.getSuspicionReason()
                        : "Không rõ");

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ALERT);
                    notification.setTitle(SUSPECT_NOTIFICATION_TITLE);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification mã tem nghi vấn. "
                        + "organizationId={}, traceCodeId={}",
                notifications.size(),
                organizationId,
                traceCode.getId());
    }

    // ===== 11. GHI LOG CẢNH BÁO =====

    /**
     * Gửi cảnh báo vào log khi có sự kiện quan trọng.
     *
     * @param message nội dung cảnh báo
     */
    @Override
    public void sendAlert(String message) {
        log.warn("CẢNH BÁO: {}", message);
    }

    // ===== 12. THÔNG BÁO ĐĂNG NHẬP BẤT THƯỜNG (NCL-01-CN-005) =====

    /**
     * Gửi thông báo khi phát hiện đăng nhập bất thường.
     *
     * @param anomaly bản ghi bất thường
     */
    @Override
    public void sendLoginAnomalyNotification(vn.nguongocso.auth.entity.LoginAnomaly anomaly) {
        List<User> recipients = getLoginSecurityRecipients(anomaly.getUser());

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có tài khoản nào được nhận thông báo đăng nhập bất thường. userId={}",
                    anomaly.getUser().getUserId());
            return;
        }

        String title = "Phát hiện đăng nhập bất thường";
        String content = String.format(
                "Tài khoản %s (%s) phát hiện hoạt động đăng nhập bất thường. "
                        + "Lý do: %s. Địa chỉ IP: %s. Quốc gia: %s.",
                anomaly.getUser().getUserName(),
                anomaly.getUser().getFullName(),
                anomaly.getReasonCode().toString(),
                anomaly.getIpAddress(),
                anomaly.getCountryCode() != null ? anomaly.getCountryCode() : "Không xác định");

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(vn.nguongocso.alert.enums.NotificationType.LOGIN_ANOMALY_DETECTED);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification bất thường đăng nhập. "
                        + "organizationId={}, userId={}, anomalyId={}",
                notifications.size(),
                anomaly.getOrganization().getOrganizationId(),
                anomaly.getUser().getUserId(),
                anomaly.getId());
    }

    /**
     * Gửi thông báo khi tài khoản bị khóa.
     *
     * @param accountLock bản ghi khóa tài khoản
     */
    @Override
    public void sendAccountLockedNotification(vn.nguongocso.auth.entity.AccountLock accountLock) {
        User lockedUser = accountLock.getUser();
        List<User> recipients = getLoginSecurityRecipients(lockedUser);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có tài khoản nào được nhận thông báo khóa tài khoản. userId={}",
                    lockedUser.getUserId());
            return;
        }

        String title = "Tài khoản bị khóa";
        String content = String.format(
                "Tài khoản %s (%s) đã bị khóa bởi %s. "
                        + "Lý do: %s. "
                        + "Vui lòng liên hệ với quản trị viên để mở khóa.",
                lockedUser.getUserName(),
                lockedUser.getFullName(),
                accountLock.getLockedBy().getUserName(),
                accountLock.getLockReason() != null ? accountLock.getLockReason() : "Không xác định");

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(vn.nguongocso.alert.enums.NotificationType.ACCOUNT_LOCKED);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification khóa tài khoản. "
                        + "userId={}, accountLockId={}",
                notifications.size(),
                lockedUser.getUserId(),
                accountLock.getId());
    }

    /**
     * Gửi thông báo khi tài khoản được mở khóa.
     *
     * @param accountLock bản ghi khóa tài khoản (status = UNLOCKED)
     */
    @Override
    public void sendAccountUnlockedNotification(vn.nguongocso.auth.entity.AccountLock accountLock) {
        User unlockedUser = accountLock.getUser();
        List<User> recipients = getLoginSecurityRecipients(unlockedUser);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có tài khoản nào được nhận thông báo mở khóa tài khoản. userId={}",
                    unlockedUser.getUserId());
            return;
        }

        String title = "Tài khoản đã được mở khóa";
        String content = String.format(
                "Tài khoản %s (%s) đã được mở khóa bởi %s vào lúc %s.",
                unlockedUser.getUserName(),
                unlockedUser.getFullName(),
                accountLock.getUnlockedBy() != null ? accountLock.getUnlockedBy().getUserName() : "Hệ thống",
                accountLock.getUnlockedAt() != null ? accountLock.getUnlockedAt() : "Không xác định");

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(vn.nguongocso.alert.enums.NotificationType.ACCOUNT_UNLOCKED);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification mở khóa tài khoản. "
                        + "userId={}, accountLockId={}",
                notifications.size(),
                unlockedUser.getUserId(),
                accountLock.getId());
    }

    /**
     * Gửi thông báo khi mã tem được mở khóa sau khi xác minh (NCL-08-CN-013).
     *
     * @param traceCode mã tem đã được mở khóa
     */
    @Override
    public void sendTraceCodeUnlockedNotification(TraceCode traceCode) {
        Shipment shipment = traceCode.getShipment();
        if (shipment == null || shipment.getOrganization() == null) {
            log.warn("Không tìm thấy thông tin tổ chức sở hữu mã tem {}", traceCode.getCodeValue());
            return;
        }

        UUID organizationId = shipment.getOrganization().getOrganizationId();

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có người dùng có permission {}:{} để nhận "
                            + "thông báo mở khóa mã tem. organizationId={}",
                    NOTIFICATION_RESOURCE,
                    NOTIFICATION_READ_ACTION,
                    organizationId);
            return;
        }

        String content = String.format(
                UNLOCK_NOTIFICATION_CONTENT_FORMAT,
                traceCode.getCodeValue(),
                traceCode.getUnlockedAt() != null ? traceCode.getUnlockedAt().toString() : LocalDateTime.now().toString(),
                traceCode.getUnlockConclusion() != null ? traceCode.getUnlockConclusion() : "Đã xác minh an toàn");

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ALERT);
                    notification.setTitle(UNLOCK_NOTIFICATION_TITLE);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification mở khóa mã tem. "
                        + "organizationId={}, traceCodeId={}",
                notifications.size(),
                organizationId,
                traceCode.getId());
    }

    // ===== 13. THÔNG BÁO KẾT QUẢ KIỂM NGHIỆM =====

    /**
     * Gửi thông báo cho Quản lý hợp tác xã khi lô sản xuất có kết quả
     * kiểm nghiệm ĐẠT.
     *
     * @param lotName        tên lô sản xuất
     * @param organizationId tổ chức sở hữu lô sản xuất
     */
    @Override
    public void sendInspectionPassedNotification(
            String lotName,
            UUID organizationId) {

        sendInspectionResultNotification(
                lotName,
                organizationId,
                NotificationType.INFO,
                INSPECTION_PASSED_TITLE,
                INSPECTION_PASSED_CONTENT_FORMAT,
                "đạt");
    }

    /**
     * Gửi cảnh báo cho Quản lý hợp tác xã khi lô sản xuất có kết quả
     * kiểm nghiệm KHÔNG ĐẠT (NCL-11-CN-005, QTN-30).
     *
     * @param lotName        tên lô sản xuất
     * @param organizationId tổ chức sở hữu lô sản xuất
     */
    @Override
    public void sendInspectionFailedNotification(
            String lotName,
            UUID organizationId) {

        sendInspectionResultNotification(
                lotName,
                organizationId,
                NotificationType.ALERT,
                INSPECTION_FAILED_TITLE,
                INSPECTION_FAILED_CONTENT_FORMAT,
                "không đạt");
    }

    /**
     * Tạo thông báo kết quả kiểm nghiệm cho các tài khoản được phép nhận
     * thông báo trong tổ chức sở hữu lô.
     */
    private void sendInspectionResultNotification(
            String lotName,
            UUID organizationId,
            NotificationType notificationType,
            String title,
            String contentFormat,
            String resultLabel) {

        if (organizationId == null) {
            log.warn(
                    "Không thể gửi thông báo kiểm nghiệm {}: organizationId null",
                    resultLabel);
            return;
        }

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có người dùng có permission {}:{} để nhận "
                            + "thông báo kiểm nghiệm {}. organizationId={}",
                    NOTIFICATION_RESOURCE,
                    NOTIFICATION_READ_ACTION,
                    resultLabel,
                    organizationId);
            return;
        }

        String content = String.format(contentFormat, lotName);

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(notificationType);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification kiểm nghiệm {}. "
                        + "organizationId={}, lotName={}",
                notifications.size(),
                resultLabel,
                organizationId,
                lotName);
    }

    // ===== 14. THÔNG BÁO PHIẾU BÀN GIAO (NCL-05-CN-008/CN-009) =====

    /**
     * Gửi thông báo vòng đời phiếu bàn giao tới mọi người dùng thuộc
     * tổ chức chỉ định có permission {@code notification:READ}.
     *
     * @param title          tiêu đề thông báo
     * @param content        nội dung thông báo
     * @param entityId       ID phiếu bàn giao để người dùng bấm vào
     *                       thông báo có thể mở chi tiết phiếu
     * @param organizationId tổ chức nhận thông báo
     */
    @Override
    public void sendHandoverNotification(
            String title,
            String content,
            UUID entityId,
            UUID organizationId) {

        if (organizationId == null) {
            log.warn(
                    "Không thể gửi thông báo bàn giao: organizationId null. title={}",
                    title);
            return;
        }

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có người dùng có permission {}:{} để nhận "
                            + "thông báo bàn giao. organizationId={}, title={}",
                    NOTIFICATION_RESOURCE,
                    NOTIFICATION_READ_ACTION,
                    organizationId,
                    title);
            return;
        }

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ALERT);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setEntityId(entityId);
                    notification.setIsRead(false);
                    notification.setReadAt(null);

                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info(
                "Đã tạo {} notification vòng đời phiếu bàn giao. "
                        + "organizationId={}, entityId={}",
                notifications.size(),
                organizationId,
                entityId);
    }

    @Override
    public int sendCodeRangeSupplementNotification(String title, String content, List<UUID> recipientIds) {
        // TODO: Implement when ready
        return 0;
    }

    @Override
    public int sendCertificationRejectionNotification(Certification certification, String rejectionReason) {
        // TODO: Implement when ready
        return 0;
    }

    @Override
    public int sendBulkRecallWorkflowNotification(
            String title,
            String content,
            UUID requestId,
            String action,
            List<vn.nguongocso.auth.entity.User> recipients) {
        // TODO: Implement when ready
        return 0;
    }

    @Override
    public int sendRecallCaseClosedNotification(String caseCode, List<UUID> recipientIds) {
        // TODO: Implement when ready
        return 0;
    }

    @Override
    public void sendInspectionExpiryNotification(
            vn.nguongocso.alert.entity.Alert alert,
            vn.nguongocso.farm.entity.ProductionLot lot,
            vn.nguongocso.certification.dto.response.InspectionValidityResponse validity) {
        // TODO: Implement when ready
    }
}
