package vn.nguongocso.event.service.processor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Processor xử lý sự kiện vận chuyển hàng hóa cho hợp tác xã.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoopTransportEventProcessor {

    private final TraceCodeRepository traceCodeRepository;
    private final UserRepository userRepository;
    private final ChainEventHashRecorder chainEventHashRecorder;
    private final EventValidationService eventValidationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Ghi nhận sự kiện vận chuyển cho lô hàng.
     *
     * @param request     thông tin sự kiện vận chuyển
     * @param currentUser thông tin người dùng thực hiện
     * @return thông tin sự kiện chuỗi cung ứng đã lưu
     */
    public ChainEventResponse recordTransportEvent(
            RecordTransportEventRequest request, CustomUserDetails currentUser) {
        if (!"VT-03".equals(currentUser.getRoleCode())) {
            throw new BusinessException("Bạn không có quyền ghi sự kiện vận chuyển.");
        }

        TraceCode traceCode = traceCodeRepository.findByCodeValue(request.getCodeValue())
                .orElseThrow(() -> new BusinessException("Mã lô hàng không tồn tại."));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new BusinessException("Mã truy xuất chưa được gắn với lô hàng.");
        }

        validateTransportShipment(shipment, currentUser);

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("fromLocation", request.getFromLocation());
        eventDataMap.put("toLocation", request.getToLocation());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            eventDataMap.put("images", request.getImages());
        }
        eventDataMap.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "WEB");

        User actor = getActor(currentUser);
        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.TRANSPORT)
                .eventData(toJson(eventDataMap))
                .recordedAt(request.getTransportTime())
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        chainEvent = chainEventHashRecorder.saveWithChainHash(chainEvent);

        publishActivityLog(currentUser, "Ghi sự kiện vận chuyển cho lô hàng " + shipment.getName(), chainEvent.getId().toString());
        return buildResponse(chainEvent, eventDataMap, null, null, actor);
    }

    private void validateTransportShipment(Shipment shipment, CustomUserDetails currentUser) {
        try {
            validateOrganization(shipment, currentUser);
            if (shipment.getStatus() == ShipmentStatus.RECALLED || shipment.getStatus() == ShipmentStatus.RECALLING) {
                throw new BusinessException("Lô hàng đã bị thu hồi, không thể ghi sự kiện vận chuyển.");
            }
            if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
                throw new BusinessException("Lô hàng chưa được kích hoạt, không thể ghi sự kiện vận chuyển.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(
                    shipment.getId(), shipment.getName(), ChainEventType.TRANSPORT, e.getMessage(), currentUser);
            throw e;
        }
    }

    private void validateOrganization(Shipment shipment, CustomUserDetails currentUser) {
        if ("VT-01".equals(currentUser.getRoleCode())) {
            return;
        }
        if (shipment.getOrganization() == null
                || !shipment.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
            throw new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN, "Bạn không có quyền ghi sự kiện cho lô hàng của tổ chức này.");
        }
    }

    private User getActor(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            return null;
        }
        return userRepository.findById(currentUser.getUserId()).orElse(null);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Lỗi chuyển đổi dữ liệu sự kiện sang JSON.");
        }
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
}
