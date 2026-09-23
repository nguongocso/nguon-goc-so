package vn.nguongocso.event.service.processor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.dto.request.WarehouseReceiptRequest;
import vn.nguongocso.event.dto.response.WarehouseReceiptResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.notifier.WarehouseDiscrepancyNotifier;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/** Component xử lý nghiệp vụ ghi nhận nhập kho (Warehouse Receipt) và kiểm tra đối chiếu chênh lệch. */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseReceiptProcessor {

    private static final double DISCREPANCY_THRESHOLD_PERCENT = 2.0;

    private final TraceCodeRepository traceCodeRepository;
    private final ShipmentHandoverRepository shipmentHandoverRepository;
    private final ChainEventRepository chainEventRepository;
    private final ChainEventHashRecorder chainEventHashRecorder;
    private final UserRepository userRepository;
    private final EventValidationService eventValidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final WarehouseDiscrepancyNotifier discrepancyNotifier;
    private final OrganizationUserRepository organizationUserRepository;
    private final ObjectMapper objectMapper;

    /**
     * Xử lý ghi nhận sự kiện nhập kho cho lô hàng.
     *
     * @param request dữ liệu yêu cầu nhập kho
     * @param currentUser thông tin người dùng thực hiện
     * @return kết quả ghi nhận nhập kho
     */
    public WarehouseReceiptResponse processWarehouseReceipt(
            WarehouseReceiptRequest request, CustomUserDetails currentUser) {

        validateRole(currentUser);

        Shipment shipment = resolveAndValidateShipment(request.getCodeValue(), currentUser.getOrganizationId());

        validateShipmentStatus(shipment, currentUser);

        validateProcurementRelationship(shipment, currentUser);

        double declaredQuantity = (double) shipment.getTotalQuantity();
        DiscrepancyResult discrepancyResult = calculateDiscrepancy(
                declaredQuantity, request.getReceivedQuantity(), request.getReason());

        LocalDate receiptDate = request.getReceiptDate() != null ? request.getReceiptDate() : LocalDate.now();

        String eventDataJson = buildEventDataJson(
                shipment, declaredQuantity, request.getReceivedQuantity(),
                discrepancyResult, request.getConditionNote(), receiptDate, request.getReason());

        User actor = findActor(currentUser.getUserId());

        ChainEvent chainEvent = saveChainEvent(shipment, eventDataJson, actor);

        publishActivityLog(shipment, chainEvent, currentUser);

        boolean notificationSent = false;
        if (discrepancyResult.isExceeded()) {
            notificationSent = discrepancyNotifier.sendDiscrepancyNotification(
                    shipment, declaredQuantity, request.getReceivedQuantity(),
                    discrepancyResult.discrepancy(), discrepancyResult.discrepancyPercent(),
                    request.getReason());
        }

        return buildResponse(chainEvent, shipment, declaredQuantity, request.getReceivedQuantity(),
                discrepancyResult, request.getConditionNote(), receiptDate,
                actor.getFullName(), notificationSent, request.getReason());
    }

    private void validateRole(CustomUserDetails currentUser) {
        if (!RoleCode.PROCUREMENT.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Chỉ Doanh nghiệp thu mua mới được ghi sự kiện nhập kho.");
        }
    }

    private Shipment resolveAndValidateShipment(String codeValue, UUID userOrgId) {
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Mã lô hàng không tồn tại."));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new BusinessException("Mã truy xuất chưa được gắn với lô hàng.");
        }

        boolean isRecipient = shipment.getRecipientOrganization() != null
                && userOrgId != null
                && userOrgId.equals(shipment.getRecipientOrganization().getOrganizationId());

        boolean hasAcceptedHandover = userOrgId != null
                && shipmentHandoverRepository.existsByShipmentIdAndToOrganizationOrganizationIdAndStatus(
                        shipment.getId(), userOrgId, ShipmentHandoverStatus.ACCEPTED);

        boolean hasRecordedEvent = userOrgId != null
                && chainEventRepository.existsByShipmentIdAndRecordedOrganizationIdAndEventType(
                        shipment.getId(), userOrgId, ChainEventType.PROCUREMENT);

        if (!isRecipient && !hasAcceptedHandover && !hasRecordedEvent) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Lô hàng không được giao cho tổ chức của bạn.", Map.of("code", "RECIPIENT_MISMATCH"));
        }

        return shipment;
    }

    private void validateShipmentStatus(Shipment shipment, CustomUserDetails currentUser) {
        try {
            if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
                throw new BusinessException(
                        "Lô hàng chưa được kích hoạt hoặc đang/đã bị thu hồi, không thể ghi nhận nhập kho.");
            }
            if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
                throw new BusinessException(
                        "Lô hàng chưa được kích hoạt hoặc đã bị thu hồi, không thể ghi nhận nhập kho.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(shipment.getId(), shipment.getName(),
                    ChainEventType.WAREHOUSE_RECEIPT, e.getMessage(), currentUser);
            throw e;
        }
    }

    private void validateProcurementRelationship(Shipment shipment, CustomUserDetails currentUser) {
        List<ChainEvent> procurementEvents = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .filter(e -> e.getEventType() == ChainEventType.PROCUREMENT)
                .toList();

        if (procurementEvents.isEmpty()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi nhận nhập kho cho lô hàng này. "
                            + "Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.");
        }

        UUID currentOrgId = currentUser.getOrganizationId();
        List<UUID> recorderIds = procurementEvents.stream()
                .map(e -> e.getRecordedBy().getUserId())
                .distinct()
                .toList();

        boolean hasRelationship = recorderIds.stream()
                .anyMatch(recorderId -> organizationUserRepository
                        .findByOrganization_OrganizationIdAndUser_UserId(currentOrgId, recorderId)
                        .isPresent());

        if (!hasRelationship) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi nhận nhập kho cho lô hàng này. "
                            + "Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.");
        }
    }

    private DiscrepancyResult calculateDiscrepancy(double declaredQuantity, Double receivedQuantity, String reason) {
        if (receivedQuantity == null || receivedQuantity <= 0) {
            throw new BusinessException("Số lượng thực nhận phải lớn hơn 0.");
        }

        double discrepancy = receivedQuantity - declaredQuantity;
        double discrepancyPercent;
        if (declaredQuantity == 0) {
            discrepancyPercent = receivedQuantity > 0 ? 100.0 : 0.0;
        } else {
            discrepancyPercent = (discrepancy / declaredQuantity) * 100.0;
        }

        double absoluteDiscrepancyPercent = Math.abs(discrepancyPercent);
        boolean isExceeded = absoluteDiscrepancyPercent > DISCREPANCY_THRESHOLD_PERCENT;

        if (isExceeded && (reason == null || reason.isBlank())) {
            Map<String, Object> errorDetails = new LinkedHashMap<>();
            errorDetails.put("declaredQuantity", declaredQuantity);
            errorDetails.put("receivedQuantity", receivedQuantity);
            errorDetails.put("discrepancyPercent", Math.round(discrepancyPercent * 100.0) / 100.0);
            errorDetails.put("threshold", DISCREPANCY_THRESHOLD_PERCENT);
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Chênh lệch số lượng vượt ngưỡng cho phép (2%). Vui lòng cung cấp lý do chênh lệch.",
                    errorDetails);
        }

        return new DiscrepancyResult(discrepancy, discrepancyPercent, isExceeded);
    }

    private String buildEventDataJson(
            Shipment shipment, double declaredQuantity, double receivedQuantity,
            DiscrepancyResult disc, String conditionNote, LocalDate receiptDate, String reason) {

        Map<String, Object> eventDataMap = new LinkedHashMap<>();
        eventDataMap.put("shipmentId", shipment.getId().toString());
        eventDataMap.put("shipmentName", shipment.getName());
        eventDataMap.put("declaredQuantity", declaredQuantity);
        eventDataMap.put("receivedQuantity", receivedQuantity);
        eventDataMap.put("discrepancy", Math.round(disc.discrepancy() * 100.0) / 100.0);
        eventDataMap.put("discrepancyPercent", Math.round(disc.discrepancyPercent() * 100.0) / 100.0);
        eventDataMap.put("threshold", DISCREPANCY_THRESHOLD_PERCENT);
        eventDataMap.put("isDiscrepancyExceeded", disc.isExceeded());
        eventDataMap.put("conditionNote", conditionNote);
        eventDataMap.put("receiptDate", receiptDate.toString());
        if (reason != null && !reason.isBlank()) {
            eventDataMap.put("reason", reason);
        }

        try {
            return objectMapper.writeValueAsString(eventDataMap);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Lỗi chuyển đổi dữ liệu sự kiện sang chuỗi JSON.");
        }
    }

    private User findActor(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người ghi nhận."));
    }

    private ChainEvent saveChainEvent(Shipment shipment, String eventDataJson, User actor) {
        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_RECEIPT)
                .eventData(eventDataJson)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        return chainEventHashRecorder.saveWithChainHash(chainEvent);
    }

    private void publishActivityLog(Shipment shipment, ChainEvent chainEvent, CustomUserDetails currentUser) {
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
    }

    private WarehouseReceiptResponse buildResponse(
            ChainEvent chainEvent, Shipment shipment, double declaredQuantity, double receivedQuantity,
            DiscrepancyResult disc, String conditionNote, LocalDate receiptDate,
            String actorName, boolean notificationSent, String reason) {

        WarehouseReceiptResponse.WarehouseReceiptResponseBuilder builder = WarehouseReceiptResponse.builder()
                .id(chainEvent.getId())
                .eventType(ChainEventType.WAREHOUSE_RECEIPT)
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .declaredQuantity(declaredQuantity)
                .receivedQuantity(receivedQuantity)
                .discrepancy(Math.round(disc.discrepancy() * 100.0) / 100.0)
                .discrepancyPercent(Math.round(disc.discrepancyPercent() * 100.0) / 100.0)
                .isDiscrepancyExceeded(disc.isExceeded())
                .reasonRequired(disc.isExceeded())
                .conditionNote(conditionNote)
                .receiptDate(receiptDate)
                .recordedAt(chainEvent.getRecordedAt())
                .recordedBy(actorName)
                .notificationSent(notificationSent);

        if (reason != null && !reason.isBlank()) {
            builder.reason(reason);
        }

        return builder.build();
    }

    /** Record hỗ trợ đóng gói kết quả tính chênh lệch số lượng. */
    public record DiscrepancyResult(double discrepancy, double discrepancyPercent, boolean isExceeded) {}
}
