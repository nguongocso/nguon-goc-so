package vn.nguongocso.event.service.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.dto.request.WarehouseReceiptRequest;
import vn.nguongocso.event.dto.response.WarehouseReceiptResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.WarehouseReceiptService;
import vn.nguongocso.event.service.processor.WarehouseReceiptProcessor;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/** Implementation của dịch vụ nhập kho và đối chiếu số lượng. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseReceiptServiceImpl implements WarehouseReceiptService {
    private final WarehouseReceiptProcessor warehouseReceiptProcessor;
    private final ChainEventRepository chainEventRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public WarehouseReceiptResponse recordWarehouseReceipt(
            WarehouseReceiptRequest request,
            CustomUserDetails currentUser) {
        return warehouseReceiptProcessor.processWarehouseReceipt(request, currentUser);
    }

    @Override
    public PageResponse<WarehouseReceiptResponse> getWarehouseReceipts(
            CustomUserDetails currentUser,
            Pageable pageable) {
        if (!RoleCode.PROCUREMENT.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Chỉ Doanh nghiệp thu mua mới được xem danh sách nhập kho.");
        }

        Page<ChainEvent> page = chainEventRepository
                .findByEventTypeAndRecordedBy_UserIdOrderByRecordedAtDesc(
                        ChainEventType.WAREHOUSE_RECEIPT, currentUser.getUserId(), pageable);

        List<WarehouseReceiptResponse> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.from(page, items);
    }

    @Override
    public WarehouseReceiptResponse getWarehouseReceiptDetail(UUID eventId, CustomUserDetails currentUser) {
        if (!RoleCode.PROCUREMENT.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Chỉ Doanh nghiệp thu mua mới được xem chi tiết nhập kho.");
        }

        ChainEvent event = chainEventRepository.findByIdAndEventType(eventId, ChainEventType.WAREHOUSE_RECEIPT)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy sự kiện nhập kho."));

        validateViewPermission(event, currentUser);

        return toResponse(event);
    }

    private void validateViewPermission(ChainEvent event, CustomUserDetails currentUser) {
        if (!event.getRecordedBy().getUserId().equals(currentUser.getUserId())) {
            if (event.getRecordedBy() != null) {
                var orgUserOpt = organizationUserRepository
                        .findByOrganization_OrganizationIdAndUser_UserId(
                                currentUser.getOrganizationId(), event.getRecordedBy().getUserId());
                if (orgUserOpt.isEmpty()) {
                    throw new BusinessException(HttpStatus.FORBIDDEN,
                            "Bạn không có quyền xem sự kiện nhập kho này.");
                }
            }
        }
    }

    private WarehouseReceiptResponse toResponse(ChainEvent event) {
        Map<String, Object> data = parseEventData(event.getEventData());

        String shipmentIdStr = (String) data.get("shipmentId");
        UUID shipmentId = shipmentIdStr != null ? UUID.fromString(shipmentIdStr) : null;
        String shipmentName = (String) data.get("shipmentName");
        Double declaredQuantity = getDoubleValue(data, "declaredQuantity");
        Double receivedQuantity = getDoubleValue(data, "receivedQuantity");
        Double discrepancy = getDoubleValue(data, "discrepancy");
        Double discrepancyPercent = getDoubleValue(data, "discrepancyPercent");
        Boolean isDiscrepancyExceeded = (Boolean) data.get("isDiscrepancyExceeded");
        String reason = (String) data.get("reason");
        String conditionNote = (String) data.get("conditionNote");
        String receiptDateStr = (String) data.get("receiptDate");
        LocalDate receiptDate = receiptDateStr != null ? LocalDate.parse(receiptDateStr) : null;

        String traceCode = resolveTraceCode(shipmentId);

        boolean reasonRequired = isDiscrepancyExceeded != null && isDiscrepancyExceeded;

        return WarehouseReceiptResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .shipmentId(shipmentId)
                .shipmentName(shipmentName)
                .traceCode(traceCode)
                .declaredQuantity(declaredQuantity)
                .receivedQuantity(receivedQuantity)
                .discrepancy(discrepancy)
                .discrepancyPercent(discrepancyPercent)
                .isDiscrepancyExceeded(isDiscrepancyExceeded)
                .reasonRequired(reasonRequired)
                .reason(reason)
                .conditionNote(conditionNote)
                .receiptDate(receiptDate)
                .recordedAt(event.getRecordedAt())
                .recordedBy(event.getRecordedBy() != null ? event.getRecordedBy().getFullName() : null)
                .build();
    }

    private String resolveTraceCode(UUID shipmentId) {
        if (shipmentId != null) {
            List<TraceCode> codes = traceCodeRepository.findByShipmentId(shipmentId);
            if (!codes.isEmpty()) {
                return codes.get(0).getCodeValue();
            }
        }
        return null;
    }

    private Map<String, Object> parseEventData(String eventDataJson) {
        if (eventDataJson == null || eventDataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(
                    eventDataJson,
                    new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Không thể parse eventData: {}", eventDataJson);
            return Collections.emptyMap();
        }
    }

    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Double d) return d;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
