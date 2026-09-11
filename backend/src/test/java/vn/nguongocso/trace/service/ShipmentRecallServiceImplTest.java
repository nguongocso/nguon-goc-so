package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.dto.request.RecallRequest;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.RecallRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.ShipmentRecallServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShipmentRecallServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private TraceCodeRepository traceCodeRepository;
    @Mock
    private CodeRangeRepository codeRangeRepository;
    @Mock
    private RecallRepository recallRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ShipmentRecallServiceImpl service;

    private CustomUserDetails currentUser;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        currentUser = mock(CustomUserDetails.class);
        when(currentUser.getRoleCode()).thenReturn("VT-02");
        when(currentUser.getOrganizationId()).thenReturn(organizationId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recallShipment_rejectsSplitParentBeforeMutatingData() {
        UUID shipmentId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setOrganizationId(organizationId);
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setOrganization(organization);
        shipment.setStatus(ShipmentStatus.SPLIT);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        RecallRequest request = new RecallRequest();
        request.setReason("Thu hồi lô cha");

        assertThatThrownBy(() -> service.recallShipment(shipmentId, request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể thu hồi lô cha đã tách");

        verify(recallRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(shipmentRepository, never()).save(shipment);
        verify(traceCodeRepository, never()).findByShipmentId(shipmentId);
    }
}
