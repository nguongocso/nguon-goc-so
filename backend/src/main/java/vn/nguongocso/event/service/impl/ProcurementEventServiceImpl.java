package vn.nguongocso.event.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.dto.request.RecordProcurementEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.ProcurementEventService;
import vn.nguongocso.event.service.resolver.ProcurementShipmentResolver;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.entity.Shipment;

/** Implementation dịch vụ ghi nhận sự kiện thu mua cho lô hàng. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcurementEventServiceImpl implements ProcurementEventService {
    private final ProcurementShipmentResolver procurementShipmentResolver;
    private final ChainEventService chainEventService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public ChainEventResponse recordProcurementEvent(
            RecordProcurementEventRequest request,
            CustomUserDetails currentUser) {
        validateRole(currentUser);

        Shipment shipment = procurementShipmentResolver.resolveAndValidateShipment(
                request.getShipmentId(), currentUser.getOrganizationId());

        procurementShipmentResolver.validateShipmentStatus(shipment, currentUser);

        Point locationPoint = buildLocationPoint(request.getLatitude(), request.getLongitude());

        Map<String, Object> eventDataMap = buildEventDataMap(shipment, request);
        String eventDataJson = serializeEventData(eventDataMap);

        User actor = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người ghi nhận."));

        ChainEvent chainEvent = saveChainEvent(
                shipment, eventDataJson, actor, locationPoint, currentUser.getOrganizationId());

        publishActivityLog(shipment, chainEvent, currentUser);

        return buildResponse(chainEvent, shipment, eventDataMap, request, actor);
    }

    private void validateRole(CustomUserDetails currentUser) {
        if (!"VT-04".equals(currentUser.getRoleCode())) {
            throw new BusinessException("Chỉ Doanh nghiệp thu mua mới được ghi sự kiện này");
        }
    }

    private Point buildLocationPoint(Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            return geometryFactory.createPoint(new Coordinate(longitude, latitude));
        }
        return null;
    }

    private Map<String, Object> buildEventDataMap(Shipment shipment, RecordProcurementEventRequest request) {
        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("shipmentId", shipment.getId().toString());
        eventDataMap.put("shipmentName", shipment.getName());
        eventDataMap.put("receivedQuantity", request.getReceivedQuantity());
        eventDataMap.put("notes", request.getNotes());
        return eventDataMap;
    }

    private String serializeEventData(Map<String, Object> eventDataMap) {
        try {
            return objectMapper.writeValueAsString(eventDataMap);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Lỗi chuyển đổi dữ liệu sang chuỗi JSON.");
        }
    }

    private ChainEvent saveChainEvent(
            Shipment shipment, String eventDataJson, User actor, Point locationPoint, UUID orgId) {
        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.PROCUREMENT)
                .eventData(eventDataJson)
                .location(locationPoint)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .recordedOrganizationId(orgId)
                .isCorrection(false)
                .build();

        return chainEventService.saveWithChainHash(chainEvent);
    }

    private void publishActivityLog(Shipment shipment, ChainEvent chainEvent, CustomUserDetails currentUser) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("CREATE")
                .description("Ghi sự kiện thu hoạch cho lô hàng " + shipment.getName())
                .entityType("ChainEvent")
                .entityId(chainEvent.getId().toString())
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private ChainEventResponse buildResponse(
            ChainEvent chainEvent, Shipment shipment, Map<String, Object> eventDataMap,
            RecordProcurementEventRequest request, User actor) {
        return ChainEventResponse.builder()
                .id(chainEvent.getId())
                .shipmentId(shipment.getId())
                .eventType(chainEvent.getEventType())
                .eventData(eventDataMap)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .recordedAt(chainEvent.getRecordedAt())
                .recordedByName(actor.getFullName())
                .createdAt(chainEvent.getCreatedAt())
                .build();
    }
}
