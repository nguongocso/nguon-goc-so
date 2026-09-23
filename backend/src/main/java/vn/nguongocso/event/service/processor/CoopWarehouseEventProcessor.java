package vn.nguongocso.event.service.processor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.service.MilestoneValidationService;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.dto.request.CorrectPackagingEventRequest;
import vn.nguongocso.event.dto.request.CorrectPreprocessingEventRequest;
import vn.nguongocso.event.dto.request.RecordMobileEventRequest;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest;
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;
import vn.nguongocso.event.dto.request.RecordWarehouseEntryRequest;
import vn.nguongocso.event.dto.request.RecordWarehouseExitRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.CoopWarehouseEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Processor xử lý các sự kiện chuỗi cung ứng tại hợp tác xã:
 * Sơ chế, đóng gói, vận chuyển, nhập kho và xuất kho HTX.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoopWarehouseEventProcessor {

    private final ProductionLotRepository productionLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final ChainEventHashRecorder chainEventHashRecorder;
    private final UserRepository userRepository;
    private final EventValidationService eventValidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final MilestoneValidationService milestoneValidationService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final CoopProcessingPackagingProcessor coopProcessingPackagingProcessor;
    private final CoopTransportEventProcessor coopTransportEventProcessor;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public ChainEventResponse recordPreprocessingEvent(
            RecordPreprocessingEventRequest request, CustomUserDetails currentUser) {
        return coopProcessingPackagingProcessor.recordPreprocessingEvent(request, currentUser);
    }

    public ChainEventResponse correctPreprocessingEvent(
            UUID originalEventId, CorrectPreprocessingEventRequest request, CustomUserDetails currentUser) {
        return coopProcessingPackagingProcessor.correctPreprocessingEvent(originalEventId, request, currentUser);
    }

    public ChainEventResponse recordPackagingEvent(
            RecordPackagingEventRequest request, CustomUserDetails currentUser) {
        return coopProcessingPackagingProcessor.recordPackagingEvent(request, currentUser);
    }

    public ChainEventResponse recordMobilePackagingEvent(
            RecordMobileEventRequest request, CustomUserDetails currentUser) {
        return coopProcessingPackagingProcessor.recordMobilePackagingEvent(request, currentUser);
    }

    public ChainEventResponse correctPackagingEvent(
            UUID originalEventId, CorrectPackagingEventRequest request, CustomUserDetails currentUser) {
        return coopProcessingPackagingProcessor.correctPackagingEvent(originalEventId, request, currentUser);
    }

    public ChainEventResponse recordTransportEvent(
            RecordTransportEventRequest request, CustomUserDetails currentUser) {
        return coopTransportEventProcessor.recordTransportEvent(request, currentUser);
    }

    public CoopWarehouseEventResponse recordWarehouseEntryEvent(
            RecordWarehouseEntryRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng."));

        ProductionLot lot = shipment.getProductionLot();
        validateWarehouseEntry(shipment, currentUser);

        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());
        Map<String, Object> eventDataMap = CoopWarehouseEventDataBuilder.buildEntryData(shipment, lot, request);
        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_ENTRY)
                .eventData(CoopWarehouseEventDataCodec.toJson(objectMapper, eventDataMap))
                .location(locationPoint)
                .recordedAt(request.getEntryTime())
                .recordedBy(actor)
                .recordedOrganizationId(currentUser.getOrganizationId())
                .isCorrection(false)
                .build();

        chainEvent = chainEventHashRecorder.saveWithChainHash(chainEvent);

        publishActivityLog(
                currentUser,
                "Ghi sự kiện nhập kho HTX cho lô hàng " + shipment.getName(),
                chainEvent.getId().toString());

        return CoopWarehouseEventResponse.builder()
                .id(chainEvent.getId())
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .productionLotId(lot != null ? lot.getId() : null)
                .productionLotName(lot != null ? lot.getName() : null)
                .eventType(ChainEventType.WAREHOUSE_ENTRY)
                .warehouseName(request.getWarehouseName())
                .entryTime(request.getEntryTime())
                .storageCondition(request.getStorageCondition())
                .notes(request.getNotes())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .images(request.getImages())
                .recordedAt(chainEvent.getRecordedAt())
                .recordedByName(actor.getFullName())
                .createdAt(chainEvent.getCreatedAt())
                .build();
    }

    private void validateWarehouseEntry(Shipment shipment, CustomUserDetails currentUser) {
        try {
            validateOrganization(shipment, currentUser);
            if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
                throw new BusinessException("Lô hàng đang hoặc đã bị thu hồi, không thể ghi sự kiện.");
            }
            List<ChainEvent> shipmentEvents = chainEventRepository
                    .findByShipmentIdOrderByRecordedAtAsc(shipment.getId());
            List<ChainEvent> entryEvents = shipmentEvents.stream()
                    .filter(e -> e.getEventType() == ChainEventType.WAREHOUSE_ENTRY && !e.isCorrection())
                    .sorted((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()))
                    .toList();
            List<ChainEvent> exitEvents = shipmentEvents.stream()
                    .filter(e -> e.getEventType() == ChainEventType.WAREHOUSE_EXIT && !e.isCorrection())
                    .sorted((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()))
                    .toList();

            if (!entryEvents.isEmpty()) {
                LocalDateTime latestEntryRecorded = entryEvents.get(0).getRecordedAt();
                boolean isExited = !exitEvents.isEmpty()
                        && !exitEvents.get(0).getRecordedAt().isBefore(latestEntryRecorded);
                if (!isExited) {
                    throw new BusinessException(
                            "Lô hàng [" + shipment.getName()
                                    + "] hiện đang trong kho HTX, vui lòng ghi xuất kho trước khi nhập kho mới.");
                }
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(
                    shipment.getId(), shipment.getName(), ChainEventType.WAREHOUSE_ENTRY, e.getMessage(), currentUser);
            throw e;
        }
    }

    public CoopWarehouseEventResponse recordWarehouseExitEvent(
            RecordWarehouseExitRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng."));

        ProductionLot lot = shipment.getProductionLot();
        ExitValidationResult exitVal = validateWarehouseExit(shipment, request, currentUser);

        CoopWarehouseEventDataBuilder.DurationResult durationResult =
                CoopWarehouseEventDataBuilder.calculateStorageDuration(
                        exitVal.entryTime(), request.getExitTime(), lot);

        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());
        Map<String, Object> eventDataMap = CoopWarehouseEventDataBuilder.buildExitData(
                shipment,
                lot,
                exitVal.warehouseName(),
                exitVal.entryTime(),
                request,
                durationResult);
        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_EXIT)
                .eventData(CoopWarehouseEventDataCodec.toJson(objectMapper, eventDataMap))
                .location(locationPoint)
                .recordedAt(request.getExitTime())
                .recordedBy(actor)
                .recordedOrganizationId(currentUser.getOrganizationId())
                .isCorrection(false)
                .build();

        chainEvent = chainEventHashRecorder.saveWithChainHash(chainEvent);

        publishActivityLog(
                currentUser,
                "Ghi sự kiện xuất kho HTX cho lô hàng " + shipment.getName(),
                chainEvent.getId().toString());

        return CoopWarehouseEventResponse.builder()
                .id(chainEvent.getId())
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .productionLotId(lot != null ? lot.getId() : null)
                .productionLotName(lot != null ? lot.getName() : null)
                .eventType(ChainEventType.WAREHOUSE_EXIT)
                .warehouseName(exitVal.warehouseName())
                .entryTime(exitVal.entryTime())
                .exitTime(request.getExitTime())
                .storageCondition(exitVal.storageCondition())
                .destination(request.getDestination())
                .notes(request.getNotes())
                .storageDurationDays(durationResult.storageDays())
                .storageDurationHours(durationResult.storageHours())
                .maxAllowedStorageDays(durationResult.maxDays())
                .isStorageExceeded(durationResult.isExceeded())
                .warningMessage(durationResult.warning())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .images(request.getImages())
                .recordedAt(chainEvent.getRecordedAt())
                .recordedByName(actor.getFullName())
                .createdAt(chainEvent.getCreatedAt())
                .build();
    }

    private ExitValidationResult validateWarehouseExit(
            Shipment shipment, RecordWarehouseExitRequest request, CustomUserDetails currentUser) {
        try {
            validateOrganization(shipment, currentUser);
            if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
                throw new BusinessException("Lô hàng đang hoặc đã bị thu hồi, không thể ghi sự kiện.");
            }

            List<ChainEvent> shipmentEvents = chainEventRepository
                    .findByShipmentIdOrderByRecordedAtAsc(shipment.getId());
            List<ChainEvent> entryEvents = shipmentEvents.stream()
                    .filter(e -> e.getEventType() == ChainEventType.WAREHOUSE_ENTRY && !e.isCorrection())
                    .sorted((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()))
                    .toList();
            List<ChainEvent> exitEvents = shipmentEvents.stream()
                    .filter(e -> e.getEventType() == ChainEventType.WAREHOUSE_EXIT && !e.isCorrection())
                    .sorted((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()))
                    .toList();

            if (entryEvents.isEmpty()) {
                throw new BusinessException(
                        "Lô hàng [" + shipment.getName()
                                + "] chưa được ghi nhận nhập kho HTX. "
                                + "Vui lòng ghi sự kiện nhập kho trước khi xuất kho.");
            }

            ChainEvent latestEntry = entryEvents.get(0);
            if (!exitEvents.isEmpty() && !exitEvents.get(0).getRecordedAt().isBefore(latestEntry.getRecordedAt())) {
                throw new BusinessException(
                        "Lô hàng [" + shipment.getName()
                                + "] đã được ghi xuất kho rồi. "
                                + "Vui lòng ghi nhập kho mới trước khi xuất kho lại.");
            }

            LocalDateTime entryTime = CoopWarehouseEventDataCodec.extractEntryTime(objectMapper, latestEntry);
            LocalDateTime exitTime = request.getExitTime();
            LocalDateTime now = clock != null ? LocalDateTime.now(clock) : LocalDateTime.now();
            if (exitTime.isAfter(now)) {
                throw new BusinessException("Thời điểm xuất kho không được ở tương lai.");
            }
            if (exitTime.isBefore(entryTime)) {
                throw new BusinessException("Thời điểm xuất kho không được trước thời điểm nhập kho.");
            }

            String warehouseName =
                    CoopWarehouseEventDataCodec.extractStringField(objectMapper, latestEntry, "warehouseName");
            String storageCondition =
                    CoopWarehouseEventDataCodec.extractStringField(objectMapper, latestEntry, "storageCondition");
            return new ExitValidationResult(entryTime, warehouseName, storageCondition);

        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(
                    shipment.getId(), shipment.getName(), ChainEventType.WAREHOUSE_EXIT, e.getMessage(), currentUser);
            throw e;
        }
    }

    private void validateEventPermission(CustomUserDetails currentUser) {
        String role = currentUser.getRoleCode();
        if (!"VT-01".equals(role) && !"VT-02".equals(role) && !"VT-03".equals(role)) {
            throw new BusinessException("Chỉ thành viên được cấp quyền trong tổ chức mới được ghi sự kiện.");
        }
    }

    private void validateOrganization(ProductionLot lot, CustomUserDetails currentUser) {
        if ("VT-01".equals(currentUser.getRoleCode())) return;
        if (!lot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc tổ chức quản lý của lô sản xuất này.");
        }
    }

    private void validateOrganization(Shipment shipment, CustomUserDetails currentUser) {
        if ("VT-01".equals(currentUser.getRoleCode())) return;
        if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi sự kiện cho lô hàng của tổ chức này.");
        }
    }

    private double calculateLossRate(double input, double output) {
        if (input <= 0) return 0.0;
        double lossRate = (input - output) / input * 100.0;
        return Math.round(lossRate * 100.0) / 100.0;
    }

    private ProductionLot resolveLotFromEventData(String eventDataJson) {
        Map<String, Object> data = parseEventData(eventDataJson);
        String lotIdStr = (String) data.get("productionLotId");
        if (lotIdStr == null) {
            throw new BusinessException("Không tìm thấy thông tin lô sản xuất trong sự kiện gốc.");
        }
        return productionLotRepository.findById(UUID.fromString(lotIdStr))
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));
    }

    private ChainEvent saveLotEvent(ChainEventType eventType, String eventDataJson,
            Double latitude, Double longitude, User actor, boolean isCorrection, ChainEvent parentEvent) {
        ChainEvent chainEvent = ChainEvent.builder()
                .eventType(eventType)
                .eventData(eventDataJson)
                .location(buildPoint(latitude, longitude))
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .parentEvent(parentEvent)
                .isCorrection(isCorrection)
                .build();
        return chainEventRepository.save(chainEvent);
    }

    private Point buildPoint(Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            return geometryFactory.createPoint(new Coordinate(longitude, latitude));
        }
        return null;
    }

    private Map<String, Object> parseEventData(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Không thể parse eventData: {}", json);
            return new HashMap<>();
        }
    }

    private User getActor(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người ghi nhận."));
    }

    private void publishActivityLog(CustomUserDetails currentUser, String desc, String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("CREATE")
                .description(desc)
                .entityType("ChainEvent")
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private ChainEventResponse buildResponse(ChainEvent event, Map<String, Object> data,
            Double lat, Double lon, User actor) {
        return ChainEventResponse.builder()
                .id(event.getId())
                .shipmentId(event.getShipment() != null ? event.getShipment().getId() : null)
                .eventType(event.getEventType())
                .eventData(data)
                .latitude(lat)
                .longitude(lon)
                .recordedAt(event.getRecordedAt())
                .recordedByName(actor != null ? actor.getFullName() : null)
                .createdAt(event.getCreatedAt())
                .build();
    }

    private record ExitValidationResult(LocalDateTime entryTime, String warehouseName, String storageCondition) {}
}
