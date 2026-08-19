package vn.nguongocso.event.service;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.event.dto.response.JourneyPointResponse;
import vn.nguongocso.event.dto.response.JourneyResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
/** Xây dựng dữ liệu hành trình của lô hàng. */
public class JourneyService {

        private final ShipmentRepository shipmentRepository;
        private final ChainEventRepository chainEventRepository;

        // Map event type to display name
        private static final Map<String, String> EVENT_DISPLAY_NAMES = Map.of(
                        "HARVEST", "Thu hoạch",
                        "TRANSPORT", "Vận chuyển",
                        "PACKAGING", "Đóng gói",
                        "PROCUREMENT", "Thu mua");

        /** Lấy hành trình chi tiết của một lô hàng. */
        @Transactional(readOnly = true)
        public JourneyResponse getJourney(UUID shipmentId) {

                // 1. Kiểm tra shipment tồn tại
                Shipment shipment = shipmentRepository.findById(shipmentId)
                                .orElseThrow(() -> new BusinessException("Không tìm thấy lô hàng"));

                // 2. Lấy danh sách sự kiện có tọa độ
                List<ChainEvent> events = chainEventRepository.findJourneyPointsByShipmentId(shipmentId);

                // 3. Chuyển đổi sang DTO
                List<JourneyPointResponse> points = IntStream.range(0, events.size())
                                .mapToObj(i -> toJourneyPoint(events.get(i), i + 1))
                                .collect(Collectors.toList());

                // 4. Trả về response
                return JourneyResponse.builder()
                                .shipmentId(shipment.getId())
                                .shipmentName(shipment.getName())
                                .totalEvents(points.size())
                                .points(points)
                                .build();
        }

        /** Chuyển một sự kiện thành điểm hành trình. */
        private JourneyPointResponse toJourneyPoint(ChainEvent event, int order) {

                Point location = event.getLocation();
                return JourneyPointResponse.builder()
                                .eventId(event.getId())
                                .eventType(event.getEventType().name())
                                .eventName(EVENT_DISPLAY_NAMES.getOrDefault(
                                                event.getEventType().name(),
                                                event.getEventType().name()))
                                .latitude(location != null ? location.getY() : null)
                                .longitude(location != null ? location.getX() : null)
                                .recordedAt(event.getRecordedAt())
                                .description(extractDescription(event))
                                .order(order)
                                .build();
        }

        /** Tạo mô tả ngắn cho sự kiện. */
        private String extractDescription(ChainEvent event) {
                return event.getEventType().name() + " tại " + event.getRecordedAt().toLocalDate();
        }
}
