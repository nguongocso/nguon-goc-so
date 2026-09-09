package vn.nguongocso.recall.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.entity.BulkRecallRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BulkRecallNotificationService {

    /**
     * Mã vai trò Quản lý HTX (xem V14__seed_roles.sql).
     * Chỉ vai trò này được nhận thông báo workflow yêu cầu thu hồi.
     */
    private static final String ORG_MANAGER_ROLE = "VT-02";

    private final NotificationService notificationService;
    private final OrganizationUserRepository organizationUserRepository;

        /**
     * Gửi thông báo liên quan đến quy trình yêu cầu thu hồi hàng loạt.
     *
     * @param actor    người thực hiện hành động
     * @param action   hành động: CREATE, APPROVE, REJECT
     * @param request  yêu cầu thu hồi hàng loạt
     * @param requestId ID của yêu cầu thu hồi
     */
    public void sendBulkRecallWorkflowNotification(
            CustomUserDetails actor,
            String action,
            BulkRecallRequest request,
            UUID requestId) {

        if (actor == null || request == null) {
            log.warn("Không thể gửi thông báo workflow: actor hoặc request null");
            return;
        }

        UUID organizationId = resolveOrganizationId(actor);

        if (organizationId == null) {
            log.warn(
                    "Không thể xác định tổ chức cho userId={}",
                    actor.getUserId()
            );
            return;
        }

        List<User> recipients = resolveEligibleManagers(
                organizationId,
                actor.getUserId()
        );

        if (recipients.isEmpty()) {
            log.warn(
                    "Không có quản lý phù hợp để nhận thông báo trong organizationId={}",
                    organizationId
            );
            return;
        }

        String title = buildNotificationTitle(action);
        String content = buildNotificationContent(
                actor.getFullName(),
                action,
                request
        );

        notificationService.sendBulkRecallWorkflowNotification(
                title,
                content,
                requestId,
                action,
                recipients
        );

        log.info(
                "Đã gửi thông báo workflow thu hồi hàng loạt. action={}, requestId={}, recipientCount={}",
                action,
                requestId,
                recipients.size()
        );
    }

    /**
     * Gửi thông báo đến doanh nghiệp thu mua.
     *
     * Hiện tại chưa triển khai logic gửi thông báo.
     *
          * @param request yêu cầu thu hồi hàng loạt
     */
    public void sendBulkRecallNotificationToPurchasingBusiness(
            BulkRecallRequest request) {

        if (request == null) {
            log.warn(
                    "Không thể gửi thông báo đến doanh nghiệp thu mua: request null"
            );
            return;
        }

        log.info(
                "Chưa triển khai gửi thông báo đến doanh nghiệp thu mua cho requestId={}",
                request.getId()
        );
    }

    /**
     * Xác định organization của người thực hiện.
     *
     * Ưu tiên lấy organizationId trực tiếp từ CustomUserDetails.
     * Nếu không có thì tìm organization đang hoạt động của người dùng.
     */
    private UUID resolveOrganizationId(CustomUserDetails user) {

        if (user.getOrganizationId() != null) {
            return user.getOrganizationId();
        }

        return organizationUserRepository
                .findByUser_UserIdAndStatus(
                        user.getUserId(),
                        OrganizationUserStatus.ACTIVE
                )
                .stream()
                .findFirst()
                .map(organizationUser -> organizationUser
                        .getOrganization()
                        .getOrganizationId())
                .orElse(null);
    }

    /**
     * Lấy danh sách Quản lý HTX đủ điều kiện nhận thông báo workflow yêu cầu thu hồi.
     *
     * <p>
     * Điều kiện recipient:
     * <ul>
     * <li>Cùng tổ chức với actor.</li>
     * <li>Vai trò là Quản lý HTX (VT-02).</li>
     * <li>Trạng thái membership ACTIVE.</li>
     * <li>Không phải chính actor thực hiện hành động.</li>
     * </ul>
     * NGSK (VT-03) và các vai trò khác không thuộc nhóm Quản lý HTX
     * không được nhận thông báo này.
     * </p>
     *
     * <p>
     * Lưu ý: không thay đổi notification thu hồi lô dành cho VT-03
     * ({@code NotificationService#sendShipmentRecallNotification} và
     * {@code NotificationService#sendRecallNotification} vẫn giữ nguyên
     * recipient theo nghiệp vụ hiện tại).
     * </p>
     */
    private List<User> resolveEligibleManagers(
            UUID organizationId,
            UUID actorId) {

        List<OrganizationUser> members = organizationUserRepository
                .findByOrganization_OrganizationIdAndStatus(
                        organizationId,
                        OrganizationUserStatus.ACTIVE);

        return members.stream()
                .filter(ou -> ou.getRole() != null
                        && ORG_MANAGER_ROLE.equals(ou.getRole().getCode()))
                .map(OrganizationUser::getUser)
                .filter(user -> user != null
                        && user.getUserId() != null
                        && !user.getUserId().equals(actorId))
                .distinct()
                .toList();
    }

    /**
     * Xây dựng tiêu đề thông báo theo hành động.
     */
    private String buildNotificationTitle(String action) {

        return switch (action) {
            case "CREATE" ->
                    "Yêu cầu thu hồi hàng loạt mới";

            case "APPROVE" ->
                    "Yêu cầu thu hồi hàng loạt được phê duyệt";

            case "REJECT" ->
                    "Yêu cầu thu hồi hàng loạt bị từ chối";

            default ->
                    "Cập nhật yêu cầu thu hồi hàng loạt";
        };
    }

    /**
     * Xây dựng nội dung thông báo theo hành động.
     */
    private String buildNotificationContent(
            String actorFullName,
            String action,
            BulkRecallRequest request) {

        String lotName = request.getProductionLot() != null
                ? request.getProductionLot().getName()
                : "không xác định";

        return switch (action) {

            case "CREATE" -> String.format(
                    "%s đã gửi yêu cầu thu hồi hàng loạt cho lô sản xuất %s. Lý do: %s",
                    actorFullName,
                    lotName,
                    request.getReason() != null
                            ? request.getReason()
                            : "không có"
            );

            case "APPROVE" -> String.format(
                    "%s đã phê duyệt yêu cầu thu hồi hàng loạt cho lô sản xuất %s.",
                    actorFullName,
                    lotName
            );

            case "REJECT" -> {

                                String baseContent = String.format(
                        "%s đã từ chối yêu cầu thu hồi hàng loạt cho lô sản xuất %s.",
                        actorFullName,
                        lotName
                );

                if (request.getRejectionReason() != null
                        && !request.getRejectionReason().isBlank()) {

                    yield baseContent
                            + " Lý do: "
                            + request.getRejectionReason();
                }

                yield baseContent;
            }

            default -> String.format(
                    "%s đã thực hiện hành động trên yêu cầu thu hồi hàng loạt cho lô sản xuất %s.",
                    actorFullName,
                    lotName
            );
        };
    }
}
