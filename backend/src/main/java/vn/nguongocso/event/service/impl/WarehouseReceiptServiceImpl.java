package vn.nguongocso.event.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.annotation.Auditable;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.dto.request.WarehouseReceiptRequest;
import vn.nguongocso.event.dto.response.WarehouseReceiptResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.WarehouseReceiptService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation của dịch vụ nhập kho và đối chiếu số lượng.
 *
 * @author Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseReceiptServiceImpl implements WarehouseReceiptService {

    private static final double DISCREPANCY_THRESHOLD_PERCENT = 2.0;

    private final TraceCodeRepository traceCodeRepository;
    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final EventValidationService eventValidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationRepository notificationRepository;
    private final OrganizationUserRepository organizationUserRepository;

    @Override
    @Transactional
    @Auditable(action = "RECORD_WAREHOUSE_RECEIPT", entityType = "CHAIN_EVENT", description = "'Ghi nhận nhập kho cho mã tem: ' + #request.codeValue + ', Số lượng thực nhận: ' + #request.receivedQuantity")
    public WarehouseReceiptResponse recordWarehouseReceipt(WarehouseReceiptRequest request, CustomUserDetails currentUser) {

        // 1. Validate role: only VT-04 (Procurement Company)
        if (!RoleCode.PROCUREMENT.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Chỉ Doanh nghiệp thu mua mới được ghi sự kiện nhập kho.");
        }

        // 2. Find TraceCode by codeValue
        TraceCode traceCode = traceCodeRepository.findByCodeValue(request.getCodeValue())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Mã lô hàng không tồn tại."));

        // 3. Find associated Shipment
        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new BusinessException("Mã truy xuất chưa được gắn với lô hàng.");
        }

        // 4. Validate shipment status (QTN-05)
        try {
            if (shipment.getStatus() == ShipmentStatus.RECALLED) {
                throw new BusinessException("Lô hàng chưa được kích hoạt hoặc đã bị thu hồi, không thể ghi nhận nhập kho.");
            }
            if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
                throw new BusinessException("Lô hàng chưa được kích hoạt hoặc đã bị thu hồi, không thể ghi nhận nhập kho.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(shipment.getId(), shipment.getName(),
                    ChainEventType.WAREHOUSE_RECEIPT, e.getMessage(), currentUser);
            throw e;
        }

        // 5. Validate procurement relationship: must have PROCUREMENT event by same organization's VT-04
        validateProcurementRelationship(shipment, currentUser);

        // 6. Validate actual received quantity > 0 (also handled by @Positive annotation)
        if (request.getReceivedQuantity() == null || request.getReceivedQuantity() <= 0) {
            throw new BusinessException("Số lượng thực nhận phải lớn hơn 0.");
        }

        // 7. Calculate discrepancy
        double declaredQuantity = (double) shipment.getTotalQuantity();
        double receivedQuantity = request.getReceivedQuantity();
        double discrepancy = receivedQuantity - declaredQuantity;

        // Handle edge case: zero recorded quantity
        double discrepancyPercent;
        if (declaredQuantity == 0) {
            discrepancyPercent = receivedQuantity > 0 ? 100.0 : 0.0;
        } else {
            discrepancyPercent = (discrepancy / declaredQuantity) * 100.0;
        }

        double absoluteDiscrepancyPercent = Math.abs(discrepancyPercent);
        boolean isDiscrepancyExceeded = absoluteDiscrepancyPercent > DISCREPANCY_THRESHOLD_PERCENT;
        boolean reasonRequired = isDiscrepancyExceeded;

        // 8. Validate reason if discrepancy exceeds threshold
        if (isDiscrepancyExceeded) {
            if (request.getReason() == null || request.getReason().isBlank()) {
                Map<String, Object> errorDetails = new LinkedHashMap<>();
                errorDetails.put("declaredQuantity", declaredQuantity);
                errorDetails.put("receivedQuantity", receivedQuantity);
                errorDetails.put("discrepancyPercent", Math.round(discrepancyPercent * 100.0) / 100.0);
                errorDetails.put("threshold", DISCREPANCY_THRESHOLD_PERCENT);
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Chênh lệch số lượng vượt ngưỡng cho phép (2%). Vui lòng cung cấp lý do chênh lệch.",
                        errorDetails);
            }
        }

        // 9. Resolve receipt date
        LocalDate receiptDate = request.getReceiptDate() != null ? request.getReceiptDate() : LocalDate.now();

        // 10. Build eventData JSON
        Map<String, Object> eventDataMap = new LinkedHashMap<>();
        eventDataMap.put("shipmentId", shipment.getId().toString());
        eventDataMap.put("shipmentName", shipment.getName());
        eventDataMap.put("declaredQuantity", declaredQuantity);
        eventDataMap.put("receivedQuantity", receivedQuantity);
        eventDataMap.put("discrepancy", Math.round(discrepancy * 100.0) / 100.0);
        eventDataMap.put("discrepancyPercent", Math.round(discrepancyPercent * 100.0) / 100.0);
        eventDataMap.put("threshold", DISCREPANCY_THRESHOLD_PERCENT);
        eventDataMap.put("isDiscrepancyExceeded", isDiscrepancyExceeded);
        eventDataMap.put("conditionNote", request.getConditionNote());
        eventDataMap.put("receiptDate", receiptDate.toString());
        if (request.getReason() != null && !request.getReason().isBlank()) {
            eventDataMap.put("reason", request.getReason());
        }

        String eventDataJson;
        try {
            eventDataJson = objectMapper.writeValueAsString(eventDataMap);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Lỗi chuyển đổi dữ liệu sự kiện sang chuỗi JSON.");
        }

        // 11. Get actor
        User actor = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người ghi nhận."));

        // 12. Create and save ChainEvent
        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_RECEIPT)
                .eventData(eventDataJson)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        chainEvent = chainEventRepository.save(chainEvent);

        // 13. Publish activity log
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("CREATE")
                .description("Ghi sự kiện nhập kho cho lô hàng " + shipment.getName())
                .entityType("ChainEvent")
                .entityId(chainEvent.getId().toString())
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());

        // 14. Send notification if discrepancy exceeded
        boolean notificationSent = false;
        if (isDiscrepancyExceeded) {
            notificationSent = sendDiscrepancyNotification(shipment, declaredQuantity,
                    receivedQuantity, discrepancy, discrepancyPercent, request.getReason());
        }

        // 15. Build response
        WarehouseReceiptResponse.WarehouseReceiptResponseBuilder builder = WarehouseReceiptResponse.builder()
                .id(chainEvent.getId())
                .eventType(ChainEventType.WAREHOUSE_RECEIPT)
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .declaredQuantity(declaredQuantity)
                .receivedQuantity(receivedQuantity)
                .discrepancy(Math.round(discrepancy * 100.0) / 100.0)
                .discrepancyPercent(Math.round(discrepancyPercent * 100.0) / 100.0)
                .isDiscrepancyExceeded(isDiscrepancyExceeded)
                .reasonRequired(reasonRequired)
                .conditionNote(request.getConditionNote())
                .receiptDate(receiptDate)
                .recordedAt(chainEvent.getRecordedAt())
                .recordedBy(actor.getFullName())
                .notificationSent(notificationSent);

        if (request.getReason() != null && !request.getReason().isBlank()) {
            builder.reason(request.getReason());
        }

        return builder.build();
    }

    /**
     * Validates that the current procurement company has a procurement
     * relationship with the shipment.
     *
     * A procurement relationship exists when at least one PROCUREMENT
     * event for this shipment was recorded by a user in the current user's
     * organization.
     */
    private void validateProcurementRelationship(Shipment shipment, CustomUserDetails currentUser) {
        List<ChainEvent> procurementEvents = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .filter(e -> e.getEventType() == ChainEventType.PROCUREMENT)
                .toList();

        if (procurementEvents.isEmpty()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi nhận nhập kho cho lô hàng này. Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.");
        }

        // Check if any procurement event was recorded by a user in the same organization
        UUID currentOrgId = currentUser.getOrganizationId();

        // Collect unique recorder userIds from procurement events
        List<UUID> recorderIds = procurementEvents.stream()
                .map(e -> e.getRecordedBy().getUserId())
                .distinct()
                .toList();

        // Check if any of those recorders belong to the current user's organization
        boolean hasRelationship = recorderIds.stream()
                .anyMatch(recorderId -> organizationUserRepository
                        .findByOrganization_OrganizationIdAndUser_UserId(currentOrgId, recorderId)
                        .isPresent());

        if (!hasRelationship) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi nhận nhập kho cho lô hàng này. Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.");
        }
    }

    /**
     * Sends discrepancy notification to VT-02 (Quản lý HTX) users
     * in the shipment's organization.
     */
    private boolean sendDiscrepancyNotification(Shipment shipment, double declaredQuantity,
            double receivedQuantity, double discrepancy, double discrepancyPercent,
            String reason) {
        try {
            UUID orgId = shipment.getOrganization().getOrganizationId();

            // Find VT-02 users in the shipment's organization
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