package vn.nguongocso.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import vn.nguongocso.event.dto.response.JourneyResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JourneyServiceTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ChainEventRepository chainEventRepository;

    @InjectMocks
    private JourneyService journeyService;

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    void getJourney_shouldReturnPoints_whenShipmentExists() {
        // Given
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng 1");

        Point point1 = geometryFactory.createPoint(new Coordinate(106.629, 10.823));
        Point point2 = geometryFactory.createPoint(new Coordinate(106.650, 10.850));

        ChainEvent event1 = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .location(point1)
                .recordedAt(LocalDateTime.now())
                .build();

        ChainEvent event2 = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.TRANSPORT)
                .location(point2)
                .recordedAt(LocalDateTime.now().plusHours(2))
                .build();

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findJourneyPointsByShipmentId(shipmentId))
                .thenReturn(List.of(event1, event2));

        // When
        JourneyResponse response = journeyService.getJourney(shipmentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getShipmentId()).isEqualTo(shipmentId);
        assertThat(response.getTotalEvents()).isEqualTo(2);
        assertThat(response.getPoints()).hasSize(2);
        assertThat(response.getPoints().get(0).getOrder()).isEqualTo(1);
        assertThat(response.getPoints().get(0).getEventType()).isEqualTo("HARVEST");
        assertThat(response.getPoints().get(0).getLatitude()).isEqualTo(10.823);
    }

    @Test
    void getJourney_shouldReturnEmpty_whenNoEvents() {
        // Given
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng 1");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findJourneyPointsByShipmentId(shipmentId))
                .thenReturn(List.of());

        // When
        JourneyResponse response = journeyService.getJourney(shipmentId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalEvents()).isEqualTo(0);
        assertThat(response.getPoints()).isEmpty();
    }

    @Test
    void getJourney_shouldThrow_whenShipmentNotFound() {
        UUID shipmentId = UUID.randomUUID();
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> journeyService.getJourney(shipmentId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy lô hàng");
    }
}