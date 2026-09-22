package vn.nguongocso.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.impl.EventHashServiceImpl;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Kiểm thử đơn vị và bộ test bất biến (Golden Master) cho EventHashService (NCL-08-CN-006 / QTN-19).
 * Đảm bảo 100% tính toàn vẹn và tính tất định của thuật toán băm chuỗi sự kiện SHA-256.
 */
class EventHashServiceTest {

    private static final String FROZEN_GENESIS_HASH =
            "04cee5f0c16a0f83ec2b596e773a46c3ee408683a92d90289c54723735a32742";
    private static final String FROZEN_EVENT2_HASH =
            "38b1c8f4408bb9933392311a4388dd4bf6923798425fe1bae9127a6dd20f2ce9";
    private static final String FROZEN_CORRECTION_HASH =
            "99bf722ab238842a548643a10a45f7e3143c388b7f4e396460ccc3e0a4d1ed94";

    private EventHashService service;
    private Shipment shipment;
    private User user;
    private ChainEvent event;

    @BeforeEach
    void setUp() {
        service = new EventHashServiceImpl(new ObjectMapper());

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
    void shouldProduce64CharHexHashWhenCalculatingHash() {
        String hash = service.calculateHash(event, "");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void shouldProduceSameHashWhenEventContentIsIdentical() {
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
    void shouldProduceDifferentHashWhenEventDataChanges() {
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
    void shouldProduceDifferentHashWhenPreviousHashChanges() {
        String h1 = service.calculateHash(event, "");
        String h2 = service.calculateHash(event, "previous-hash-value");
        assertThat(h2).isNotEqualTo(h1);
    }

    @Test
    void shouldSortKeysDeterministicallyWhenCanonicalizingEventData() {
        String canonical = service.canonicalizeEventData("{\"b\":1,\"a\":2}");
        assertThat(canonical).contains("\"a\":2");
        assertThat(canonical.indexOf("\"a\"")).isLessThan(canonical.indexOf("\"b\""));
    }

    @Test
    @DisplayName("Genesis event với previousHash rỗng phải khớp 100% hash đóng băng từ baseline")
    void shouldMatchFrozenBaselineHashWhenGenesisEventWithEmptyPreviousHash() {
        ChainEvent genesisEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.TRANSPORT)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 10, 30, 0))
                .recordedBy(user)
                .eventData("{\"toLocation\":\"HN\",\"fromLocation\":\"TN\"}")
                .build();

        String hash = service.calculateHash(genesisEvent, "");
        assertThat(hash).isEqualTo(FROZEN_GENESIS_HASH);
    }

    @Test
    @DisplayName("Sự kiện thứ hai liên kết chuỗi với previousHash phải khớp 100% hash đóng băng từ baseline")
    void shouldMatchFrozenBaselineHashWhenSecondEventChained() {
        ChainEvent secondEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.STORAGE_CONDITION)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 12, 0, 0))
                .recordedBy(user)
                .eventData("{\"temperature\":25.5,\"humidity\":70}")
                .build();

        String hash = service.calculateHash(secondEvent, FROZEN_GENESIS_HASH);
        assertThat(hash).isEqualTo(FROZEN_EVENT2_HASH);
    }

    @Test
    @DisplayName("Sự kiện đính chính (correction) liên kết vào chuỗi phải khớp 100% hash đóng băng từ baseline")
    void shouldMatchFrozenBaselineHashWhenCorrectionEventChained() {
        ChainEvent correctionEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.STORAGE_CONDITION)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 12, 5, 0))
                .recordedBy(user)
                .eventData("{\"humidity\":68,\"temperature\":24.0}")
                .isCorrection(true)
                .build();

        String hash = service.calculateHash(correctionEvent, FROZEN_EVENT2_HASH);
        assertThat(hash).isEqualTo(FROZEN_CORRECTION_HASH);
    }

    @Test
    @DisplayName("Kiểm tra thứ tự sắp xếp sự kiện bất biến theo createdAt ASC và id ASC")
    void shouldSortByCreatedAtAndIdDeterministicallyWhenOrderingEvents() {
        LocalDateTime t1 = LocalDateTime.of(2026, 8, 11, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 8, 11, 11, 0, 0);

        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        ChainEvent e1 = ChainEvent.builder().id(id1).createdAt(t1).build();
        ChainEvent e2 = ChainEvent.builder().id(id2).createdAt(t2).build();
        ChainEvent e3 = ChainEvent.builder().id(id1).createdAt(t2).build();

        var comparator = service.eventOrdering();

        assertThat(comparator.compare(e1, e2)).isNegative();
        assertThat(comparator.compare(e2, e1)).isPositive();
        assertThat(comparator.compare(e3, e2)).isNegative();
    }

    @Test
    @DisplayName("Null previousHash được coi tương đương chuỗi rỗng trong calculateHash")
    void shouldTreatNullPreviousHashAsEmptyStringWhenCalculatingHash() {
        ChainEvent testEvent = ChainEvent.builder()
                .shipment(shipment)
                .eventType(ChainEventType.TRANSPORT)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 10, 30, 0))
                .recordedBy(user)
                .eventData("{\"fromLocation\":\"TN\",\"toLocation\":\"HN\"}")
                .build();

        String hashWithNull = service.calculateHash(testEvent, null);
        String hashWithEmpty = service.calculateHash(testEvent, "");

        assertThat(hashWithNull).isEqualTo(hashWithEmpty);
        assertThat(hashWithNull).isEqualTo(FROZEN_GENESIS_HASH);
    }
}