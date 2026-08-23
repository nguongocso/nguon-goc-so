package vn.nguongocso.event.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.annotation.Auditable;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.dto.request.*;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.ChainVerificationResponse;
import vn.nguongocso.event.dto.response.EventVerificationItem;
import vn.nguongocso.event.dto.response.ScanLookupResponse;
import vn.nguongocso.event.dto.response.StorageConditionResponse;
import vn.nguongocso.event.dto.response.ThresholdInfo;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.EventHashService;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation của dịch vụ sự kiện chuỗi cung ứng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChainEventServiceImpl implements ChainEventService {
    private final ChainEventRepository chainEventRepository;
    private final ProductionLotRepository productionLotRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final TraceCodeRepository traceCodeRepository;
    private final ShipmentRepository shipmentRepository;
    private final EventValidationService eventValidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final PermissionChecker permissionChecker;
    private final OrganizationUserRepository organizationUserRepository;
    private final EventHashService eventHashService;
    private final Clock clock;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Ghi nhận sự kiện thu hoạch cho lô sản xuất.
     *
     * @param request     yêu cầu ghi nhận sự kiện thu hoạch
     * @param currentUser người dùng hiện tại
     * @return phản hồi sự kiện chuỗi cung ứng
     */
    @Override
    @Transactional
    @Auditable(action = "RECORD_HARVEST_EVENT", entityType = "CHAIN_EVENT", description = "'Ghi nhận sự kiện thu hoạch cho lô sản xuất ID: ' + #request.productionLotId + ', Sản lượng: ' + #request.quantity + ' kg'")
    public ChainEventResponse recordHarvestEvent(RecordHarvestEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        try {
            validateOrganization(lot, currentUser);
            if (lot.getStatus() != ProductionLotStatus.APPROVED) {
                throw new BusinessException("Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(request.getProductionLotId(), lot.getName(),
                    ChainEventType.HARVEST, e.getMessage(), currentUser);
            throw e;
        }

        // Cập nhật trạng thái lô
        lot.setStatus(ProductionLotStatus.HARVESTED);
        lot.setHarvestDate(request.getHarvestDate());
        lot.setActualQuantity(request.getQuantity());
        productionLotRepository.save(lot);

        // Tọa độ
        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());

        // Dữ liệu sự kiện
        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", lot.getId().toString());
        eventDataMap.put("productionLotName", lot.getName());
        eventDataMap.put("harvestDate", request.getHarvestDate().toString());
        eventDataMap.put("quantity", request.getQuantity());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            eventDataMap.put("images", request.getImages());
        }
        eventDataMap.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "WEB");

        String eventDataJson = toJson(eventDataMap);

        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .eventType(ChainEventType.HARVEST)
                .eventData(eventDataJson)
                .location(locationPoint)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        chainEvent = chainEventRepository.save(chainEvent);

        publishActivityLog(currentUser, "Ghi sự kiện thu hoạch cho lô " + lot.getName(),
                "ChainEvent", chainEvent.getId().toString());

        return buildResponse(chainEvent, eventDataMap, request.getLatitude(), request.getLongitude(), actor);
    }

    /**
     * Ghi nhận sự kiện sơ chế và phân loại cho lô sản xuất.
     *
     * @param request     yêu cầu ghi nhận sự kiện sơ chế
     * @param currentUser người dùng hiện tại
     * @return phản hồi sự kiện chuỗi cung ứng
     */
    @Override
    @Transactional
    @Auditable(action = "RECORD_PREPROCESSING_EVENT", entityType = "CHAIN_EVENT", description = "'Ghi nhận sự kiện sơ chế cho lô sản xuất ID: ' + #request.productionLotId + ', Khối lượng vào: ' + #request.inputQuantity + ' kg, Khối lượng ra: ' + #request.outputQuantity + ' kg'")
    public ChainEventResponse recordPreprocessingEvent(RecordPreprocessingEventRequest request,
            CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        try {
            validateOrganization(lot, currentUser);
            if (lot.getStatus() != ProductionLotStatus.HARVESTED) {
                throw new BusinessException("Chỉ được ghi nhận sự kiện sơ chế cho lô đã thu hoạch.");
            }
            if (request.getOutputQuantity() > request.getInputQuantity()) {
                throw new BusinessException("Khối lượng sau sơ chế không được lớn hơn khối lượng vào.");
            }
            if (request.getPreprocessingDate().isAfter(LocalDate.now(clock))) {
                throw new BusinessException("Ngày sơ chế không được là ngày ở tương lai.");
            }
            if (lot.getHarvestDate() != null && request.getPreprocessingDate().isBefore(lot.getHarvestDate())) {
                throw new BusinessException("Ngày sơ chế phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(request.getProductionLotId(), lot.getName(),
                    ChainEventType.PREPROCESSING, e.getMessage(), currentUser);
            throw e;
        }

        // Tính tỷ lệ hao hụt (%)
        double lossRate = 0.0;
        if (request.getInputQuantity() > 0) {
            lossRate = (request.getInputQuantity() - request.getOutputQuantity()) / request.getInputQuantity() * 100.0;
            lossRate = Math.round(lossRate * 100.0) / 100.0;
        }

        // Cập nhật trạng thái lô và khối lượng thực tế mới
        lot.setStatus(ProductionLotStatus.PREPROCESSED);
        lot.setActualQuantity(request.getOutputQuantity());
        productionLotRepository.save(lot);

        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", lot.getId().toString());
        eventDataMap.put("productionLotName", lot.getName());
        eventDataMap.put("inputQuantity", request.getInputQuantity());
        eventDataMap.put("outputQuantity", request.getOutputQuantity());
        eventDataMap.put("lossRate", lossRate);
        if (request.getGrade() != null) {
            eventDataMap.put("grade", request.getGrade());
        }
        if (request.getProcessingMethod() != null) {
            eventDataMap.put("processingMethod", request.getProcessingMethod());
        }
        eventDataMap.put("preprocessingDate", request.getPreprocessingDate().toString());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            eventDataMap.put("images", request.getImages());
        }
        eventDataMap.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "WEB");

        String eventDataJson = toJson(eventDataMap);
        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .eventType(ChainEventType.PREPROCESSING)
                .eventData(eventDataJson)
                .location(locationPoint)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        chainEvent = chainEventRepository.save(chainEvent);

        publishActivityLog(currentUser, "Ghi sự kiện sơ chế cho lô " + lot.getName(),
                "ChainEvent", chainEvent.getId().toString());

        return buildResponse(chainEvent, eventDataMap, request.getLatitude(), request.getLongitude(), actor);
    }

    /**
     * Đính chính sự kiện sơ chế cho lô sản xuất.
     *
     * @param originalEventId ID sự kiện gốc
     * @param request         yêu cầu đính chính sự kiện sơ chế
     * @param currentUser     người dùng hiện tại
     * @return phản hồi sự kiện chuỗi cung ứng
     */
    @Override
    @Transactional
    @Auditable(action = "CORRECT_PREPROCESSING_EVENT", entityType = "CHAIN_EVENT", description = "'Đính chính thông tin sơ chế cho sự kiện gốc ID: ' + #originalEventId")
    public ChainEventResponse correctPreprocessingEvent(UUID originalEventId, CorrectPreprocessingEventRequest request,
            CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ChainEvent originalEvent = chainEventRepository.findById(originalEventId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sự kiện sơ chế cần đính chính."));

        if (originalEvent.getEventType() != ChainEventType.PREPROCESSING) {
            throw new BusinessException("Sự kiện gốc không phải là sự kiện sơ chế.");
        }

        Map<String, Object> originalDataMap = parseEventData(originalEvent.getEventData());
        String productionLotIdStr = (String) originalDataMap.get("productionLotId");
        if (productionLotIdStr == null) {
            throw new BusinessException("Không tìm thấy thông tin lô sản xuất trong sự kiện gốc.");
        }
        UUID productionLotId = UUID.fromString(productionLotIdStr);

        ProductionLot lot = productionLotRepository.findById(productionLotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        validateOrganization(lot, currentUser);

        if (request.getOutputQuantity() > request.getInputQuantity()) {
            throw new BusinessException("Khối lượng sau sơ chế không được lớn hơn khối lượng vào.");
        }

        if (request.getPreprocessingDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessException("Ngày sơ chế không được là ngày ở tương lai.");
        }
        if (lot.getHarvestDate() != null && request.getPreprocessingDate().isBefore(lot.getHarvestDate())) {
            throw new BusinessException("Ngày sơ chế phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
        }

        // Tính tỷ lệ hao hụt (%) đính chính
        double lossRate = 0.0;
        if (request.getInputQuantity() > 0) {
            lossRate = (request.getInputQuantity() - request.getOutputQuantity()) / request.getInputQuantity() * 100.0;
            lossRate = Math.round(lossRate * 100.0) / 100.0;
        }

        // Cập nhật khối lượng thực tế của lô
        lot.setActualQuantity(request.getOutputQuantity());
        productionLotRepository.save(lot);

        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", lot.getId().toString());
        eventDataMap.put("productionLotName", lot.getName());
        eventDataMap.put("inputQuantity", request.getInputQuantity());
        eventDataMap.put("outputQuantity", request.getOutputQuantity());
        eventDataMap.put("lossRate", lossRate);
        if (request.getGrade() != null) {
            eventDataMap.put("grade", request.getGrade());
        }
        if (request.getProcessingMethod() != null) {
            eventDataMap.put("processingMethod", request.getProcessingMethod());
        }
        eventDataMap.put("preprocessingDate", request.getPreprocessingDate().toString());
        eventDataMap.put("correctionReason", request.getCorrectionReason());
        eventDataMap.put("parentEventId", originalEventId.toString());

        String eventDataJson = toJson(eventDataMap);
        User actor = getActor(currentUser);

        ChainEvent correctionEvent = ChainEvent.builder()
                .eventType(ChainEventType.PREPROCESSING)
                .eventData(eventDataJson)
                .location(locationPoint)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .parentEvent(originalEvent)
                .isCorrection(true)
                .build();

        correctionEvent = chainEventRepository.save(correctionEvent);

        publishActivityLog(currentUser, "Đính chính sự kiện sơ chế cho lô " + lot.getName(),
                "ChainEvent", correctionEvent.getId().toString());

        return buildResponse(correctionEvent, eventDataMap, request.getLatitude(), request.getLongitude(), actor);
    }

    /**
     * Ghi nhận sự kiện đóng gói cho lô sản xuất.
     *
     * @param request     yêu cầu ghi nhận sự kiện đóng gói
     * @param currentUser người dùng hiện tại
     * @return phản hồi sự kiện chuỗi cung ứng
     */
    @Override
    @Transactional
    @Auditable(action = "RECORD_PACKAGING_EVENT", entityType = "CHAIN_EVENT", description = "'Ghi nhận sự kiện đóng gói cho lô sản xuất ID: ' + #request.productionLotId + ', Quy cách: ' + #request.packagingSpecification")
    public ChainEventResponse recordPackagingEvent(RecordPackagingEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        try {
            validateOrganization(lot, currentUser);
            if (lot.getStatus() != ProductionLotStatus.HARVESTED
                    && lot.getStatus() != ProductionLotStatus.PREPROCESSED) {
                throw new BusinessException("Chỉ được ghi nhận sự kiện đóng gói cho lô đã thu hoạch hoặc đã sơ chế.");
            }
            if (request.getPackagingDate().isAfter(LocalDate.now())) {
                throw new BusinessException("Ngày đóng gói không được là ngày ở tương lai.");
            }
            if (lot.getHarvestDate() != null && request.getPackagingDate().isBefore(lot.getHarvestDate())) {
                throw new BusinessException("Ngày đóng gói phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(request.getProductionLotId(), lot.getName(),
                    ChainEventType.PACKAGING, e.getMessage(), currentUser);
            throw e;
        }

        lot.setStatus(ProductionLotStatus.PACKAGED);
        productionLotRepository.save(lot);

        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", lot.getId().toString());
        eventDataMap.put("productionLotName", lot.getName());
        eventDataMap.put("packagingSpecification", request.getPackagingSpecification());
        eventDataMap.put("packagingDate", request.getPackagingDate().toString());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            eventDataMap.put("images", request.getImages());
        }
        eventDataMap.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "WEB");

        String eventDataJson = toJson(eventDataMap);
        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .eventType(ChainEventType.PACKAGING)
                .eventData(eventDataJson)
                .location(locationPoint)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        chainEvent = chainEventRepository.save(chainEvent);

        publishActivityLog(currentUser, "Ghi sự kiện đóng gói cho lô " + lot.getName(),
                "ChainEvent", chainEvent.getId().toString());

        return buildResponse(chainEvent, eventDataMap, request.getLatitude(), request.getLongitude(), actor);
    }

    /**
     * Đính chính sự kiện đóng gói cho lô sản xuất.
     *
     * @param originalEventId ID sự kiện gốc
     * @param request         yêu cầu đính chính sự kiện đóng gói
     * @param currentUser     người dùng hiện tại
     * @return phản hồi sự kiện chuỗi cung ứng
     */
    @Override
    @Transactional
    @Auditable(action = "CORRECT_PACKAGING_EVENT", entityType = "CHAIN_EVENT", description = "'Đính chính thông tin đóng gói cho sự kiện gốc ID: ' + #originalEventId")
    public ChainEventResponse correctPackagingEvent(UUID originalEventId, CorrectPackagingEventRequest request,
            CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ChainEvent originalEvent = chainEventRepository.findById(originalEventId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sự kiện đóng gói cần đính chính."));

        if (originalEvent.getEventType() != ChainEventType.PACKAGING) {
            throw new BusinessException("Sự kiện gốc không phải là sự kiện đóng gói.");
        }

        Map<String, Object> originalDataMap = parseEventData(originalEvent.getEventData());
        String productionLotIdStr = (String) originalDataMap.get("productionLotId");
        if (productionLotIdStr == null) {
            throw new BusinessException("Không tìm thấy thông tin lô sản xuất trong sự kiện gốc.");
        }
        UUID productionLotId = UUID.fromString(productionLotIdStr);

        ProductionLot lot = productionLotRepository.findById(productionLotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        validateOrganization(lot, currentUser);

        if (request.getPackagingDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Ngày đóng gói không được là ngày ở tương lai.");
        }
        if (lot.getHarvestDate() != null && request.getPackagingDate().isBefore(lot.getHarvestDate())) {
            throw new BusinessException("Ngày đóng gói phải sau hoặc bằng ngày thu hoạch của lô sản xuất.");
        }

        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", lot.getId().toString());
        eventDataMap.put("productionLotName", lot.getName());
        eventDataMap.put("packagingSpecification", request.getPackagingSpecification());
        eventDataMap.put("packagingDate", request.getPackagingDate().toString());
        eventDataMap.put("correctionReason", request.getCorrectionReason());
        eventDataMap.put("parentEventId", originalEventId.toString());

        String eventDataJson = toJson(eventDataMap);
        User actor = getActor(currentUser);

        ChainEvent correctionEvent = ChainEvent.builder()
                .eventType(ChainEventType.PACKAGING)
                .eventData(eventDataJson)
                .location(locationPoint)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .parentEvent(originalEvent)
                .isCorrection(true)
                .build();

        correctionEvent = chainEventRepository.save(correctionEvent);

        publishActivityLog(currentUser, "Đính chính sự kiện đóng gói cho lô " + lot.getName(),
                "ChainEvent", correctionEvent.getId().toString());

        return buildResponse(correctionEvent, eventDataMap, request.getLatitude(), request.getLongitude(), actor);
    }

    /**
     * Ghi nhận sự kiện vận chuyển cho lô sản xuất.
     *
     * @param request     yêu cầu ghi nhận sự kiện vận chuyển
     * @param currentUser người dùng hiện tại
     * @return phản hồi sự kiện chuỗi cung ứng
     */
    @Override
    @Transactional
    @Auditable(action = "RECORD_TRANSPORT_EVENT", entityType = "CHAIN_EVENT", description = "'Ghi nhận sự kiện vận chuyển mã tem: ' + #request.codeValue + ', Từ: ' + #request.fromLocation + ', Đến: ' + #request.toLocation")
    public ChainEventResponse recordTransportEvent(RecordTransportEventRequest request, CustomUserDetails currentUser) {
        if (!"VT-03".equals(currentUser.getRoleCode())) {
            throw new BusinessException("Bạn không có quyền ghi sự kiện vận chuyển.");
        }

        TraceCode traceCode = traceCodeRepository.findByCodeValue(request.getCodeValue())
                .orElseThrow(() -> new BusinessException("Mã lô hàng không tồn tại."));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new BusinessException("Mã truy xuất chưa được gắn với lô hàng.");
        }

        try {
            validateOrganization(shipment, currentUser);
            if (shipment.getStatus() == ShipmentStatus.RECALLED) {
                throw new BusinessException("Lô hàng đã bị thu hồi, không thể ghi sự kiện vận chuyển.");
            }
            if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
                throw new BusinessException("Lô hàng chưa được kích hoạt, không thể ghi sự kiện vận chuyển.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(shipment.getId(), shipment.getName(),
                    ChainEventType.TRANSPORT, e.getMessage(), currentUser);
            throw e;
        }

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("fromLocation", request.getFromLocation());
        eventDataMap.put("toLocation", request.getToLocation());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            eventDataMap.put("images", request.getImages());
        }
        eventDataMap.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "WEB");

        String eventDataJson = toJson(eventDataMap);
        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.TRANSPORT)
                .eventData(eventDataJson)
                .recordedAt(request.getTransportTime())
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        chainEvent = saveWithChainHash(chainEvent);

        publishActivityLog(currentUser, "Ghi sự kiện vận chuyển cho lô hàng " + shipment.getName(),
                "ChainEvent", chainEvent.getId().toString());

        return ChainEventResponse.builder()
                .id(chainEvent.getId())
                .shipmentId(shipment.getId())
                .eventType(chainEvent.getEventType())
                .eventData(eventDataMap)
                .latitude(null)
                .longitude(null)
                .recordedAt(chainEvent.getRecordedAt())
                .recordedByName(actor.getFullName())
                .createdAt(chainEvent.getCreatedAt())
                .build();
    }

    /**
     * Ghi nhận sự kiện ngoại tuyến từ thiết bị di động.
     */
    @Override
    @Transactional
    public ChainEventResponse recordMobileEvent(RecordMobileEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        validateOrganization(lot, currentUser);

        if (request.getRecordedAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Thời điểm ghi nhận không được là thời gian ở tương lai.");
        }

        // Delegate to shared online service methods to ensure consistent validation and
        // processing
        ChainEventResponse delegateResponse;
        if (request.getEventType() == ChainEventType.HARVEST) {
            delegateResponse = delegateHarvestFromMobile(lot, request, currentUser);
        } else if (request.getEventType() == ChainEventType.PACKAGING) {
            delegateResponse = delegatePackagingFromMobile(lot, request, currentUser);
        } else {
            throw new BusinessException("Loại sự kiện không được hỗ trợ ghi nhận từ thiết bị di động.");
        }

        // Enrich response with mobile-specific fields (deviceSource, images)
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

    /**
     * Lấy dòng thời gian của một lô hàng.
     *
     * @param shipmentId ID lô hàng
     * @return danh sách sự kiện trong dòng thời gian
     */
    @Override
    public List<ChainEventResponse> getShipmentTimeline(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException("Lô hàng không tồn tại."));

        List<ChainEvent> shipmentEvents = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId);

        List<ChainEvent> productionLotEvents = Collections.emptyList();
        if (shipment.getProductionLot() != null) {
            UUID productionLotId = shipment.getProductionLot().getId();
            List<ChainEvent> allUnassignedEvents = chainEventRepository.findByShipmentIsNullAndEventTypeIn(
                    List.of(ChainEventType.HARVEST, ChainEventType.PREPROCESSING, ChainEventType.PACKAGING));
            productionLotEvents = allUnassignedEvents.stream()
                    .filter(e -> {
                        Map<String, Object> data = parseEventData(e.getEventData());
                        Object lotId = data.get("productionLotId");
                        return lotId != null && lotId.toString().equals(productionLotId.toString());
                    })
                    .collect(Collectors.toList());
        }

        List<ChainEvent> allEvents = new ArrayList<>();
        allEvents.addAll(shipmentEvents);
        allEvents.addAll(productionLotEvents);
        allEvents.sort(Comparator.comparing(ChainEvent::getRecordedAt));

        return allEvents.stream()
                .map(this::toChainEventResponse)
                .collect(Collectors.toList());
    }

    private void validateEventPermission(CustomUserDetails currentUser) {
        String role = currentUser.getRoleCode();
        if (!"VT-02".equals(role) && !"VT-03".equals(role)) {
            throw new BusinessException("Chỉ thành viên được cấp quyền trong tổ chức mới được ghi sự kiện.");
        }
    }

    private void validateOrganization(ProductionLot lot, CustomUserDetails currentUser) {
        if (!lot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không thuộc tổ chức quản lý của lô sản xuất này.");
        }
    }

    private void validateOrganization(Shipment shipment, CustomUserDetails currentUser) {
        if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không thuộc tổ chức quản lý của lô hàng.");
        }
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

    private User getActor(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người ghi nhận."));
    }

    private void publishActivityLog(CustomUserDetails currentUser, String description,
            String entityType, String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("CREATE")
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private ChainEventResponse buildResponse(ChainEvent event, Map<String, Object> eventData,
            Double latitude, Double longitude, User actor) {
        return ChainEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .eventData(eventData)
                .latitude(latitude)
                .longitude(longitude)
                .recordedAt(event.getRecordedAt())
                .recordedByName(actor.getFullName())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private Map<String, Object> parseEventData(String eventDataJson) {
        if (eventDataJson == null || eventDataJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(eventDataJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Không thể parse eventData: {}", eventDataJson);
            return new HashMap<>();
        }
    }

    private ChainEventResponse toChainEventResponse(ChainEvent event) {
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
                .build();
    }

    /**
     * Delegates harvest event creation from mobile to the online recordHarvestEvent
     * method.
     * Constructs a RecordHarvestEventRequest from mobile DTO fields and delegates.
     */
    private ChainEventResponse delegateHarvestFromMobile(ProductionLot lot, RecordMobileEventRequest request,
            CustomUserDetails currentUser) {
        Object quantityObj = request.getEventData().get("quantity");
        Object harvestDateStrObj = request.getEventData().get("harvestDate");

        if (quantityObj == null || harvestDateStrObj == null) {
            throw new BusinessException("Thiếu dữ liệu sản lượng hoặc ngày thu hoạch.");
        }

        Double quantity = Double.valueOf(quantityObj.toString());
        if (quantity <= 0) {
            throw new BusinessException("Sản lượng thu hoạch phải lớn hơn 0");
        }

        LocalDate harvestDate = LocalDate.parse(harvestDateStrObj.toString());
        if (harvestDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Ngày thu hoạch không được là ngày ở tương lai.");
        }

        RecordHarvestEventRequest harvestRequest = new RecordHarvestEventRequest();
        harvestRequest.setProductionLotId(request.getProductionLotId());
        harvestRequest.setHarvestDate(harvestDate);
        harvestRequest.setQuantity(quantity);
        harvestRequest.setLatitude(request.getLatitude());
        harvestRequest.setLongitude(request.getLongitude());

        // Delegate to the shared online method — ensures identical validation, status
        // updates, and error logging
        return recordHarvestEvent(harvestRequest, currentUser);
    }

    /**
     * Delegates packaging event creation from mobile to the online
     * recordPackagingEvent method.
     * Constructs a RecordPackagingEventRequest from mobile DTO fields and
     * delegates.
     */
    private ChainEventResponse delegatePackagingFromMobile(ProductionLot lot, RecordMobileEventRequest request,
            CustomUserDetails currentUser) {
        Object specObj = request.getEventData().get("packagingSpecification");
        Object packagingDateStrObj = request.getEventData().get("packagingDate");

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
        if (packagingDate.isAfter(LocalDate.now())) {
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

        // Delegate to the shared online method — ensures identical validation, status
        // updates, and error logging
        return recordPackagingEvent(packagingRequest, currentUser);
    }

    /**
     * Tra cứu thông tin lô hàng dựa trên mã truy xuất.
     *
     * @param codeValue   giá trị mã truy xuất
     * @param currentUser người dùng hiện tại
     * @return phản hồi tra cứu thông tin lô hàng
     */
    @Override
    @Transactional(readOnly = true)
    public ScanLookupResponse scanLookup(String codeValue, CustomUserDetails currentUser) {

        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new BusinessException("Mã truy xuất không tồn tại."));

        Shipment shipment = traceCode.getShipment();

        if (shipment == null) {
            throw new BusinessException("Mã truy xuất chưa được gắn với lô hàng.");
        }

        // For VT-04 (procurement company), skip organization validation since
        // they belong to a different organization than the shipment's cooperative.
        // The procurement relationship check will be done when recording the event.
        if (!"VT-04".equals(currentUser.getRoleCode())) {
            validateOrganization(shipment, currentUser);
        }

        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Lô hàng đã bị thu hồi.");
        }

        if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
            throw new BusinessException("Lô hàng chưa được kích hoạt.");
        }

        Optional<ChainEvent> latestEvent = chainEventRepository
                .findTopByShipmentIdOrderByRecordedAtDesc(shipment.getId());

        List<String> allowedEventTypes = determineAllowedEventTypes(latestEvent);

        ProductionLot productionLot = shipment.getProductionLot();

        // Ngưỡng bảo quản của loại nông sản để hiển thị trước khi nhập số liệu
        vn.nguongocso.farm.entity.ProductCategory productCategory = productionLot.getProductCategory();
        Double tempMin = productCategory != null ? productCategory.getTempMin() : null;
        Double tempMax = productCategory != null ? productCategory.getTempMax() : null;
        Double humidityMin = productCategory != null ? productCategory.getHumidityMin() : null;
        Double humidityMax = productCategory != null ? productCategory.getHumidityMax() : null;

        ThresholdInfo thresholds = null;
        if (tempMin != null || tempMax != null || humidityMin != null || humidityMax != null) {
            thresholds = ThresholdInfo.builder()
                    .tempMin(tempMin)
                    .tempMax(tempMax)
                    .humidityMin(humidityMin)
                    .humidityMax(humidityMax)
                    .build();
        }

        return ScanLookupResponse.builder()
                .valid(true)
                .message(null)
                .traceCode(traceCode.getCodeValue())
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .shipmentStatus(shipment.getStatus().name())
                .productionLotId(productionLot.getId())
                .productCategoryName(
                        productionLot.getProductCategory() != null
                                ? productionLot.getProductCategory().getName()
                                : null)
                .farmAreaName(
                        productionLot.getFarmArea() != null
                                ? productionLot.getFarmArea().getName()
                                : null)
                .organizationId(shipment.getOrganization().getOrganizationId())
                .organizationName(shipment.getOrganization().getName())
                .allowedEventTypes(allowedEventTypes)
                .lastEventType(
                        latestEvent.map(e -> e.getEventType().name()).orElse(null))
                .lastEventRecordedAt(
                        latestEvent.map(ChainEvent::getRecordedAt).orElse(null))
                .totalQuantity(shipment.getTotalQuantity())
                .thresholds(thresholds)
                .build();
    }

    private List<String> determineAllowedEventTypes(Optional<ChainEvent> latestEvent) {

        if (latestEvent.isEmpty()) {
            return List.of(ChainEventType.TRANSPORT.name());
        }

        ChainEventType lastType = latestEvent.get().getEventType();

        return switch (lastType) {
            case TRANSPORT -> List.of(ChainEventType.TRANSPORT.name());
            default -> Collections.emptyList();
        };
    }

    /**
     * Ghi nhận mốc điều kiện bảo quản khi vận chuyển.
     */
    @Override
    @Transactional
    @Auditable(action = "RECORD_STORAGE_CONDITION", entityType = "CHAIN_EVENT", description = "'Ghi nhận điều kiện bảo quản mã tem: ' + #request.codeValue + ', Nhiệt độ: ' + #request.temperature + '°C, Độ ẩm: ' + #request.humidity + '%'")
    public StorageConditionResponse recordStorageCondition(StorageConditionRequest request,
            CustomUserDetails currentUser) {

        // 1. Validate role: VT-03 or VT-04 only
        String role = currentUser.getRoleCode();
        if (!"VT-03".equals(role) && !"VT-04".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi nhận điều kiện bảo quản cho lô hàng này.");
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

        // 4. Validate authorization based on role
        if ("VT-04".equals(role)) {
            // Procurement company: validate via PROCUREMENT relationship (may belong to
            // a different organization than the producer/cooperative)
            validateStorageProcurementRelationship(shipment, currentUser);
        } else {
            // VT-03: must belong to the shipment's managing organization
            if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Bạn không thuộc tổ chức quản lý của lô hàng này.");
            }
        }

        // 5. Validate shipment status (QTN-05)
        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException(
                    "Lô hàng chưa được kích hoạt hoặc đã bị thu hồi, không thể ghi nhận mốc bảo quản.");
        }
        if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
            throw new BusinessException(
                    "Lô hàng chưa được kích hoạt hoặc đã bị thu hồi, không thể ghi nhận mốc bảo quản.");
        }

        // 5b. Transportation precondition: the lot must have at least one TRANSPORT
        // event
        boolean hasTransportEvent = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .anyMatch(e -> e.getEventType() == ChainEventType.TRANSPORT);
        if (!hasTransportEvent) {
            throw new BusinessException("Lô hàng phải có sự kiện vận chuyển trước khi ghi nhận điều kiện bảo quản.");
        }

        // 6. Get ProductCategory thresholds
        vn.nguongocso.farm.entity.ProductCategory productCategory = null;
        ProductionLot productionLot = shipment.getProductionLot();
        if (productionLot != null) {
            productCategory = productionLot.getProductCategory();
        }

        Double tempMin = productCategory != null ? productCategory.getTempMin() : null;
        Double tempMax = productCategory != null ? productCategory.getTempMax() : null;
        Double humidityMin = productCategory != null ? productCategory.getHumidityMin() : null;
        Double humidityMax = productCategory != null ? productCategory.getHumidityMax() : null;

        // 7. Compare temperature and humidity against thresholds
        boolean isTempExceeded = false;
        boolean isHumidityExceeded = false;

        if (tempMin != null && tempMax != null) {
            isTempExceeded = request.getTemperature() < tempMin || request.getTemperature() > tempMax;
        }
        if (humidityMin != null && humidityMax != null) {
            isHumidityExceeded = request.getHumidity() < humidityMin || request.getHumidity() > humidityMax;
        }

        String alertLevel;
        if (isTempExceeded && isHumidityExceeded) {
            alertLevel = "CRITICAL";
        } else if (isTempExceeded || isHumidityExceeded) {
            alertLevel = "WARNING";
        } else {
            alertLevel = "OK";
        }

        // 8. Resolve recordedAt
        LocalDateTime recordedAt = request.getRecordedAt() != null
                ? request.getRecordedAt()
                : LocalDateTime.now();

        // 9. Build ThresholdInfo
        ThresholdInfo thresholds = null;
        if (tempMin != null || tempMax != null || humidityMin != null || humidityMax != null) {
            thresholds = ThresholdInfo.builder()
                    .tempMin(tempMin)
                    .tempMax(tempMax)
                    .humidityMin(humidityMin)
                    .humidityMax(humidityMax)
                    .build();
        }

        // 10. Build eventData JSON
        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("shipmentId", shipment.getId().toString());
        eventDataMap.put("shipmentName", shipment.getName());
        eventDataMap.put("temperature", request.getTemperature());
        eventDataMap.put("humidity", request.getHumidity());
        eventDataMap.put("isTemperatureExceeded", isTempExceeded);
        eventDataMap.put("isHumidityExceeded", isHumidityExceeded);
        eventDataMap.put("alertLevel", alertLevel);
        if (thresholds != null) {
            eventDataMap.put("tempMin", tempMin);
            eventDataMap.put("tempMax", tempMax);
            eventDataMap.put("humidityMin", humidityMin);
            eventDataMap.put("humidityMax", humidityMax);
        }

        String eventDataJson = toJson(eventDataMap);

        // 11. Get actor
        User actor = getActor(currentUser);

        // 12. Create and save ChainEvent
        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.STORAGE_CONDITION)
                .eventData(eventDataJson)
                .recordedAt(recordedAt)
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        chainEvent = saveWithChainHash(chainEvent);

        // 13. Publish activity log
        publishActivityLog(currentUser, "Ghi mốc bảo quản cho lô hàng " + shipment.getName(),
                "ChainEvent", chainEvent.getId().toString());

        // 14. Build response
        return StorageConditionResponse.builder()
                .id(chainEvent.getId())
                .eventType(ChainEventType.STORAGE_CONDITION)
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .temperature(request.getTemperature())
                .humidity(request.getHumidity())
                .thresholds(thresholds)
                .isTemperatureExceeded(isTempExceeded)
                .isHumidityExceeded(isHumidityExceeded)
                .alertLevel(alertLevel)
                .recordedAt(recordedAt)
                .recordedBy(actor.getFullName())
                .build();
    }

    /**
     * Validates that the current VT-04 procurement company has a legitimate
     * procurement relationship with the shipment.
     *
     * The procurement relationship is represented by existing PROCUREMENT
     * ChainEvents: at least one PROCUREMENT event for this shipment must have
     * been recorded by a user belonging to the current user's organization.
     *
     * This intentionally does NOT require the procurement company's
     * organization to equal the producer/cooperative organization of the
     * shipment.
     */
    private void validateStorageProcurementRelationship(Shipment shipment, CustomUserDetails currentUser) {
        List<ChainEvent> procurementEvents = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .filter(e -> e.getEventType() == ChainEventType.PROCUREMENT)
                .toList();

        if (procurementEvents.isEmpty()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi nhận điều kiện bảo quản cho lô hàng này. Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.");
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
                    "Bạn không có quyền ghi nhận điều kiện bảo quản cho lô hàng này. Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.");
        }
    }

    /**
     * Kiểm chứng tính toàn vẹn dòng sự kiện của một lô hàng.
     *
     * Tính lại hash của từng sự kiện theo thứ tự recordedAt ASC, so sánh với
     * hash đã lưu. Phát hiện sự kiện đầu tiên bị lệch và ghi ActivityLog
     * VERIFY_CHAIN sau mỗi lần kiểm chứng.
     */
    @Override
    @Transactional(readOnly = true)
    public ChainVerificationResponse verifyChainIntegrity(UUID shipmentId, CustomUserDetails currentUser) {
        // 1. Find shipment
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy lô hàng."));

        // 2. Authorization
        String role = currentUser.getRoleCode();
        if ("VT-01".equals(role) || "VT-05".equals(role)) {
            // Platform admin & industry regulator may verify any shipment
        } else if ("VT-04".equals(role)) {
            // Procurement company: must have a procurement relationship
            validateStorageProcurementRelationship(shipment, currentUser);
        } else {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền kiểm chứng dòng sự kiện của lô này.");
        }

        // 3. Load all events for this shipment, ordered deterministically
        List<ChainEvent> events = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipmentId)
                .stream()
                .sorted(eventHashService.eventOrdering())
                .toList();

        // 4. Empty event chain -> TC-04
        if (events.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Lô hàng chưa có sự kiện nào để kiểm chứng.");
        }

        // 5. Recalculate chain
        LocalDateTime verifiedAt = LocalDateTime.now();
        String previousHash = "";
        boolean verified = true;
        Integer failedIndex = null;
        UUID failedEventId = null;
        String failureReason = null;

        List<EventVerificationItem> verificationItems = new ArrayList<>();

        for (int i = 0; i < events.size(); i++) {
            ChainEvent event = events.get(i);
            int index = i + 1;

            String expectedHash = eventHashService.calculateHash(event, previousHash);
            String storedHash = event.getHash();
            String storedPrevious = event.getPreviousHash();

            boolean isPreviousValid = (i == 0)
                    ? (storedPrevious == null || storedPrevious.isEmpty())
                    : previousHash.equals(storedPrevious != null ? storedPrevious : "");

            boolean isHashValid = expectedHash.equals(storedHash != null ? storedHash : "");

            boolean isValid = isPreviousValid && isHashValid;

            EventVerificationItem.EventVerificationItemBuilder itemBuilder = EventVerificationItem.builder()
                    .index(index)
                    .eventId(event.getId())
                    .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                    .recordedAt(event.getRecordedAt())
                    .hash(storedHash)
                    .previousHash(event.getPreviousHash())
                    .isValid(isValid);

            if (!isValid && verified) {
                // First invalid event
                verified = false;
                failedIndex = index;
                failedEventId = event.getId();
                if (!isPreviousValid) {
                    failureReason = "Previous hash mismatch: expected " + previousHash + ", got "
                            + (storedPrevious != null ? storedPrevious : "") + ".";
                } else if (!isHashValid) {
                    failureReason = "Hash mismatch: expected " + expectedHash + ", got "
                            + (storedHash != null ? storedHash : "") + ".";
                }
                itemBuilder.expectedHash(expectedHash);
            } else if (!isValid) {
                // Subsequent invalid events depend on the first broken link
                itemBuilder.expectedHash(expectedHash);
            }

            verificationItems.add(itemBuilder.build());

            // Update link for the next event
            previousHash = expectedHash;
        }

        // 6. Build response
        ChainVerificationResponse response = ChainVerificationResponse.builder()
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .totalEvents(events.size())
                .isIntegrityVerified(verified)
                .verificationStatus(verified ? "INTACT" : "BROKEN")
                .failedEventIndex(failedIndex)
                .failedEventId(failedEventId)
                .failureReason(failureReason)
                .verifiedAt(verifiedAt)
                .hashAlgorithm(EventHashService.HASH_ALGORITHM)
                .events(verificationItems)
                .build();

        // 7. Record verification in ActivityLog (TC-03)
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("VERIFY_CHAIN")
                .description("Kiểm chứng dòng sự kiện lô hàng: " + shipment.getName())
                .entityType("SHIPMENT")
                .entityId(shipment.getId().toString())
                .ipAddress(IpUtils.getClientIp())
                .timestamp(verifiedAt)
                .build());

        return response;
    }

    /**
     * Lưu ChainEvent và tự động tính chuỗi băm liên kết trước khi persist.
     *
     * Tính toán hash của sự kiện mới dựa trên hash sự kiện liền trước trong
     * cùng Shipment. Sự kiện đầu tiên dùng previousHash = "".
     */
    @Override
    @Transactional
    public ChainEvent saveWithChainHash(ChainEvent event) {
        if (event.getShipment() == null) {
            // Không gắn shipment (VD: HARVEST/PACKAGING chưa gắn lô hàng) -> không thể tính
            // chuỗi.
            return chainEventRepository.save(event);
        }

        // Tìm sự kiện được ghi gần nhất (createdAt) của cùng shipment.
        // Thứ tự chuỗi mật mã dựa trên createdAt (máy chủ sinh ra), KHÔNG dùng
        // recordedAt (client cung cấp) để tránh phân nhánh chuỗi không nhất quán.
        Optional<ChainEvent> lastEvent = chainEventRepository
                .findTopByShipmentIdOrderByCreatedAtDesc(event.getShipment().getId());

        String previousHash = "";
        if (lastEvent.isPresent()) {
            previousHash = lastEvent.get().getHash() != null ? lastEvent.get().getHash() : "";
        }

        event.setPreviousHash(previousHash.isEmpty() ? null : previousHash);
        event.setHash(eventHashService.calculateHash(event, previousHash));

        return chainEventRepository.save(event);
    }
}