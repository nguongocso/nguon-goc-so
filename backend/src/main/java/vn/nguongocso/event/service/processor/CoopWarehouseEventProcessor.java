package vn.nguongocso.event.service.processor;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    // ==========================================
    // 1. SƠ CHẾ & PHÂN LOẠI (Ủy quyền)
    // ==========================================

    public ChainEventResponse recordPreprocessingEvent(
            RecordPreprocessingEventRequest request, CustomUserDetails currentUser) {
        return coopProcessingPackagingProcessor.recordPreprocessingEvent(request, currentUser);
    }

    public ChainEventResponse correctPreprocessingEvent(
            UUID originalEventId, CorrectPreprocessingEventRequest request, CustomUserDetails currentUser) {
        return coopProcessingPackagingProcessor.correctPreprocessingEvent(originalEventId, request, currentUser);
    }

    // ==========================================
    // 2. ĐÓNG GÓI (Ủy quyền)
    // ==========================================

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

    // ==========================================
    // 3. VẬN CHUYỂN (Ủy quyền)
    // ==========================================

    public ChainEventResponse recordTransportEvent(
            RecordTransportEventRequest request, CustomUserDetails currentUser) {
        return coopTransportEventProcessor.recordTransportEvent(request, currentUser);
    }

    // ==========================================
    // 4. NHẬP KHO & XUẤT KHO HTX
    // ==========================================

    public CoopWarehouseEventResponse recordWarehouseEntryEvent(
            RecordWarehouseEntryRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng."));

        ProductionLot lot = shipment.getProductionLot();
        validateWarehouseEntry(shipment, currentUser);

        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());
        Map<String, Object> eventDataMap = buildWarehouseEntryDataMap(shipment, lot, request);
        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_ENTRY)
                .eventData(toJson(eventDataMap))
                .location(locationPoint)
                .recordedAt(request.getEntryTime())
                .recordedBy(actor)
                .recordedOrganizationId(currentUser.getOrganizationId())
                .isCorrection(false)
                .build();

        chainEvent = chainEventHashRecorder.saveWithChainHash(chainEvent);

        publishActivityLog(currentUser, "Ghi sự kiện nhập kho HTX cho lô hàng " + shipment.getName(), chainEvent.getId().toString());

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
            List<ChainEvent> shipmentEvents = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId());
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
                boolean isExited = !exitEvents.isEmpty() && !exitEvents.get(0).getRecordedAt().isBefore(latestEntryRecorded);
                if (!isExited) {
                    throw new BusinessException("Lô hàng [" + shipment.getName() + "] hiện đang trong kho HTX, vui lòng ghi xuất kho trước khi nhập kho mới.");
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

        DurationResult durationResult = calculateStorageDurations(exitVal.entryTime(), request.getExitTime(), lot);

        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());
        Map<String, Object> eventDataMap = buildWarehouseExitDataMap(
                shipment, lot, exitVal, request, durationResult);
        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_EXIT)
                .eventData(toJson(eventDataMap))
                .location(locationPoint)
                .recordedAt(request.getExitTime())
                .recordedBy(actor)
                .recordedOrganizationId(currentUser.getOrganizationId())
                .isCorrection(false)
                .build();

        chainEvent = chainEventHashRecorder.saveWithChainHash(chainEvent);

        publishActivityLog(currentUser, "Ghi sự kiện xuất kho HTX cho lô hàng " + shipment.getName(), chainEvent.getId().toString());

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

            List<ChainEvent> shipmentEvents = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId());
            List<ChainEvent> entryEvents = shipmentEvents.stream()
                    .filter(e -> e.getEventType() == ChainEventType.WAREHOUSE_ENTRY && !e.isCorrection())
                    .sorted((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()))
                    .toList();
            List<ChainEvent> exitEvents = shipmentEvents.stream()
                    .filter(e -> e.getEventType() == ChainEventType.WAREHOUSE_EXIT && !e.isCorrection())
                    .sorted((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()))
                    .toList();

            if (entryEvents.isEmpty()) {
                throw new BusinessException("Lô hàng [" + shipment.getName() + "] chưa được ghi nhận nhập kho HTX. Vui lòng ghi sự kiện nhập kho trước khi xuất kho.");
            }

            ChainEvent latestEntry = entryEvents.get(0);
            if (!exitEvents.isEmpty() && !exitEvents.get(0).getRecordedAt().isBefore(latestEntry.getRecordedAt())) {
                throw new BusinessException("Lô hàng [" + shipment.getName() + "] đã được ghi xuất kho rồi. Vui lòng ghi nhập kho mới trước khi xuất kho lại.");
            }

            LocalDateTime entryTime = extractEntryTime(latestEntry);
            LocalDateTime exitTime = request.getExitTime();
            LocalDateTime now = clock != null ? LocalDateTime.now(clock) : LocalDateTime.now();
            if (exitTime.isAfter(now)) {
                throw new BusinessException("Thời điểm xuất kho không được ở tương lai.");
            }
            if (exitTime.isBefore(entryTime)) {
                throw new BusinessException("Thời điểm xuất kho không được trước thời điểm nhập kho.");
            }

            String warehouseName = extractStringField(latestEntry, "warehouseName");
            String storageCondition = extractStringField(latestEntry, "storageCondition");
            return new ExitValidationResult(entryTime, warehouseName, storageCondition);

        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(
                    shipment.getId(), shipment.getName(), ChainEventType.WAREHOUSE_EXIT, e.getMessage(), currentUser);
            throw e;
        }
    }

    private LocalDateTime extractEntryTime(ChainEvent latestEntry) {
        if (latestEntry.getEventData() != null) {
            try {
                Map<String, Object> data = objectMapper.readValue(latestEntry.getEventData(), new TypeReference<Map<String, Object>>() {});
                if (data.get("entryTime") != null) {
                    return LocalDateTime.parse(data.get("entryTime").toString());
                }
            } catch (Exception e) {
                return latestEntry.getRecordedAt();
            }
        }
        return latestEntry.getRecordedAt();
    }

    private String extractStringField(ChainEvent event, String key) {
        if (event.getEventData() != null) {
            try {
                Map<String, Object> data = objectMapper.readValue(event.getEventData(), new TypeReference<Map<String, Object>>() {});
                Object val = data.get(key);
                return val != null ? val.toString() : null;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private DurationResult calculateStorageDurations(LocalDateTime entryTime, LocalDateTime exitTime, ProductionLot lot) {
        long storageDurationHours = Duration.between(entryTime, exitTime).toHours();
        long storageDurationDays = Duration.between(entryTime, exitTime).toDays();

        Integer maxAllowedStorageDays = (lot != null && lot.getProductCategory() != null)
                ? lot.getProductCategory().getMaxStorageDays() : null;

        boolean isStorageExceeded = false;
        String warningMessage = null;
        if (maxAllowedStorageDays != null && storageDurationDays > maxAllowedStorageDays) {
            isStorageExceeded = true;
            String categoryName = lot.getProductCategory() != null ? lot.getProductCategory().getName() : "";
            warningMessage = "CẢNH BÁO: Thời gian lưu kho (" + storageDurationDays + " ngày) vượt quá ngưỡng bảo quản cho phép (" + maxAllowedStorageDays + " ngày) cho loại nông sản [" + categoryName + "]";
        }

        return new DurationResult(storageDurationHours, storageDurationDays, maxAllowedStorageDays, isStorageExceeded, warningMessage);
    }

    // ==========================================
    // SHARED PRIVATE HELPERS
    // ==========================================

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
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền ghi sự kiện cho lô hàng của tổ chức này.");
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

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Lỗi chuyển đổi dữ liệu sự kiện sang chuỗi JSON.");
        }
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

    private Map<String, Object> buildWarehouseEntryDataMap(Shipment shipment, ProductionLot lot, RecordWarehouseEntryRequest req) {
        Map<String, Object> m = new HashMap<>();
        m.put("shipmentId", shipment.getId().toString());
        m.put("shipmentName", shipment.getName());
        if (lot != null) {
            m.put("productionLotId", lot.getId().toString());
            m.put("productionLotName", lot.getName());
        }
        m.put("warehouseName", req.getWarehouseName());
        m.put("entryTime", req.getEntryTime().toString());
        if (req.getStorageCondition() != null) m.put("storageCondition", req.getStorageCondition());
        if (req.getNotes() != null) m.put("notes", req.getNotes());
        if (req.getImages() != null && !req.getImages().isEmpty()) m.put("images", req.getImages());
        m.put("deviceSource", req.getDeviceSource() != null ? req.getDeviceSource() : "WEB");
        return m;
    }

    private Map<String, Object> buildWarehouseExitDataMap(
            Shipment shipment, ProductionLot lot, ExitValidationResult exitVal,
            RecordWarehouseExitRequest req, DurationResult dur) {
        Map<String, Object> m = new HashMap<>();
        m.put("shipmentId", shipment.getId().toString());
        m.put("shipmentName", shipment.getName());
        if (lot != null) {
            m.put("productionLotId", lot.getId().toString());
            m.put("productionLotName", lot.getName());
        }
        m.put("warehouseName", exitVal.warehouseName());
        m.put("entryTime", exitVal.entryTime().toString());
        m.put("exitTime", req.getExitTime().toString());
        m.put("storageDurationDays", dur.storageDays());
        m.put("storageDurationHours", dur.storageHours());
        if (dur.maxDays() != null) m.put("maxAllowedStorageDays", dur.maxDays());
        m.put("isStorageExceeded", dur.isExceeded());
        if (dur.warning() != null) m.put("warningMessage", dur.warning());
        if (req.getDestination() != null) m.put("destination", req.getDestination());
        if (req.getNotes() != null) m.put("notes", req.getNotes());
        if (req.getImages() != null && !req.getImages().isEmpty()) m.put("images", req.getImages());
        m.put("deviceSource", req.getDeviceSource() != null ? req.getDeviceSource() : "WEB");
        return m;
    }

    private record ExitValidationResult(LocalDateTime entryTime, String warehouseName, String storageCondition) {}
    private record DurationResult(long storageHours, long storageDays, Integer maxDays, boolean isExceeded, String warning) {}
}
