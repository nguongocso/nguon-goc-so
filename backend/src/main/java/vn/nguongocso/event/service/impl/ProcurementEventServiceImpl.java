package vn.nguongocso.event.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.ProcurementEventService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
/** Ghi nhận sự kiện thu mua cho lô hàng. */
public class ProcurementEventServiceImpl implements ProcurementEventService {

    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final ChainEventService chainEventService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final EventValidationService eventValidationService;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final ApplicationEventPublisher eventPublisher;

    /** Ghi nhận sự kiện thu mua. */
    @Override
    @Transactional
    public ChainEventResponse recordProcurementEvent(RecordProcurementEventRequest request, CustomUserDetails currentUser) {

        // 1. Kiểm tra quyền: Chỉ VT-04 mới được ghi sự kiện thu mua
        String role = currentUser.getRoleCode();
        if (!"VT-04".equals(role)) {
            throw new BusinessException("Chỉ Doanh nghiệp thu mua mới được ghi sự kiện này");
        }

        // 2. Tìm shipment
        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng."));


        try {
            // 4. Kiểm tra trạng thái lô: Không được thu hồi (QTN-05)
            if (shipment.getStatus() == ShipmentStatus.RECALLED) {
                throw new BusinessException("Lô hàng đã bị thu hồi, không thể ghi sự kiện.");
            }

            if (shipment.getStatus() != ShipmentStatus.ACTIVATED) {
                throw new BusinessException("Lô hàng chưa được kích hoạt, không thể ghi sự kiện thu mua.");
            }
        } catch (BusinessException e) {
            eventValidationService.logFailedAttempt(shipment.getId(), shipment.getName(), ChainEventType.PROCUREMENT, e.getMessage(), currentUser);
            throw e;
        }

        // 5. Tạo điểm vị trí (nếu có)
        Point locationPoint = null;
        if (request.getLatitude() != null && request.getLongitude() != null) {
            locationPoint = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
            );
        }

        // 6. Tạo dữ liệu JSON cho event_data
        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("shipmentId", shipment.getId().toString());
        eventDataMap.put("shipmentName", shipment.getName());
        eventDataMap.put("receivedQuantity", request.getReceivedQuantity());
        eventDataMap.put("notes", request.getNotes());

        String eventDataJson;
        try {
            eventDataJson = objectMapper.writeValueAsString(eventDataMap);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Lỗi chuyển đổi dữ liệu sang chuỗi JSON.");
        }

        // 7. Lấy thông tin người ghi
        User actor = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người ghi nhận."));

        // 8. Tạo và lưu ChainEvent
        ChainEvent chainEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.PROCUREMENT)
                .eventData(eventDataJson)
                .location(locationPoint)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .isCorrection(false)
                .build();

        chainEvent = chainEventService.saveWithChainHash(chainEvent);

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
                .build()
        );

        // 9. Trả về response
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
