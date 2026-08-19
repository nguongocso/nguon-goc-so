package vn.nguongocso.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.trace.entity.Shipment;

class EventHashServiceTest {

    private EventHashService service;
    private Shipment shipment;
    private User user;
    private ChainEvent event;

    @BeforeEach
    void setUp() {
        service = new EventHashService(new ObjectMapper());
        shipment = new Shipment();
        shipment.setId(UUID.fromString("9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f"));

        user = new User();
        user.setUserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        event = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.TRANSPORT)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 10, 30, 0))
                .recordedBy(user)
                .eventData("{\"toLocation\":\"HN\",\"fromLocation\":\"TN\"}")
                .build();
    }

    @Test
    void hashIs64CharHex() {
        String hash = service.calculateHash(event, "");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void sameEventContentProducesSameHash() {
        ChainEvent copy = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.TRANSPORT)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 10, 30, 0))
                .recordedBy(user)
                .eventData("{\"fromLocation\":\"TN\",\"toLocation\":\"HN\"}")
                .build();

        String h1 = service.calculateHash(event, "");
        String h2 = service.calculateHash(copy, "");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void changingEventDataChangesHash() {
        String original = service.calculateHash(event, "");
        ChainEvent modified = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.TRANSPORT)
                .recordedAt(event.getRecordedAt())
                .recordedBy(user)
                .eventData("{\"toLocation\":\"HCM\",\"fromLocation\":\"TN\"}")
                .build();
        assertThat(service.calculateHash(modified, "")).isNotEqualTo(original);
    }

    @Test
    void changingPreviousHashChangesHash() {
        String h1 = service.calculateHash(event, "");
        String h2 = service.calculateHash(event, "previous-hash-value");
        assertThat(h2).isNotEqualTo(h1);
    }

    @Test
    void canonicalizeEventDataSortsKeysDeterministically() {
        String canonical = service.canonicalizeEventData("{\"b\":1,\"a\":2}");
        assertThat(canonical).contains("\"a\":2");
        assertThat(canonical.indexOf("\"a\"")).isLessThan(canonical.indexOf("\"b\""));
    }
}