package vn.nguongocso.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.response.ChainVerificationResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

@ExtendWith(MockitoExtension.class)
class ChainVerificationServiceTest {

    @Mock private ChainEventRepository chainEventRepository;
    @Mock private ProductionLotRepository productionLotRepository;
    @Mock private UserRepository userRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Spy private EventHashService eventHashService = new EventHashService(new ObjectMapper());
    @Mock private TraceCodeRepository traceCodeRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private EventValidationService eventValidationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PermissionChecker permissionChecker;
    @Mock private OrganizationUserRepository organizationUserRepository;
    @InjectMocks private ChainEventServiceImpl chainEventService;

    private Shipment shipment;
    private User actor;
    private CustomUserDetails adminUser;

    @BeforeEach
    void setUp() {
        shipment = new Shipment();
        shipment.setId(UUID.fromString("9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f"));
        shipment.setName("Lô chè Tân Cương T8/2026");
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        actor = new User();
        actor.setUserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        actor.setFullName("Nguyễn Văn C");

        adminUser = mock(CustomUserDetails.class);
        lenient().when(adminUser.getRoleCode()).thenReturn("VT-01");
        lenient().when(adminUser.getUserId()).thenReturn(actor.getUserId());
        lenient().when(adminUser.getUsername()).thenReturn("admin");
        lenient().when(adminUser.getFullName()).thenReturn("Admin");
        lenient().when(adminUser.getOrganizationId()).thenReturn(UUID.randomUUID());
    }

    private ChainEvent buildEvent(ChainEventType type, LocalDateTime recordedAt, LocalDateTime createdAt,
            String data, String hash, String prev) {
        return ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(type)
                .recordedAt(recordedAt)
                .createdAt(createdAt)
                .recordedBy(actor)
                .eventData(data)
                .hash(hash)
                .previousHash(prev)
                .isCorrection(false)
                .build();
    }

    private String compute(ChainEvent e, String prev) {
        return new EventHashService(new ObjectMapper()).calculateHash(e, prev);
    }

    /** Builds a correct 3-event linked chain using the real deterministic algorithm.
     *  Hash chain ordering follows createdAt (server-generated), not recordedAt. */
    private List<ChainEvent> buildIntactChain() {
        ChainEvent e1 = buildEvent(ChainEventType.TRANSPORT,
                LocalDateTime.of(2026, 8, 11, 10, 0, 0),   // recordedAt
                LocalDateTime.of(2026, 8, 11, 10, 1, 0),   // createdAt
                "{\"from\":\"A\"}", null, null);
        String h1 = compute(e1, "");
        e1.setHash(h1);
        e1.setPreviousHash(null);

        ChainEvent e2 = buildEvent(ChainEventType.TRANSPORT,
                LocalDateTime.of(2026, 8, 11, 11, 0, 0),
                LocalDateTime.of(2026, 8, 11, 11, 1, 0),
                "{\"to\":\"B\"}", null, null);
        String h2 = compute(e2, h1);
        e2.setHash(h2);
        e2.setPreviousHash(h1);

        ChainEvent e3 = buildEvent(ChainEventType.WAREHOUSE_RECEIPT,
                LocalDateTime.of(2026, 8, 11, 12, 0, 0),
                LocalDateTime.of(2026, 8, 11, 12, 1, 0),
                "{\"qty\":500}", null, null);
        String h3 = compute(e3, h2);
        e3.setHash(h3);
        e3.setPreviousHash(h2);

        return List.of(e1, e2, e3);
    }

    /**
     * REGRESSION TEST — recordedAt must NOT determine the cryptographic chain position.
     *
     * Chain is built in insertion order (createdAt):
     *   TRANSPORT #1 (createdAt 13:32:43, recordedAt 13:32:00) -> H1
     *   PROCUREMENT (createdAt 13:35:39, recordedAt 13:35:39)  -> H2 (prev H1)
     *   TRANSPORT #2 (createdAt 13:36:43, recordedAt 13:32:00) -> H3 (prev H2)
     *   TRANSPORT #3 (createdAt 13:38:17, recordedAt 13:36:00) -> H4 (prev H3)
     *
     * Even though TRANSPORT #2 has an EARLIER recordedAt (13:32) than PROCUREMENT (13:35),
     * the hash chain must follow createdAt insertion order and verify INTACT.
     */
    @Test
    void recordedAtDoesNotDetermineChainPosition() {
        // Event 1: TRANSPORT, createdAt 13:32:43, recordedAt 13:32:00
        ChainEvent t1 = buildEvent(ChainEventType.TRANSPORT,
                LocalDateTime.of(2026, 8, 11, 13, 32, 0),
                LocalDateTime.of(2026, 8, 11, 13, 32, 43),
                "{\"from\":\"A\"}", null, null);
        String h1 = compute(t1, "");
        t1.setHash(h1);
        t1.setPreviousHash(null);

        // Event 2: PROCUREMENT, createdAt 13:35:39, recordedAt 13:35:39
        ChainEvent p = buildEvent(ChainEventType.PROCUREMENT,
                LocalDateTime.of(2026, 8, 11, 13, 35, 39),
                LocalDateTime.of(2026, 8, 11, 13, 35, 39),
                "{\"receivedQuantity\":50}", null, null);
        String h2 = compute(p, h1);
        p.setHash(h2);
        p.setPreviousHash(h1);

        // Event 3: TRANSPORT #2, createdAt 13:36:43, recordedAt 13:32:00 (EARLIER than PROCUREMENT)
        ChainEvent t2 = buildEvent(ChainEventType.TRANSPORT,
                LocalDateTime.of(2026, 8, 11, 13, 32, 0),
                LocalDateTime.of(2026, 8, 11, 13, 36, 43),
                "{\"from\":\"B\"}", null, null);
        String h3 = compute(t2, h2);
        t2.setHash(h3);
        t2.setPreviousHash(h2);

        // Event 4: TRANSPORT #3, createdAt 13:38:17, recordedAt 13:36:00
        ChainEvent t3 = buildEvent(ChainEventType.TRANSPORT,
                LocalDateTime.of(2026, 8, 11, 13, 36, 0),
                LocalDateTime.of(2026, 8, 11, 13, 38, 17),
                "{\"from\":\"C\"}", null, null);
        String h4 = compute(t3, h3);
        t3.setHash(h4);
        t3.setPreviousHash(h3);

        List<ChainEvent> events = List.of(t1, p, t2, t3);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId())).thenReturn(events);

        ChainVerificationResponse resp = chainEventService.verifyChainIntegrity(shipment.getId(), adminUser);

        assertThat(resp.getIsIntegrityVerified()).isTrue();
        assertThat(resp.getVerificationStatus()).isEqualTo("INTACT");
        assertThat(resp.getTotalEvents()).isEqualTo(4);
        assertThat(resp.getFailedEventIndex()).isNull();
        // Assert chain follows insertion order, not recordedAt order:
        assertThat(resp.getEvents().get(0).getEventId()).isEqualTo(t1.getId());
        assertThat(resp.getEvents().get(1).getEventId()).isEqualTo(p.getId());
        assertThat(resp.getEvents().get(2).getEventId()).isEqualTo(t2.getId());
        assertThat(resp.getEvents().get(3).getEventId()).isEqualTo(t3.getId());
    }

    @Test
    void tc01_intactChainVerifiesTrue() {
        List<ChainEvent> events = buildIntactChain();
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId())).thenReturn(events);

        ChainVerificationResponse resp = chainEventService.verifyChainIntegrity(shipment.getId(), adminUser);

        assertThat(resp.getIsIntegrityVerified()).isTrue();
        assertThat(resp.getVerificationStatus()).isEqualTo("INTACT");
        assertThat(resp.getTotalEvents()).isEqualTo(3);
        assertThat(resp.getFailedEventIndex()).isNull();
        // TC-03: verification creates an activity log
        verify(eventPublisher, times(1)).publishEvent(org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void tc02_tamperedThirdEventDetectedAsFirstInvalid() {
        List<ChainEvent> events = buildIntactChain();
        // Tamper with event #3 (index 2): change its eventData so stored hash no longer matches
        ChainEvent tampered = events.get(2);
        tampered.setEventData("{\"qty\":450}");

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId())).thenReturn(events);

        ChainVerificationResponse resp = chainEventService.verifyChainIntegrity(shipment.getId(), adminUser);

        assertThat(resp.getIsIntegrityVerified()).isFalse();
        assertThat(resp.getVerificationStatus()).isEqualTo("BROKEN");
        assertThat(resp.getFailedEventIndex()).isEqualTo(3);
        assertThat(resp.getFailedEventId()).isEqualTo(tampered.getId());
        assertThat(resp.getFailureReason()).isNotNull();
    }

    @Test
    void tc02b_previousHashMismatchDetected() {
        List<ChainEvent> events = buildIntactChain();
        // Corrupt the stored previousHash of event #2 (index 1)
        events.get(1).setPreviousHash("bogus-previous-hash");

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId())).thenReturn(events);

        ChainVerificationResponse resp = chainEventService.verifyChainIntegrity(shipment.getId(), adminUser);

        assertThat(resp.getIsIntegrityVerified()).isFalse();
        assertThat(resp.getFailedEventIndex()).isEqualTo(2);
        assertThat(resp.getFailureReason()).contains("Previous hash mismatch");
    }

    @Test
    void tc03_verificationRecordsActivityLog() {
        List<ChainEvent> events = buildIntactChain();
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId())).thenReturn(events);

        chainEventService.verifyChainIntegrity(shipment.getId(), adminUser);

        verify(eventPublisher, times(1)).publishEvent(org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void tc04_emptyChainReturns400() {
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> chainEventService.verifyChainIntegrity(shipment.getId(), adminUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa có sự kiện");
    }

    @Test
    void tc05_shipmentNotFoundReturns404() {
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chainEventService.verifyChainIntegrity(shipment.getId(), adminUser))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}