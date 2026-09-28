package vn.nguongocso.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordProcurementEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.impl.ProcurementEventServiceImpl;
import vn.nguongocso.event.service.resolver.ProcurementShipmentResolver;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
public class ProcurementEventServiceTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentHandoverRepository shipmentHandoverRepository;
    @Mock private ChainEventRepository chainEventRepository;
    @Mock private ChainEventService chainEventService;
    @Mock private UserRepository userRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private EventValidationService eventValidationService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private ProcurementEventServiceImpl service;
    private ProcurementShipmentResolver procurementShipmentResolver;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID shipmentId = UUID.randomUUID();

    private CustomUserDetails userDetails;
    private Shipment shipment;
    private User user;
    private Organization org;

    @BeforeEach
    void setUp() {
        procurementShipmentResolver = new ProcurementShipmentResolver(
                shipmentRepository, shipmentHandoverRepository, chainEventRepository, eventValidationService);
        service = new ProcurementEventServiceImpl(
                procurementShipmentResolver, chainEventService, userRepository, objectMapper, eventPublisher);
        org = new Organization();
        org.setOrganizationId(orgId);

        user = new User();
        user.setUserId(userId);
        user.setFullName("Công ty ABC");

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng 1");
        Organization sourceOrganization = new Organization();
        sourceOrganization.setOrganizationId(UUID.randomUUID());
        shipment.setOrganization(sourceOrganization);
        shipment.setRecipientOrganization(org);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        userDetails = mock(CustomUserDetails.class);
    }

    @Test
    void recordProcurement_shouldSuccess() throws Exception {
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getRoleCode()).thenReturn("VT-04");
        when(userDetails.getOrganizationId()).thenReturn(orgId);

        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);
        request.setNotes("OK");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chainEventService.saveWithChainHash(any(ChainEvent.class))).thenAnswer(inv -> {
            ChainEvent event = (ChainEvent) inv.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        ChainEventResponse response = service.recordProcurementEvent(request, userDetails);

        assertThat(response).isNotNull();
        assertThat(response.getShipmentId()).isEqualTo(shipmentId);
        assertThat(response.getEventData().get("receivedQuantity")).isEqualTo(100L);
        ArgumentCaptor<ChainEvent> eventCaptor = ArgumentCaptor.forClass(ChainEvent.class);
        verify(chainEventService).saveWithChainHash(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getRecordedOrganizationId()).isEqualTo(orgId);
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    void recordProcurement_shouldThrow_whenShipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());
        when(userDetails.getRoleCode()).thenReturn("VT-04");

        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);

        assertThatThrownBy(() -> service.recordProcurementEvent(request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy lô hàng.");
    }

    @Test
    void recordProcurement_shouldThrow_whenShipmentRecalled() {
        when(userDetails.getRoleCode()).thenReturn("VT-04");
        when(userDetails.getOrganizationId()).thenReturn(orgId);

        shipment.setStatus(ShipmentStatus.RECALLED);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);

        assertThatThrownBy(() -> service.recordProcurementEvent(request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô hàng đã bị thu hồi, không thể ghi sự kiện.");
    }

    @Test
    void recordProcurement_shouldRejectDifferentRecipientOrganization() {
        when(userDetails.getRoleCode()).thenReturn("VT-04");
        when(userDetails.getOrganizationId()).thenReturn(UUID.randomUUID());
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);

        assertThatThrownBy(() -> service.recordProcurementEvent(request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô hàng không được giao cho tổ chức của bạn.");
        verify(chainEventService, never()).saveWithChainHash(any());
    }

    @Test
    void recordProcurement_shouldThrow_whenRoleNotVT04() {
        when(userDetails.getRoleCode()).thenReturn("VT-02");

        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);

        assertThatThrownBy(() -> service.recordProcurementEvent(request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chỉ Doanh nghiệp thu mua mới được ghi sự kiện này");
    }

    @Test
    void recordProcurement_shouldSuccess_whenShipmentHasAcceptedHandover() throws Exception {
        shipment.setRecipientOrganization(null); // non-split shipment
        when(userDetails.getRoleCode()).thenReturn("VT-04");
        when(userDetails.getOrganizationId()).thenReturn(orgId);
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getUsername()).thenReturn("procurement_user");
        when(userDetails.getFullName()).thenReturn("Công ty ABC");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentHandoverRepository.existsByShipmentIdAndToOrganizationOrganizationIdAndStatus(
                shipmentId, orgId, ShipmentHandoverStatus.ACCEPTED)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{\"shipmentId\":\"" + shipmentId + "\"}");

        ChainEvent savedEvent = new ChainEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setEventData("{\"shipmentName\":\"Lô hàng 1\",\"receivedQuantity\":100}");
        savedEvent.setRecordedBy(user);
        when(chainEventService.saveWithChainHash(any())).thenReturn(savedEvent);

        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);
        request.setNotes("Đã nhận hàng từ phiếu bàn giao");

        ChainEventResponse response = service.recordProcurementEvent(request, userDetails);

        assertThat(response).isNotNull();
        assertThat(response.getEventData().get("receivedQuantity")).isEqualTo(100L);
        verify(chainEventService, times(1)).saveWithChainHash(any());
    }
}
