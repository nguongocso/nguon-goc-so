package vn.nguongocso.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordProcurementEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.impl.ProcurementEventServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcurementEventServiceTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ChainEventRepository chainEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ProcurementEventServiceImpl service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID shipmentId = UUID.randomUUID();

    private CustomUserDetails userDetails;
    private Shipment shipment;
    private User user;
    private Organization org;

    @BeforeEach
    void setUp() {
        org = new Organization();
        org.setOrganizationId(orgId);

        user = new User();
        user.setUserId(userId);
        user.setFullName("Công ty ABC");

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng 1");
        shipment.setOrganization(org);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        userDetails = mock(CustomUserDetails.class);
    }

    @Test
    void recordProcurement_shouldSuccess() throws Exception {
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getRoleCode()).thenReturn("VT-04");

        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);
        request.setNotes("OK");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chainEventRepository.save(any(ChainEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{}");

        ChainEventResponse response = service.recordProcurementEvent(request, userDetails);

        assertThat(response).isNotNull();
        assertThat(response.getShipmentId()).isEqualTo(shipmentId);
        assertThat(response.getEventData().get("receivedQuantity")).isEqualTo(100L);
        verify(chainEventRepository).save(any(ChainEvent.class));
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
    void recordProcurement_shouldThrow_whenRoleNotVT04() {
        when(userDetails.getRoleCode()).thenReturn("VT-02");

        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);

        assertThatThrownBy(() -> service.recordProcurementEvent(request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chỉ Doanh nghiệp thu mua mới được ghi sự kiện này");
    }
}