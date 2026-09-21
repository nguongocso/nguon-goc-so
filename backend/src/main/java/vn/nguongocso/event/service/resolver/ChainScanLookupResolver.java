package vn.nguongocso.event.service.resolver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.ScanLookupResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Resolver chuyên trách xử lý tra cứu mã quét (scanLookup) và dòng thời gian (timeline) cho chuỗi sự kiện.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainScanLookupResolver {

    private final TraceCodeRepository traceCodeRepository;
    private final ChainEventRepository chainEventRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentHandoverRepository shipmentHandoverRepository;
    private final ObjectMapper objectMapper;

    /**
     * Tra cứu thông tin lô hàng và các loại sự kiện được phép ghi dựa trên mã quét.
     */
    public ScanLookupResponse scanLookup(String codeValue, CustomUserDetails currentUser) {
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new BusinessException("Mã truy xuất không tồn tại."));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new BusinessException("Mã truy xuất chưa được gắn với lô hàng.");
        }

        validateScanLookupShipment(shipment, currentUser);

        Optional<ChainEvent> latestEvent = chainEventRepository
                .findTopByShipmentIdOrderByRecordedAtDesc(shipment.getId());
        List<String> allowedEventTypes = determineAllowedEventTypes(latestEvent);
        Boolean storageEligible = determineStorageEligibility(shipment, currentUser);

        return buildScanLookupResponse(traceCode, shipment, latestEvent, allowedEventTypes, storageEligible);
    }

    private void validateScanLookupShipment(Shipment shipment, CustomUserDetails currentUser) {
        if (!"VT-04".equals(currentUser.getRoleCode())) {
            validateOrganization(shipment, currentUser);
        }
        if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
            throw new BusinessException(HttpStatus.CONFLICT, "Lô hàng đang hoặc đã bị thu hồi.");
        }
        if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
            throw new BusinessException("Lô hàng chưa được kích hoạt.");
        }
    }

    private Boolean determineStorageEligibility(Shipment shipment, CustomUserDetails currentUser) {
        String role = currentUser.getRoleCode();
        if (!"VT-03".equals(role) && !"VT-04".equals(role)) {
            return null;
        }
        if ("VT-04".equals(role)) {
            return isProcurementRelated(shipment, currentUser);
        }
        return chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .anyMatch(e -> e.getEventType() == ChainEventType.TRANSPORT);
    }

    private ScanLookupResponse buildScanLookupResponse(
            TraceCode traceCode, Shipment shipment, Optional<ChainEvent> latestEvent,
            List<String> allowedEventTypes, Boolean storageEligible) {
        ProductionLot productionLot = shipment.getProductionLot();
        return ScanLookupResponse.builder()
                .valid(true)
                .message(null)
                .traceCode(traceCode.getCodeValue())
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .shipmentStatus(shipment.getStatus().name())
                .productionLotId(productionLot != null ? productionLot.getId() : null)
                .productCategoryName(
                        productionLot != null && productionLot.getProductCategory() != null
                                ? productionLot.getProductCategory().getName()
                                : null)
                .farmAreaName(
                        productionLot != null && productionLot.getFarmArea() != null
                                ? productionLot.getFarmArea().getName()
                                : null)
                .organizationId(shipment.getOrganization().getOrganizationId())
                .organizationName(shipment.getOrganization().getName())
                .allowedEventTypes(allowedEventTypes)
                .lastEventType(latestEvent.map(e -> e.getEventType().name()).orElse(null))
                .lastEventRecordedAt(latestEvent.map(ChainEvent::getRecordedAt).orElse(null))
                .storageEligible(storageEligible)
                .build();
    }

    /**
     * Lấy dòng thời gian của một lô hàng.
     */
    public List<ChainEventResponse> getShipmentTimeline(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("Lô hàng không tồn tại."));
        validateShipmentTimelineScope(shipment);

        List<ChainEvent> shipmentEvents = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId);

        List<ChainEvent> productionLotEvents = resolveProductionLotEvents(shipment);

        List<ChainEventResponse> timeline = new ArrayList<>();
        productionLotEvents.forEach(event -> timeline.add(toChainEventResponse(event, "PRODUCTION_LOT", null, true)));

        if (shipment.getParentShipment() != null) {
            Shipment parent = shipment.getParentShipment();
            LocalDateTime splitAt = shipment.getSplitAt();
            chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(parent.getId()).stream()
                    .filter(event -> splitAt == null || event.getRecordedAt() == null || !event.getRecordedAt().isAfter(splitAt))
                    .forEach(event -> timeline.add(toChainEventResponse(event, "SOURCE_SHIPMENT", parent.getId(), true)));
        }

        shipmentEvents.forEach(event -> timeline.add(toChainEventResponse(
                event, shipment.getParentShipment() == null ? "SOURCE_SHIPMENT" : "CHILD_SHIPMENT",
                shipment.getId(), false)));

        timeline.sort(Comparator.comparing(ChainEventResponse::getRecordedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return timeline;
    }

    private List<ChainEvent> resolveProductionLotEvents(Shipment shipment) {
        if (shipment.getProductionLot() == null) {
            return Collections.emptyList();
        }
        UUID productionLotId = shipment.getProductionLot().getId();
        List<ChainEvent> allUnassignedEvents = chainEventRepository.findByShipmentIsNullAndEventTypeIn(
                List.of(ChainEventType.HARVEST, ChainEventType.PREPROCESSING, ChainEventType.PACKAGING));
        return allUnassignedEvents.stream()
                .filter(e -> {
                    Map<String, Object> data = parseEventData(e.getEventData());
                    Object lotId = data.get("productionLotId");
                    return lotId != null && lotId.toString().equals(productionLotId.toString());
                })
                .collect(Collectors.toList());
    }

    private void validateShipmentTimelineScope(Shipment shipment) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentUser)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập.",
                    Map.of("code", "AUTHENTICATION_REQUIRED"));
        }

        UUID organizationId = currentUser.getOrganizationId();
        if ("VT-04".equals(currentUser.getRoleCode())) {
            validateVt04TimelineScope(shipment, organizationId);
            return;
        }

        validateStandardTimelineScope(shipment, organizationId);
    }

    private void validateVt04TimelineScope(Shipment shipment, UUID organizationId) {
        if (!isVt04Authorized(shipment, organizationId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Lô hàng không được giao cho tổ chức của bạn.",
                    Map.of("code", "RECIPIENT_MISMATCH"));
        }
    }

    private boolean isVt04Authorized(Shipment shipment, UUID organizationId) {
        if (organizationId == null) {
            return false;
        }
        if (shipment.getRecipientOrganization() != null
                && organizationId.equals(shipment.getRecipientOrganization().getOrganizationId())) {
            return true;
        }
        if (shipmentHandoverRepository.existsByShipmentIdAndToOrganizationOrganizationId(
                shipment.getId(), organizationId)) {
            return true;
        }
        if (chainEventRepository.existsByShipmentIdAndRecordedOrganizationId(
                shipment.getId(), organizationId)) {
            return true;
        }
        return shipment.getOrganization() != null
                && organizationId.equals(shipment.getOrganization().getOrganizationId());
    }

    private void validateStandardTimelineScope(Shipment shipment, UUID organizationId) {
        if (organizationId == null || shipment.getOrganization() == null
                || !organizationId.equals(shipment.getOrganization().getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem dòng sự kiện của lô hàng thuộc tổ chức khác.",
                    Map.of("code", "CROSS_ORGANIZATION_ACCESS"));
        }
    }

    public ChainEventResponse toChainEventResponse(ChainEvent event, String lineageLevel,
            UUID sourceShipmentId, boolean inherited) {
        Map<String, Object> eventDataMap = parseEventData(event.getEventData());

        Double latitude = null;
        Double longitude = null;
        if (event.getLocation() != null) {
            latitude = event.getLocation().getY();
            longitude = event.getLocation().getX();
        }

        String recordedByName = event.getRecordedBy() != null
                ? event.getRecordedBy().getFullName()
                : null;

        return ChainEventResponse.builder()
                .id(event.getId())
                .shipmentId(event.getShipment() != null ? event.getShipment().getId() : null)
                .eventType(event.getEventType())
                .eventData(eventDataMap)
                .latitude(latitude)
                .longitude(longitude)
                .recordedAt(event.getRecordedAt())
                .recordedByName(recordedByName)
                .createdAt(event.getCreatedAt())
                .lineageLevel(lineageLevel)
                .sourceShipmentId(sourceShipmentId)
                .inherited(inherited)
                .build();
    }

    public Map<String, Object> parseEventData(String eventDataJson) {
        if (eventDataJson == null || eventDataJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(eventDataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Không thể parse eventData: {}", eventDataJson);
            return new HashMap<>();
        }
    }

    private void validateOrganization(Shipment shipment, CustomUserDetails currentUser) {
        if ("VT-01".equals(currentUser.getRoleCode())) {
            return;
        }
        if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi sự kiện cho lô hàng của tổ chức này.");
        }
    }

    private List<String> determineAllowedEventTypes(Optional<ChainEvent> latestEvent) {
        if (latestEvent.isEmpty()) {
            return List.of(
                    ChainEventType.HARVEST.name(),
                    ChainEventType.PREPROCESSING.name(),
                    ChainEventType.PACKAGING.name());
        }
        ChainEventType lastType = latestEvent.get().getEventType();
        return switch (lastType) {
            case HARVEST -> List.of(ChainEventType.PREPROCESSING.name(), ChainEventType.PACKAGING.name());
            case PREPROCESSING -> List.of(ChainEventType.PACKAGING.name());
            case PACKAGING -> List.of(ChainEventType.TRANSPORT.name());
            case TRANSPORT -> List.of(ChainEventType.WAREHOUSE_RECEIPT.name());
            case WAREHOUSE_RECEIPT -> Collections.emptyList();
            case STORAGE_CONDITION -> Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    private boolean isProcurementRelated(Shipment shipment, CustomUserDetails currentUser) {
        List<ChainEvent> procurementEvents = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .filter(e -> e.getEventType() == ChainEventType.PROCUREMENT)
                .toList();

        if (procurementEvents.isEmpty()) {
            return false;
        }

        UUID currentOrgId = currentUser.getOrganizationId();
        List<UUID> recorderIds = procurementEvents.stream()
                .map(e -> e.getRecordedBy().getUserId())
                .distinct()
                .toList();

        return recorderIds.stream()
                .anyMatch(recorderId -> organizationUserRepository
                        .findByOrganization_OrganizationIdAndUser_UserId(currentOrgId, recorderId)
                        .isPresent());
    }
}
