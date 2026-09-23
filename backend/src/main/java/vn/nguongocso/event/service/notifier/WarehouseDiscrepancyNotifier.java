package vn.nguongocso.event.service.notifier;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Component chuyên trách gửi thông báo cảnh báo chênh lệch số lượng khi nhập kho.
 *
 * <p>Hoạt động theo mô hình non-blocking: sự cố phát sinh khi gửi thông báo
 * được bắt lại an toàn và không làm gián đoạn hoặc rollback giao dịch nhập kho.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseDiscrepancyNotifier {

    private static final double DISCREPANCY_THRESHOLD_PERCENT = 2.0;

    private final OrganizationUserRepository organizationUserRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Gửi thông báo cảnh báo chênh lệch nhập kho tới người dùng quản lý HTX (VT-02).
     *
     * @param shipment           lô hàng nhập kho
     * @param declaredQuantity   số lượng khai báo
     * @param receivedQuantity   số lượng thực nhận
     * @param discrepancy        chênh lệch khối lượng
     * @param discrepancyPercent tỷ lệ phần trăm chênh lệch
     * @param reason             lý do giải trình nếu có
     * @return true nếu gửi thành công, false nếu thất bại hoặc không có người nhận
     */
    public boolean sendDiscrepancyNotification(Shipment shipment, double declaredQuantity,
            double receivedQuantity, double discrepancy, double discrepancyPercent,
            String reason) {
        try {
            UUID orgId = shipment.getOrganization().getOrganizationId();

            List<User> recipients = organizationUserRepository
                    .findAllByOrganization_OrganizationIdAndRole_Code(orgId, RoleCode.ORG_MANAGER)
                    .stream()
                    .map(ou -> ou.getUser())
                    .filter(Objects::nonNull)
                    .toList();

            if (recipients.isEmpty()) {
                log.warn("Không tìm thấy người dùng VT-02 trong tổ chức {} để gửi thông báo chênh lệch.",
                        orgId);
                return false;
            }

            String title = "Cảnh báo chênh lệch số lượng nhập kho";
            String content = String.format(
                    "Lô hàng \"%s\" có chênh lệch số lượng khi nhập kho.\n"
                            + "- Số lượng khai báo: %.1f kg\n"
                            + "- Số lượng thực nhận: %.1f kg\n"
                            + "- Chênh lệch: %.1f kg (%.1f%%)\n"
                            + "- Ngưỡng cho phép: %.1f%%\n"
                            + "%s",
                    shipment.getName(),
                    declaredQuantity,
                    receivedQuantity,
                    discrepancy,
                    discrepancyPercent,
                    DISCREPANCY_THRESHOLD_PERCENT,
                    reason != null && !reason.isBlank()
                            ? "- Lý do: " + reason
                            : "");

            List<Notification> notifications = recipients.stream()
                    .map(user -> {
                        Notification notification = new Notification();
                        notification.setUser(user);
                        notification.setType(vn.nguongocso.alert.enums.NotificationType.ALERT);
                        notification.setTitle(title);
                        notification.setContent(content);
                        notification.setIsRead(false);
                        notification.setReadAt(null);
                        return notification;
                    })
                    .toList();

            notificationRepository.saveAll(notifications);
            log.info("Đã gửi {} thông báo chênh lệch nhập kho cho tổ chức {}. shipmentId={}",
                    notifications.size(), orgId, shipment.getId());
            return true;

        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo chênh lệch nhập kho: {}", e.getMessage(), e);
            return false;
        }
    }
}
