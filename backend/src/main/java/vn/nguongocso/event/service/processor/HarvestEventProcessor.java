package vn.nguongocso.event.service.processor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.request.RecordMobileEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.farm.service.HarvestEligibilityService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;

/** Processor chuyên trách xử lý nghiệp vụ ghi nhận sự kiện thu hoạch cho lô sản xuất. */
@Slf4j
@Component
@RequiredArgsConstructor
public class HarvestEventProcessor {

    private final ProductionLotRepository productionLotRepository;
    private final ChainEventRepository chainEventRepository;
    private final UserRepository userRepository;
    private final HarvestEligibilityService harvestEligibilityService;
    private final EventValidationService eventValidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Ghi nhận sự kiện thu hoạch cho lô sản xuất.
     *
     * @param request     yêu cầu ghi nhận sự kiện thu hoạch
     * @param currentUser người dùng hiện tại
     * @return phản hồi sự kiện chuỗi cung ứng
     */
    public ChainEventResponse recordHarvestEvent(RecordHarvestEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        HarvestEligibilityResponse eligibility = validateHarvestEligibility(request, lot, currentUser);
        EarlyHarvestResult earlyHarvest = resolveEarlyHarvest(request, lot, eligibility, currentUser);

        Map<String, Object> eventDataMap = updateLotAndBuildEventData(request, lot, eligibility, earlyHarvest);
        User actor = getActor(currentUser);
        ChainEvent chainEvent = saveHarvestChainEvent(request, toJson(eventDataMap), actor);

        publishHarvestActivityLog(lot, eligibility, earlyHarvest, chainEvent.getId(), currentUser);
        return buildResponse(chainEvent, eventDataMap, request.getLatitude(), request.getLongitude(), actor);
    }

    private HarvestEligibilityResponse validateHarvestEligibility(
            RecordHarvestEventRequest request, ProductionLot lot, CustomUserDetails currentUser) {
        try {
            validateOrganization(lot, currentUser);
            if (lot.getStatus() == ProductionLotStatus.CANCELLED) {
                throw new BusinessException("Lô sản xuất đã bị hủy, không thể ghi sự kiện.");
            }
            if (lot.getStatus() != ProductionLotStatus.APPROVED) {
                throw new BusinessException("Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.");
            }

            LocalDate today = clock != null ? LocalDate.now(clock) : LocalDate.now();
            if (request.getHarvestDate().isAfter(today)) {
                throw new BusinessException("Ngày thu hoạch không được là ngày ở tương lai.");
            }
            if (lot.getPlantingDate() != null && request.getHarvestDate().isBefore(lot.getPlantingDate())) {
                throw new BusinessException("Ngày thu hoạch phải sau hoặc bằng ngày gieo trồng của lô.");
            }

            return harvestEligibilityService.calculateHarvestEligibility(lot.getId());
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(request.getProductionLotId(), lot.getName(),
                    ChainEventType.HARVEST, e.getMessage(), currentUser);
            throw e;
        }
    }

    private EarlyHarvestResult resolveEarlyHarvest(
            RecordHarvestEventRequest request, ProductionLot lot,
            HarvestEligibilityResponse eligibility, CustomUserDetails currentUser) {
        if (!eligibility.isDetermined() || eligibility.getEligibleHarvestDate() == null
                || !request.getHarvestDate().isBefore(eligibility.getEligibleHarvestDate())) {
            return new EarlyHarvestResult(false, null);
        }

        String roleCode = currentUser.getRoleCode();
        if ("VT-02".equals(roleCode)) {
            if (request.getEarlyHarvestReason() == null || request.getEarlyHarvestReason().trim().isEmpty()) {
                String errorMsg = "Thu hoạch trước ngày đủ điều kiện cách ly ("
                        + eligibility.getEligibleHarvestDate()
                        + "). Quản lý cần nhập lý do ghi đè bắt buộc.";
                eventValidationService.logFailedAttempt(request.getProductionLotId(), lot.getName(),
                        ChainEventType.HARVEST, errorMsg, currentUser);
                throw new BusinessException(errorMsg);
            }
            return new EarlyHarvestResult(true, request.getEarlyHarvestReason().trim());
        }

        String errorMsg = "VT-03".equals(roleCode)
                ? "Lô sản xuất chưa hết thời gian cách ly (ngày đủ điều kiện: "
                        + eligibility.getEligibleHarvestDate()
                        + "). Người ghi sự kiện không có quyền ghi đè thu hoạch sớm."
                : "Lô sản xuất chưa hết thời gian cách ly (ngày đủ điều kiện: "
                        + eligibility.getEligibleHarvestDate()
                        + "). Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch sớm.";
        eventValidationService.logFailedAttempt(request.getProductionLotId(), lot.getName(),
                ChainEventType.HARVEST, errorMsg, currentUser);
        throw new BusinessException(errorMsg);
    }

    private Map<String, Object> updateLotAndBuildEventData(
            RecordHarvestEventRequest request, ProductionLot lot,
            HarvestEligibilityResponse eligibility, EarlyHarvestResult earlyHarvest) {
        lot.setStatus(ProductionLotStatus.HARVESTED);
        lot.setHarvestDate(request.getHarvestDate());
        lot.setActualQuantity(request.getQuantity());
        productionLotRepository.save(lot);

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", lot.getId().toString());
        eventDataMap.put("productionLotName", lot.getName());
        eventDataMap.put("harvestDate", request.getHarvestDate().toString());
        eventDataMap.put("quantity", request.getQuantity());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            eventDataMap.put("images", request.getImages());
        }
        eventDataMap.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "WEB");

        if (earlyHarvest.isEarlyHarvest()) {
            eventDataMap.put("earlyHarvest", true);
            eventDataMap.put("earlyHarvestReason", earlyHarvest.reason());
            eventDataMap.put("eligibleHarvestDate", eligibility.getEligibleHarvestDate().toString());
        } else {
            eventDataMap.put("earlyHarvest", false);
            eventDataMap.put("eligibleHarvestDate",
                    eligibility.isDetermined() && eligibility.getEligibleHarvestDate() != null
                            ? eligibility.getEligibleHarvestDate().toString()
                            : null);
        }

        if (!eligibility.isDetermined() && eligibility.getUnmatchedMaterials() != null
                && !eligibility.getUnmatchedMaterials().isEmpty()) {
            eventDataMap.put("unmatchedMaterials", eligibility.getUnmatchedMaterials());
        }
        return eventDataMap;
    }

    private ChainEvent saveHarvestChainEvent(RecordHarvestEventRequest request, String eventDataJson, User actor) {
        Point locationPoint = buildPoint(request.getLatitude(), request.getLongitude());
        ChainEvent chainEvent = ChainEvent.builder()
                .eventType(ChainEventType.HARVEST)
                .eventData(eventDataJson)
                .location(locationPoint)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .isCorrection(false)
                .build();
        return chainEventRepository.save(chainEvent);
    }

    private void publishHarvestActivityLog(
            ProductionLot lot, HarvestEligibilityResponse eligibility,
            EarlyHarvestResult earlyHarvest, UUID eventId, CustomUserDetails currentUser) {
        String activityDesc = earlyHarvest.isEarlyHarvest()
                ? String.format("Ghi nhận thu hoạch sớm cho lô %s (Đủ điều kiện: %s) - Lý do: %s",
                        lot.getName(), eligibility.getEligibleHarvestDate(), earlyHarvest.reason())
                : "Ghi sự kiện thu hoạch cho lô " + lot.getName();
        publishActivityLog(currentUser, activityDesc, "ChainEvent", eventId.toString());
    }

    private record EarlyHarvestResult(boolean isEarlyHarvest, String reason) {}

    /** Ghi nhận sự kiện thu hoạch từ thiết bị di động. */
    public ChainEventResponse recordMobileHarvestEvent(
            RecordMobileEventRequest request, CustomUserDetails currentUser) {
        validateEventPermission(currentUser);

        ProductionLot lot = productionLotRepository.findById(request.getProductionLotId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất."));

        validateOrganization(lot, currentUser);

        if (request.getRecordedAt() != null && request.getRecordedAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Thời điểm ghi nhận không được là thời gian ở tương lai.");
        }

        RecordHarvestEventRequest harvestRequest = buildHarvestRequestFromMobile(request);
        ChainEventResponse delegateResponse = recordHarvestEvent(harvestRequest, currentUser);

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

    private RecordHarvestEventRequest buildHarvestRequestFromMobile(RecordMobileEventRequest request) {
        Map<String, Object> data = request.getEventData() != null ? request.getEventData() : Map.of();
        Object quantityObj = data.get("quantity");
        Object harvestDateStrObj = data.get("harvestDate");

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

        Object earlyHarvestReasonObj = data.get("earlyHarvestReason");
        if (earlyHarvestReasonObj != null && !earlyHarvestReasonObj.toString().trim().isEmpty()) {
            harvestRequest.setEarlyHarvestReason(earlyHarvestReasonObj.toString().trim());
        }
        return harvestRequest;
    }

    private void validateEventPermission(CustomUserDetails currentUser) {
        String role = currentUser.getRoleCode();
        if (!"VT-01".equals(role) && !"VT-02".equals(role) && !"VT-03".equals(role)) {
            throw new BusinessException("Chỉ thành viên được cấp quyền trong tổ chức mới được ghi sự kiện.");
        }
    }

    private void validateOrganization(ProductionLot lot, CustomUserDetails currentUser) {
        if ("VT-01".equals(currentUser.getRoleCode())) {
            return;
        }
        if (!lot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Bạn không thuộc tổ chức quản lý của lô sản xuất này.");
        }
    }

    private User getActor(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng."));
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
            log.error("Lỗi khi chuyển đổi dữ liệu sự kiện sang JSON", e);
            throw new BusinessException("Lỗi xử lý dữ liệu sự kiện.");
        }
    }

    private void publishActivityLog(CustomUserDetails currentUser, String description,
            String entityType, String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("CHAIN_EVENT_CREATE")
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private ChainEventResponse buildResponse(ChainEvent chainEvent, Map<String, Object> eventDataMap,
            Double latitude, Double longitude, User actor) {
        return ChainEventResponse.builder()
                .id(chainEvent.getId())
                .eventType(chainEvent.getEventType())
                .eventData(eventDataMap)
                .latitude(latitude)
                .longitude(longitude)
                .recordedAt(chainEvent.getRecordedAt())
                .recordedByName(actor.getFullName())
                .createdAt(chainEvent.getCreatedAt())
                .build();
    }
}
