package vn.nguongocso.event.service.processor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
import vn.nguongocso.event.dto.request.StorageConditionRequest;
import vn.nguongocso.event.dto.response.StorageConditionResponse;
import vn.nguongocso.event.dto.response.ThresholdInfo;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/** Processor chuyên trách xử lý ghi nhận mốc điều kiện bảo quản (nhiệt độ, độ ẩm) khi vận chuyển. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageConditionProcessor {

    private final TraceCodeRepository traceCodeRepository;
    private final ChainEventRepository chainEventRepository;
    private final ChainEventHashRecorder chainEventHashRecorder;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** Ghi nhận mốc điều kiện bảo quản khi vận chuyển. */
    public StorageConditionResponse recordStorageCondition(StorageConditionRequest request,
            CustomUserDetails currentUser) {
        String role = currentUser.getRoleCode();
        if (!"VT-03".equals(role) && !"VT-04".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi nhận điều kiện bảo quản cho lô hàng này.");
        }

        TraceCode traceCode = traceCodeRepository.findByCodeValue(request.getCodeValue())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Mã lô hàng không tồn tại."));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new BusinessException("Mã truy xuất chưa được gắn với lô hàng.");
        }

        if ("VT-04".equals(role)) {
            validateStorageProcurementRelationship(shipment, currentUser);
        } else if (!shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không thuộc tổ chức quản lý của lô hàng này.");
        }

        if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
            throw new BusinessException(
                    "Lô hàng chưa được kích hoạt hoặc đang/đã bị thu hồi, không thể ghi nhận mốc bảo quản.");
        }
        if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
            throw new BusinessException(
                    "Lô hàng chưa được kích hoạt hoặc đã bị thu hồi, không thể ghi nhận mốc bảo quản.");
        }

        boolean hasTransportEvent = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .anyMatch(e -> e.getEventType() == ChainEventType.TRANSPORT);
        if (!hasTransportEvent) {
            throw new BusinessException("Lô hàng phải có sự kiện vận chuyển trước khi ghi nhận điều kiện bảo quản.");
        }

        ProductCategory productCategory = null;
        ProductionLot productionLot = shipment.getProductionLot();
        if (productionLot != null) {
            productCategory = productionLot.getProductCategory();
        }

        Double tempMin = productCategory != null ? productCategory.getTempMin() : null;
        Double tempMax = productCategory != null ? productCategory.getTempMax() : null;
        Double humidityMin = productCategory != null ? productCategory.getHumidityMin() : null;
        Double humidityMax = productCategory != null ? productCategory.getHumidityMax() : null;

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

        LocalDateTime now = clock != null ? LocalDateTime.now(clock) : LocalDateTime.now();
        LocalDateTime recordedAt = request.getRecordedAt() != null
                ? request.getRecordedAt()
                : now;

        ThresholdInfo thresholds = null;
        if (tempMin != null || tempMax != null || humidityMin != null || humidityMax != null) {
            thresholds = ThresholdInfo.builder()
                    .tempMin(tempMin)
                    .tempMax(tempMax)
                    .humidityMin(humidityMin)
                    .humidityMax(humidityMax)
                    .build();
        }

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
        User actor = getActor(currentUser);

        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.STORAGE_CONDITION)
                .eventData(eventDataJson)
                .recordedAt(recordedAt)
                .recordedBy(actor)
                .recordedOrganizationId(currentUser.getOrganizationId())
                .isCorrection(false)
                .build();

        chainEvent = chainEventHashRecorder.saveWithChainHash(chainEvent);

        publishActivityLog(currentUser,
                "Ghi nhận mốc điều kiện bảo quản cho lô hàng " + shipment.getName()
                        + " (Nhiệt độ: " + request.getTemperature() + "°C, Độ ẩm: " + request.getHumidity() + "%)",
                "ChainEvent", chainEvent.getId().toString());

        return StorageConditionResponse.builder()
                .id(chainEvent.getId())
                .eventType(chainEvent.getEventType())
                .shipmentId(shipment.getId())
                .shipmentName(shipment.getName())
                .temperature(request.getTemperature())
                .humidity(request.getHumidity())
                .recordedAt(chainEvent.getRecordedAt())
                .recordedBy(actor.getFullName())
                .thresholds(thresholds)
                .isTemperatureExceeded(isTempExceeded)
                .isHumidityExceeded(isHumidityExceeded)
                .alertLevel(alertLevel)
                .build();
    }

    private void validateStorageProcurementRelationship(Shipment shipment, CustomUserDetails currentUser) {
        UUID currentOrgId = currentUser.getOrganizationId();
        if (currentOrgId == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không thuộc tổ chức nào, không thể thực hiện thao tác này.");
        }

        boolean hasProcurementEvent = chainEventRepository
                .findByShipmentIdOrderByRecordedAtAsc(shipment.getId())
                .stream()
                .filter(e -> e.getEventType() == ChainEventType.PROCUREMENT)
                .anyMatch(e -> {
                    if (e.getRecordedBy() == null) {
                        return false;
                    }
                    return organizationUserRepository
                            .findByOrganization_OrganizationIdAndUser_UserId(
                                    currentOrgId, e.getRecordedBy().getUserId())
                            .isPresent();
                });

        if (!hasProcurementEvent) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền ghi nhận điều kiện bảo quản cho lô hàng này. "
                            + "Chỉ doanh nghiệp đã thu mua lô hàng mới được thực hiện.");
        }
    }

    private User getActor(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng."));
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
}
