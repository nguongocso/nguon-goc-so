package vn.nguongocso.event.service.processor;

import java.time.Clock;
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
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;

/**
 * Processor xử lý các sự kiện sơ chế và đóng gói sản phẩm tại hợp tác xã.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoopProcessingPackagingProcessor {

    private final ProductionLotRepository productionLotRepository;
    private final ChainEventRepository chainEventRepository;
    private final UserRepository userRepository;
    private final EventValidationService eventValidationService;
    private final MilestoneValidationService milestoneValidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Ghi nhận sự kiện sơ chế và phân loại cho lô sản xuất.
     */
    public ChainEventResponse recordPreprocessingEvent(
            RecordPreprocessingEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        validatePreprocessing(lot, request, currentUser);

        double lossRate = calculateLossRate(request.getInputQuantity(), request.getOutputQuantity());
        lot.setStatus(ProductionLotStatus.PREPROCESSED);
        lot.setActualQuantity(request.getOutputQuantity());
        productionLotRepository.save(lot);

        Map<String, Object> eventDataMap = buildPreprocessingDataMap(lot, request, lossRate);
        ChainEvent chainEvent = saveLotEvent(
                ChainEventType.PREPROCESSING, toJson(eventDataMap), request.getLatitude(),
                request.getLongitude(), getActor(currentUser), false, null);

        publishActivityLog(currentUser, "Ghi sự kiện sơ chế cho lô " + lot.getName(), chainEvent.getId().toString());
        return buildResponse(chainEvent, eventDataMap, request.getLatitude(), request.getLongitude(), chainEvent.getRecordedBy());
    }

    private void validatePreprocessing(ProductionLot lot, RecordPreprocessingEventRequest request, CustomUserDetails currentUser) {
        try {
            validateOrganization(lot, currentUser);
            if (lot.getStatus() == ProductionLotStatus.CANCELLED) {
                throw new BusinessException("Lô sản xuất đã bị hủy, không thể ghi sự kiện.");
            }
            if (lot.getStatus() != ProductionLotStatus.HARVESTED) {
                throw new BusinessException("Chỉ được ghi nhận sự kiện sơ chế cho lô đã thu hoạch.");
            }
            if (request.getOutputQuantity() > request.getInputQuantity()) {
                throw new BusinessException("Khối lượng sau sơ chế không được lớn hơn khối lượng vào.");
            }
            LocalDate today = clock != null ? LocalDate.now(clock) : LocalDate.now();
            if (request.getPreprocessingDate().isAfter(today)) {
                throw new BusinessException("Ngày sơ chế không được là ngày ở tương lai.");
            }
            if (lot.getHarvestDate() != null && request.getPreprocessingDate().isBefore(lot.getHarvestDate())) {
                throw new BusinessException("Ngày sơ chế phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(
                    request.getProductionLotId(), lot.getName(), ChainEventType.PREPROCESSING, e.getMessage(), currentUser);
            throw e;
        }
    }

    /**
     * Đính chính sự kiện sơ chế và phân loại đã ghi nhận.
     */
    public ChainEventResponse correctPreprocessingEvent(
            UUID originalEventId, CorrectPreprocessingEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ChainEvent originalEvent = chainEventRepository.findById(originalEventId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sự kiện sơ chế cần đính chính."));

        if (originalEvent.getEventType() != ChainEventType.PREPROCESSING) {
            throw new BusinessException("Sự kiện gốc không phải là sự kiện sơ chế.");
        }

        ProductionLot lot = resolveLotFromEventData(originalEvent.getEventData());
        validateOrganization(lot, currentUser);
        validateCorrectPreprocessingDates(lot, request);

        double lossRate = calculateLossRate(request.getInputQuantity(), request.getOutputQuantity());
        lot.setActualQuantity(request.getOutputQuantity());
        productionLotRepository.save(lot);

        Map<String, Object> eventDataMap = buildCorrectPreprocessingDataMap(lot, request, lossRate, originalEventId);
        ChainEvent correctionEvent = saveLotEvent(
                ChainEventType.PREPROCESSING, toJson(eventDataMap), request.getLatitude(),
                request.getLongitude(), getActor(currentUser), true, originalEvent);

        publishActivityLog(currentUser, "Đính chính sự kiện sơ chế cho lô " + lot.getName(), correctionEvent.getId().toString());
        return buildResponse(correctionEvent, eventDataMap, request.getLatitude(), request.getLongitude(), correctionEvent.getRecordedBy());
    }

    private void validateCorrectPreprocessingDates(ProductionLot lot, CorrectPreprocessingEventRequest request) {
        if (request.getOutputQuantity() > request.getInputQuantity()) {
            throw new BusinessException("Khối lượng sau sơ chế không được lớn hơn khối lượng vào.");
        }
        LocalDate today = clock != null ? LocalDate.now(clock) : LocalDate.now();
        if (request.getPreprocessingDate().isAfter(today)) {
            throw new BusinessException("Ngày sơ chế không được là ngày ở tương lai.");
        }
        if (lot.getHarvestDate() != null && request.getPreprocessingDate().isBefore(lot.getHarvestDate())) {
            throw new BusinessException("Ngày sơ chế phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
        }
    }

    /**
     * Ghi nhận sự kiện đóng gói sản phẩm cho lô sản xuất.
     */
    public ChainEventResponse recordPackagingEvent(
            RecordPackagingEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        validatePackaging(lot, request, currentUser);

        lot.setStatus(ProductionLotStatus.PACKAGED);
        productionLotRepository.save(lot);

        Map<String, Object> eventDataMap = buildPackagingDataMap(lot, request);
        ChainEvent chainEvent = saveLotEvent(
                ChainEventType.PACKAGING, toJson(eventDataMap), request.getLatitude(),
                request.getLongitude(), getActor(currentUser), false, null);

        publishActivityLog(currentUser, "Ghi sự kiện đóng gói cho lô " + lot.getName(), chainEvent.getId().toString());
        return buildResponse(chainEvent, eventDataMap, request.getLatitude(), request.getLongitude(), chainEvent.getRecordedBy());
    }

    private void validatePackaging(ProductionLot lot, RecordPackagingEventRequest request, CustomUserDetails currentUser) {
        try {
            validateOrganization(lot, currentUser);
            if (lot.getStatus() == ProductionLotStatus.CANCELLED) {
                throw new BusinessException("Lô sản xuất đã bị hủy, không thể ghi sự kiện.");
            }
            if (lot.getStatus() != ProductionLotStatus.HARVESTED && lot.getStatus() != ProductionLotStatus.PREPROCESSED) {
                throw new BusinessException("Chỉ được ghi nhận sự kiện đóng gói cho lô đã thu hoạch hoặc đã sơ chế.");
            }
            LocalDate today = clock != null ? LocalDate.now(clock) : LocalDate.now();
            if (request.getPackagingDate().isAfter(today)) {
                throw new BusinessException("Ngày đóng gói không được là ngày ở tương lai.");
            }
            if (lot.getHarvestDate() != null && request.getPackagingDate().isBefore(lot.getHarvestDate())) {
                throw new BusinessException("Ngày đóng gói phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
            }
            List<String> missingMilestones = milestoneValidationService.validateMilestoneCompletion(lot);
            if (!missingMilestones.isEmpty()) {
                throw new BusinessException("Lô chưa đủ mốc canh tác bắt buộc: " + String.join(", ", missingMilestones));
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(
                    request.getProductionLotId(), lot.getName(), ChainEventType.PACKAGING, e.getMessage(), currentUser);
            throw e;
        }
    }

    /**
     * Ghi nhận sự kiện đóng gói từ thiết bị di động.
     */
    public ChainEventResponse recordMobilePackagingEvent(RecordMobileEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        validateOrganization(lot, currentUser);

        if (request.getRecordedAt() != null && request.getRecordedAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Thời điểm ghi nhận không được là thời gian ở tương lai.");
        }

        RecordPackagingEventRequest packagingRequest = buildPackagingRequestFromMobile(lot, request);
        ChainEventResponse delegateResponse = recordPackagingEvent(packagingRequest, currentUser);

        Map<String, Object> enrichedData = new HashMap<>(delegateResponse.getEventData());
        enrichedData.put("images", request.getImages());
        enrichedData.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "MOBILE");

        return ChainEventResponse.builder()
                .id(delegateResponse.getId())
                .eventType(delegateResponse.getEventType())
                .eventData(enrichedData)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .recordedAt(delegateResponse.getRecordedAt())
                .recordedByName(delegateResponse.getRecordedByName())
                .createdAt(delegateResponse.getCreatedAt())
                .build();
    }

    private RecordPackagingEventRequest buildPackagingRequestFromMobile(ProductionLot lot, RecordMobileEventRequest request) {
        Map<String, Object> data = request.getEventData() != null ? request.getEventData() : Map.of();
        Object specObj = data.get("packagingSpecification");
        Object packagingDateStrObj = data.get("packagingDate");

        if (specObj == null || packagingDateStrObj == null) {
            throw new BusinessException("Thiếu thông tin quy cách hoặc ngày đóng gói.");
        }

        String packagingSpecification = specObj.toString();
        if (packagingSpecification.trim().isEmpty()) {
            throw new BusinessException("Quy cách đóng gói không được để trống");
        }
        if (packagingSpecification.length() > 255) {
            throw new BusinessException("Quy cách đóng gói không được vượt quá 255 ký tự");
        }

        LocalDate packagingDate = LocalDate.parse(packagingDateStrObj.toString());
        LocalDate today = clock != null ? LocalDate.now(clock) : LocalDate.now();
        if (packagingDate.isAfter(today)) {
            throw new BusinessException("Ngày đóng gói không được là ngày ở tương lai.");
        }
        if (lot.getHarvestDate() != null && packagingDate.isBefore(lot.getHarvestDate())) {
            throw new BusinessException("Ngày đóng gói phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
        }

        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(request.getProductionLotId());
        packagingRequest.setPackagingSpecification(packagingSpecification);
        packagingRequest.setPackagingDate(packagingDate);
        packagingRequest.setLatitude(request.getLatitude());
        packagingRequest.setLongitude(request.getLongitude());
        return packagingRequest;
    }

    /**
     * Đính chính sự kiện đóng gói sản phẩm đã ghi nhận.
     */
    public ChainEventResponse correctPackagingEvent(
            UUID originalEventId, CorrectPackagingEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ChainEvent originalEvent = chainEventRepository.findById(originalEventId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sự kiện đóng gói cần đính chính."));

        if (originalEvent.getEventType() != ChainEventType.PACKAGING) {
            throw new BusinessException("Sự kiện gốc không phải là sự kiện đóng gói.");
        }

        ProductionLot lot = resolveLotFromEventData(originalEvent.getEventData());
        validateOrganization(lot, currentUser);

        LocalDate today = clock != null ? LocalDate.now(clock) : LocalDate.now();
        if (request.getPackagingDate().isAfter(today)) {
            throw new BusinessException("Ngày đóng gói không được là ngày ở tương lai.");
        }
        if (lot.getHarvestDate() != null && request.getPackagingDate().isBefore(lot.getHarvestDate())) {
            throw new BusinessException("Ngày đóng gói phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
        }

        Map<String, Object> eventDataMap = buildCorrectPackagingDataMap(lot, request, originalEventId);
        ChainEvent correctionEvent = saveLotEvent(
                ChainEventType.PACKAGING, toJson(eventDataMap), request.getLatitude(),
                request.getLongitude(), getActor(currentUser), true, originalEvent);

        publishActivityLog(currentUser, "Đính chính sự kiện đóng gói cho lô " + lot.getName(), correctionEvent.getId().toString());
        return buildResponse(correctionEvent, eventDataMap, request.getLatitude(), request.getLongitude(), correctionEvent.getRecordedBy());
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

    private Map<String, Object> buildPreprocessingDataMap(ProductionLot lot, RecordPreprocessingEventRequest req, double lossRate) {
        Map<String, Object> m = new HashMap<>();
        m.put("productionLotId", lot.getId().toString());
        m.put("productionLotName", lot.getName());
        m.put("inputQuantity", req.getInputQuantity());
        m.put("outputQuantity", req.getOutputQuantity());
        m.put("lossRate", lossRate);
        if (req.getGrade() != null) m.put("grade", req.getGrade());
        if (req.getProcessingMethod() != null) m.put("processingMethod", req.getProcessingMethod());
        m.put("preprocessingDate", req.getPreprocessingDate().toString());
        if (req.getImages() != null && !req.getImages().isEmpty()) m.put("images", req.getImages());
        m.put("deviceSource", req.getDeviceSource() != null ? req.getDeviceSource() : "WEB");
        return m;
    }

    private Map<String, Object> buildCorrectPreprocessingDataMap(ProductionLot lot, CorrectPreprocessingEventRequest req, double lossRate, UUID origId) {
        Map<String, Object> m = new HashMap<>();
        m.put("productionLotId", lot.getId().toString());
        m.put("productionLotName", lot.getName());
        m.put("inputQuantity", req.getInputQuantity());
        m.put("outputQuantity", req.getOutputQuantity());
        m.put("lossRate", lossRate);
        if (req.getGrade() != null) m.put("grade", req.getGrade());
        if (req.getProcessingMethod() != null) m.put("processingMethod", req.getProcessingMethod());
        m.put("preprocessingDate", req.getPreprocessingDate().toString());
        m.put("correctionReason", req.getCorrectionReason());
        m.put("parentEventId", origId.toString());
        return m;
    }

    private Map<String, Object> buildPackagingDataMap(ProductionLot lot, RecordPackagingEventRequest req) {
        Map<String, Object> m = new HashMap<>();
        m.put("productionLotId", lot.getId().toString());
        m.put("productionLotName", lot.getName());
        m.put("packagingSpecification", req.getPackagingSpecification());
        m.put("packagingDate", req.getPackagingDate().toString());
        if (req.getImages() != null && !req.getImages().isEmpty()) m.put("images", req.getImages());
        m.put("deviceSource", req.getDeviceSource() != null ? req.getDeviceSource() : "WEB");
        return m;
    }

    private Map<String, Object> buildCorrectPackagingDataMap(ProductionLot lot, CorrectPackagingEventRequest req, UUID origId) {
        Map<String, Object> m = new HashMap<>();
        m.put("productionLotId", lot.getId().toString());
        m.put("productionLotName", lot.getName());
        m.put("packagingSpecification", req.getPackagingSpecification());
        m.put("packagingDate", req.getPackagingDate().toString());
        m.put("correctionReason", req.getCorrectionReason());
        m.put("parentEventId", origId.toString());
        return m;
    }
}
