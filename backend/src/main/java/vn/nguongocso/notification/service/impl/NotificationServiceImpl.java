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
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.LoginAnomaly;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.enums.InspectionValidityStatus;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
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

/**
 * Triển khai dịch vụ thông báo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String NOTIFICATION_RESOURCE = "notification";
    private static final String NOTIFICATION_READ_ACTION = "READ";

    private static final String NOTIFICATION_TITLE = "Cảnh báo tem quét bất thường";
    private static final String NOTIFICATION_CONTENT =
            "Hệ thống phát hiện mã truy xuất có dấu hiệu bị quét bất thường ở nhiều vị trí.";

    private static final String RECALL_TITLE = "Thông báo thu hồi lô hàng";
    private static final String LOT_RECALL_TITLE = "Thông báo thu hồi lô hàng";
    private static final String CERTIFICATION_REJECTED_TITLE = "Chứng nhận bị từ chối xác thực";

    private static final String MSG_NOTIFICATION_NOT_FOUND = "Thông báo không tồn tại.";
    private static final String MSG_NO_PERMISSION_TO_ACCESS = "Bạn không có quyền thao tác với thông báo này.";
    private static final String MSG_TRACE_CODE_NOT_FOUND = "Mã truy xuất không tồn tại.";
    private static final String MSG_CERTIFICATION_NOT_FOUND = "Chứng nhận không tồn tại.";

    private static final String SUSPECT_NOTIFICATION_TITLE = "Mã tem bị nghi vấn";
    private static final String SUSPECT_NOTIFICATION_CONTENT_FORMAT =
            "Mã tem %s bị đánh dấu nghi vấn (điểm: %d). Lý do: %s";

    private static final String UNLOCK_NOTIFICATION_TITLE = "Mã tem đã được mở khóa";
    private static final String UNLOCK_NOTIFICATION_CONTENT_FORMAT =
            "Mã tem %s đã được Quản trị viên mở khóa sau khi xác minh vào lúc %s. Kết luận: %s";

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

    private final NotificationRepository notificationRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final CertificationRepository certificationRepository;
    private final PermissionChecker permissionChecker;

    private record InspectionResultNotificationParams(
            String lotName,
            UUID organizationId,
            NotificationType notificationType,
            String title,
            String contentFormat,
            String resultLabel) {
    }

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

    @Override
    public void sendScanAnomalyNotification(Alert alert) {
        TraceCode traceCode = traceCodeRepository.findById(alert.getRelatedEntityId())
                .orElseThrow(() -> new BusinessException(MSG_TRACE_CODE_NOT_FOUND));

        Shipment shipment = traceCode.getShipment();
        UUID organizationId = shipment.getOrganization().getOrganizationId();

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn("Không có người dùng có permission {}:{} để nhận cảnh báo quét bất thường. organizationId={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, organizationId);
            return;
        }

        List<Notification> notifications = recipients.stream()
                .map(this::buildScanAnomalyNotification)
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification cảnh báo quét bất thường. organizationId={}, traceCodeId={}",
                notifications.size(), organizationId, traceCode.getId());
    }

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

    @Override
    public void sendShipmentRecallNotification(Recall recall) {
        Shipment shipment = recall.getShipment();
        UUID organizationId = shipment.getOrganization().getOrganizationId();

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn("Không có người dùng có permission {}:{} để nhận thông báo thu hồi. "
                            + "organizationId={}, shipmentId={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, organizationId, shipment.getId());
            return;
        }

        List<Notification> notifications = recipients.stream()
                .map(user -> buildRecallNotification(recall, user))
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification thu hồi lô hàng. organizationId={}, shipmentId={}",
                notifications.size(), organizationId, shipment.getId());
    }

    private Notification buildRecallNotification(Recall recall, User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.ALERT);
        notification.setTitle(RECALL_TITLE);
        notification.setContent("Lô hàng \""
                + recall.getShipment().getName()
                + "\" đã bị thu hồi. Lý do: "
                + recall.getReason());
        notification.setIsRead(false);
        notification.setReadAt(null);
        return notification;
    }

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

        log.info("Đã tạo {} notification thu hồi lô hàng. shipmentName={}",
                notifications.size(), shipmentName);

        return notifications.size();
    }

    @Override
    public void sendCertificationExpiryNotification(Alert alert) {
        Certification certification = certificationRepository.findById(alert.getRelatedEntityId())
                .orElseThrow(() -> new BusinessException(MSG_CERTIFICATION_NOT_FOUND));

        UUID organizationId = certification.getOrganization().getOrganizationId();
        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn("Không có người dùng có permission {}:{} để nhận thông báo chứng nhận. "
                            + "organizationId={}, certificationId={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, organizationId, certification.getId());
            return;
        }

        List<Notification> notifications = recipients.stream()
                .map(user -> buildCertificationExpiryNotification(alert, certification, user))
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification chứng nhận. organizationId={}, certificationId={}",
                notifications.size(), organizationId, certification.getId());
    }

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
            notification.setContent("Chứng nhận \""
                    + certification.getName()
                    + "\" ("
                    + certification.getCode()
                    + ") đã hết hạn vào ngày "
                    + certification.getExpiryDate()
                    + ".");
        } else {
            notification.setTitle("Chứng nhận sắp hết hạn");
            notification.setContent("Chứng nhận \""
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

    private List<User> getNotificationRecipients(UUID organizationId) {
        return organizationUserRepository.findUsersByPermission(
                organizationId,
                NOTIFICATION_RESOURCE,
                NOTIFICATION_READ_ACTION);
    }

    private List<User> getLoginSecurityRecipients(User targetUser) {
        return targetUser == null ? List.of() : List.of(targetUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Boolean isRead, Pageable pageable) {
        permissionChecker.check(NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION);

        CustomUserDetails currentUser = getCurrentUser();
        Page<Notification> page;

        if (isRead == null) {
            page = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(
                    currentUser.getUserId(), pageable);
        } else {
            page = notificationRepository.findByUser_UserIdAndIsReadOrderByCreatedAtDesc(
                    currentUser.getUserId(), isRead, pageable);
        }

        List<NotificationResponse> items = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, items);
    }

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

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        permissionChecker.check(NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION);

        CustomUserDetails currentUser = getCurrentUser();
        long unreadCount = notificationRepository.countByUser_UserIdAndIsReadFalse(
                currentUser.getUserId());

        return UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    public NotificationResponse markAsRead(UUID notificationId) {
        permissionChecker.check(NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION);

        CustomUserDetails currentUser = getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(MSG_NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new BusinessException(MSG_NO_PERMISSION_TO_ACCESS);
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) authentication.getPrincipal();
    }

    @Override
    public void sendSuspectTraceCodeNotification(TraceCode traceCode) {
        Shipment shipment = traceCode.getShipment();
        UUID organizationId = shipment.getOrganization().getOrganizationId();

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn("Không có người dùng có permission {}:{} để nhận thông báo mã tem nghi vấn. organizationId={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, organizationId);
            return;
        }

        String content = String.format(
                SUSPECT_NOTIFICATION_CONTENT_FORMAT,
                traceCode.getCodeValue(),
                traceCode.getSuspicionScore(),
                traceCode.getSuspicionReason() != null ? traceCode.getSuspicionReason() : "Không rõ");

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

        log.info("Đã tạo {} notification mã tem nghi vấn. organizationId={}, traceCodeId={}",
                notifications.size(), organizationId, traceCode.getId());
    }

    @Override
    public void sendAlert(String message) {
        log.warn("CẢNH BÁO: {}", message);
    }

    @Override
    public void sendLoginAnomalyNotification(LoginAnomaly anomaly) {
        List<User> recipients = getLoginSecurityRecipients(anomaly.getUser());

        if (recipients.isEmpty()) {
            log.warn("Không có tài khoản nào được nhận thông báo đăng nhập bất thường. userId={}",
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
                anomaly.getCountryCode() != null ? anomaly.getCountryCode() : "Không xác định"
        );

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.LOGIN_ANOMALY_DETECTED);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification bất thường đăng nhập. organizationId={}, userId={}, anomalyId={}",
                notifications.size(),
                anomaly.getOrganization().getOrganizationId(),
                anomaly.getUser().getUserId(),
                anomaly.getId());
    }

    @Override
    public void sendAccountLockedNotification(AccountLock accountLock) {
        User lockedUser = accountLock.getUser();
        List<User> recipients = getLoginSecurityRecipients(lockedUser);

        if (recipients.isEmpty()) {
            log.warn("Không có tài khoản nào được nhận thông báo khóa tài khoản. userId={}",
                    lockedUser.getUserId());
            return;
        }

        String title = "Tài khoản bị khóa";
        String content = String.format(
                "Tài khoản %s (%s) đã bị khóa bởi %s. Lý do: %s. Vui lòng liên hệ với quản trị viên để mở khóa.",
                lockedUser.getUserName(),
                lockedUser.getFullName(),
                accountLock.getLockedBy().getUserName(),
                accountLock.getLockReason() != null ? accountLock.getLockReason() : "Không xác định"
        );

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ACCOUNT_LOCKED);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification khóa tài khoản. userId={}, accountLockId={}",
                notifications.size(), lockedUser.getUserId(), accountLock.getId());
    }

    @Override
    public void sendAccountUnlockedNotification(AccountLock accountLock) {
        User unlockedUser = accountLock.getUser();
        List<User> recipients = getLoginSecurityRecipients(unlockedUser);

        if (recipients.isEmpty()) {
            log.warn("Không có tài khoản nào được nhận thông báo mở khóa tài khoản. userId={}",
                    unlockedUser.getUserId());
            return;
        }

        String title = "Tài khoản đã được mở khóa";
        String content = String.format(
                "Tài khoản %s (%s) đã được mở khóa bởi %s vào lúc %s.",
                unlockedUser.getUserName(),
                unlockedUser.getFullName(),
                accountLock.getUnlockedBy() != null ? accountLock.getUnlockedBy().getUserName() : "Hệ thống",
                accountLock.getUnlockedAt() != null ? accountLock.getUnlockedAt() : "Không xác định"
        );

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ACCOUNT_UNLOCKED);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification mở khóa tài khoản. userId={}, accountLockId={}",
                notifications.size(), unlockedUser.getUserId(), accountLock.getId());
    }

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
            log.warn("Không có người dùng có permission {}:{} để nhận thông báo mở khóa mã tem. organizationId={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, organizationId);
            return;
        }

        String content = String.format(
                UNLOCK_NOTIFICATION_CONTENT_FORMAT,
                traceCode.getCodeValue(),
                traceCode.getUnlockedAt() != null
                        ? traceCode.getUnlockedAt().toString()
                        : LocalDateTime.now().toString(),
                traceCode.getUnlockConclusion() != null
                        ? traceCode.getUnlockConclusion()
                        : "Đã xác minh an toàn");

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

        log.info("Đã tạo {} notification mở khóa mã tem. organizationId={}, traceCodeId={}",
                notifications.size(), organizationId, traceCode.getId());
    }

    @Override
    public void sendInspectionPassedNotification(String lotName, UUID organizationId) {
        sendInspectionResultNotification(new InspectionResultNotificationParams(
                lotName,
                organizationId,
                NotificationType.INFO,
                INSPECTION_PASSED_TITLE,
                INSPECTION_PASSED_CONTENT_FORMAT,
                "đạt"));
    }

    @Override
    public void sendInspectionFailedNotification(String lotName, UUID organizationId) {
        sendInspectionResultNotification(new InspectionResultNotificationParams(
                lotName,
                organizationId,
                NotificationType.ALERT,
                INSPECTION_FAILED_TITLE,
                INSPECTION_FAILED_CONTENT_FORMAT,
                "không đạt"));
    }

    private void sendInspectionResultNotification(InspectionResultNotificationParams params) {
        if (params.organizationId() == null) {
            log.warn("Không thể gửi thông báo kiểm nghiệm {}: organizationId null", params.resultLabel());
            return;
        }

        List<User> recipients = getNotificationRecipients(params.organizationId());

        if (recipients.isEmpty()) {
            log.warn("Không có người dùng có permission {}:{} để nhận thông báo kiểm nghiệm {}. organizationId={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, params.resultLabel(), params.organizationId());
            return;
        }

        String content = String.format(params.contentFormat(), params.lotName());

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(params.notificationType());
                    notification.setTitle(params.title());
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification kiểm nghiệm {}. organizationId={}, lotName={}",
                notifications.size(), params.resultLabel(), params.organizationId(), params.lotName());
    }

    @Override
    public void sendHandoverNotification(
            String title,
            String content,
            UUID entityId,
            UUID organizationId) {
        if (organizationId == null) {
            log.warn("Không thể gửi thông báo bàn giao: organizationId null. title={}", title);
            return;
        }

        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn("Không có người dùng có permission {}:{} để nhận thông báo bàn giao. "
                            + "organizationId={}, title={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, organizationId, title);
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

        log.info("Đã tạo {} notification bàn giao. organizationId={}, title={}",
                notifications.size(), organizationId, title);
    }

    @Override
    public int sendCodeRangeSupplementNotification(String title, String content, List<UUID> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            log.warn("Không có người dùng để nhận thông báo cấp bổ sung dải mã. title={}", title);
            return 0;
        }

        List<User> recipients = userRepository.findAllById(recipientIds);
        if (recipients.isEmpty()) {
            return 0;
        }

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ALERT);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification cấp bổ sung dải mã. title={}",
                notifications.size(), title);

        return notifications.size();
    }

    @Override
    public int sendCertificationRejectionNotification(Certification certification, String rejectionReason) {
        if (certification == null || certification.getOrganization() == null) {
            log.warn("Không thể gửi thông báo từ chối chứng nhận: chứng nhận hoặc tổ chức null");
            return 0;
        }

        UUID organizationId = certification.getOrganization().getOrganizationId();
        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn("Không có người dùng có permission {}:{} để nhận thông báo từ chối chứng nhận. "
                            + "organizationId={}, certificationId={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, organizationId, certification.getId());
            return 0;
        }

        String certIdentifier = certification.getName() != null
                ? certification.getName()
                : certification.getCode();
        String content = String.format(
                "Chứng nhận \"%s\" (%s) đã bị từ chối xác thực. Lý do: %s. "
                        + "Vui lòng kiểm tra và nộp lại thông tin/chứng nhận phù hợp.",
                certIdentifier,
                certification.getCode(),
                rejectionReason != null ? rejectionReason : "Không có lý do chi tiết");

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ALERT);
                    notification.setTitle(CERTIFICATION_REJECTED_TITLE);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification từ chối chứng nhận. organizationId={}, certificationId={}",
                notifications.size(), organizationId, certification.getId());

        return notifications.size();
    }

    @Override
    public int sendBulkRecallWorkflowNotification(
            String title,
            String content,
            UUID requestId,
            String action,
            List<User> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return 0;
        }

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.INFO);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification workflow bulk recall. action={}, requestId={}",
                notifications.size(), action, requestId);

        return notifications.size();
    }

    @Override
    public void sendInspectionExpiryNotification(
            Alert alert,
            ProductionLot lot,
            InspectionValidityResponse validity) {
        if (lot == null || lot.getOrganization() == null || lot.getOrganization().getOrganizationId() == null) {
            log.warn("Không thể gửi thông báo kiểm nghiệm: lô sản xuất hoặc tổ chức không hợp lệ.");
            return;
        }

        UUID organizationId = lot.getOrganization().getOrganizationId();
        List<User> recipients = getNotificationRecipients(organizationId);

        if (recipients.isEmpty()) {
            log.warn("Không có người dùng có permission {}:{} để nhận thông báo kiểm nghiệm. "
                            + "organizationId={}, lotId={}",
                    NOTIFICATION_RESOURCE, NOTIFICATION_READ_ACTION, organizationId, lot.getId());
            return;
        }

        boolean isExpired = (alert != null && alert.getType() == AlertType.INSPECTION_EXPIRED)
                || (validity != null && validity.getStatus() == InspectionValidityStatus.EXPIRED);

        String title = isExpired ? INSPECTION_EXPIRED_TITLE : INSPECTION_EXPIRING_TITLE;
        String content;
        if (isExpired) {
            String expiryStr = (validity != null && validity.getExpiryDate() != null)
                    ? validity.getExpiryDate().toString()
                    : "N/A";
            String criteriaStr = (validity != null && validity.getExpiredCriteria() != null
                    && !validity.getExpiredCriteria().isEmpty())
                    ? String.join(", ", validity.getExpiredCriteria())
                    : null;
            if (criteriaStr != null) {
                content = String.format(
                        "Lô sản xuất \"%s\" có kết quả kiểm nghiệm đã hết hiệu lực vào ngày %s (tiêu chí: %s). "
                                + "Vui lòng tạo yêu cầu kiểm nghiệm mới để đảm bảo tính hợp lệ của sản phẩm.",
                        lot.getName(), expiryStr, criteriaStr);
            } else {
                content = String.format(INSPECTION_EXPIRED_CONTENT_FORMAT, lot.getName(), expiryStr);
            }
        } else {
            long daysLeft = (validity != null && validity.getDaysUntilExpiry() != null)
                    ? validity.getDaysUntilExpiry()
                    : 0;
            String expiryStr = (validity != null && validity.getExpiryDate() != null)
                    ? validity.getExpiryDate().toString()
                    : "N/A";
            String criteriaStr = (validity != null && validity.getExpiringCriteria() != null
                    && !validity.getExpiringCriteria().isEmpty())
                    ? String.join(", ", validity.getExpiringCriteria())
                    : null;
            if (criteriaStr != null) {
                content = String.format(
                        "Lô sản xuất \"%s\" có kết quả kiểm nghiệm sẽ hết hiệu lực sau %d ngày "
                                + "(ngày hết hạn: %s, tiêu chí: %s). Vui lòng chủ động lập kế hoạch kiểm nghiệm mới.",
                        lot.getName(), daysLeft, expiryStr, criteriaStr);
            } else {
                content = String.format(INSPECTION_EXPIRING_CONTENT_FORMAT, lot.getName(), daysLeft, expiryStr);
            }
        }

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ALERT);
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} notification cảnh báo kiểm nghiệm (type={}). organizationId={}, lotId={}",
                notifications.size(),
                isExpired ? AlertType.INSPECTION_EXPIRED : AlertType.INSPECTION_EXPIRING,
                organizationId,
                lot.getId());
    }

    @Override
    public int sendRecallCaseClosedNotification(String caseCode, List<UUID> recipientIds) {
        if (caseCode == null || recipientIds == null || recipientIds.isEmpty()) {
            log.warn("Không có người dùng để nhận thông báo kết thúc vụ việc thu hồi. caseCode={}", caseCode);
            return 0;
        }

        List<User> recipients = userRepository.findAllById(recipientIds);
        if (recipients.isEmpty()) {
            return 0;
        }

        String content = String.format("Vụ việc thu hồi %s đã được xử lý và kết thúc.", caseCode);

        List<Notification> notifications = recipients.stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(NotificationType.ALERT);
                    notification.setTitle("Thông báo kết thúc vụ việc thu hồi");
                    notification.setContent(content);
                    notification.setIsRead(false);
                    notification.setReadAt(null);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Đã tạo {} thông báo kết thúc vụ việc thu hồi. caseCode={}",
                notifications.size(), caseCode);

        return notifications.size();
    }
}
