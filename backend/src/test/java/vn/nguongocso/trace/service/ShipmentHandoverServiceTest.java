package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.CancelHandoverRequest;
import vn.nguongocso.trace.dto.request.CreateHandoverRequest;
import vn.nguongocso.trace.dto.response.HandoverResponse;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.ShipmentHandover;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.ShipmentHandoverServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShipmentHandoverServiceTest {

    @Mock
    private ShipmentHandoverRepository handoverRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TraceCodeRepository traceCodeRepository;
    @Mock
    private ChainEventService chainEventService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private ShipmentHandoverServiceImpl handoverService;

    private CustomUserDetails currentUser;
    private Organization fromOrganization;
    private Organization toOrganization;
    private Shipment shipment;
    private UUID fromOrgId;
    private UUID toOrgId;
    private UUID shipmentId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        fromOrgId = UUID.randomUUID();
        toOrgId = UUID.randomUUID();
        shipmentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        currentUser = mock(CustomUserDetails.class);
        lenient().when(currentUser.getOrganizationId()).thenReturn(fromOrgId);
        lenient().when(currentUser.getUserId()).thenReturn(userId);

        User user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);
        lenient().when(currentUser.getUser()).thenReturn(user);

        fromOrganization = new Organization();
        fromOrganization.setOrganizationId(fromOrgId);
        fromOrganization.setName("From Org");

        toOrganization = new Organization();
        toOrganization.setOrganizationId(toOrgId);
        toOrganization.setName("To Org");

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Test Shipment");
        shipment.setTotalQuantity(10000L);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setOrganization(fromOrganization);

        // Setup SecurityContext
        org.springframework.security.core.Authentication authentication =
                mock(org.springframework.security.core.Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(currentUser);
        org.springframework.security.core.context.SecurityContext securityContext =
                mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreateHandover_Success() {
        CreateHandoverRequest request = new CreateHandoverRequest();
        request.setShipmentId(shipmentId);
        request.setToOrganizationId(toOrgId);
        request.setQuantity(800L);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(organizationRepository.findById(toOrgId)).thenReturn(Optional.of(toOrganization));
        when(traceCodeRepository.existsByShipmentIdAndStatus(shipmentId, TraceCodeStatus.LOCKED))
                .thenReturn(false);
        when(handoverRepository.sumQuantityByShipmentIdAndStatusIn(eq(shipmentId), any()))
                .thenReturn(0L);
        when(handoverRepository.save(any(ShipmentHandover.class))).thenAnswer(inv -> inv.getArgument(0));

        HandoverResponse response = handoverService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ShipmentHandoverStatus.PENDING_CONFIRMATION);
        assertThat(response.getQuantity()).isEqualTo(800L);
        verify(handoverRepository).save(any(ShipmentHandover.class));
    }

    @Test
    void testCreateHandover_ExceedsRemaining() {
        CreateHandoverRequest request = new CreateHandoverRequest();
        request.setShipmentId(shipmentId);
        request.setToOrganizationId(toOrgId);
        request.setQuantity(8000L);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(organizationRepository.findById(toOrgId)).thenReturn(Optional.of(toOrganization));
        when(traceCodeRepository.existsByShipmentIdAndStatus(shipmentId, TraceCodeStatus.LOCKED))
                .thenReturn(false);
        when(handoverRepository.sumQuantityByShipmentIdAndStatusIn(eq(shipmentId), any()))
                .thenReturn(5000L);

        assertThatThrownBy(() -> handoverService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void testCreateHandover_RecalledShipment() {
        shipment.setStatus(ShipmentStatus.RECALLED);

        CreateHandoverRequest request = new CreateHandoverRequest();
        request.setShipmentId(shipmentId);
        request.setToOrganizationId(toOrgId);
        request.setQuantity(800L);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> handoverService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void testCancelHandover_Success() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(fromOrganization)
                .toOrganization(toOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .createdBy(currentUser.getUser())
                .build();

        CancelHandoverRequest request = new CancelHandoverRequest();
        request.setReason("Khong can nua");

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));
        when(handoverRepository.save(any(ShipmentHandover.class))).thenAnswer(inv -> inv.getArgument(0));

        HandoverResponse response = handoverService.cancel(handoverId, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ShipmentHandoverStatus.CANCELLED);
        assertThat(response.getCancelReason()).isEqualTo("Khong can nua");
    }

    @Test
    void testAcceptHandover_Success() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(toOrganization)
                .toOrganization(fromOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdBy(mock(User.class))
                .build();

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));
        when(handoverRepository.save(any(ShipmentHandover.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chainEventService.saveWithChainHash(any())).thenAnswer(inv -> inv.getArgument(0));

        HandoverResponse response = handoverService.accept(handoverId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ShipmentHandoverStatus.ACCEPTED);
        verify(chainEventService).saveWithChainHash(any());
    }

    @Test
    void testRejectHandover_Success() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(toOrganization)
                .toOrganization(fromOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .createdBy(mock(User.class))
                .build();

        CancelHandoverRequest request = new CancelHandoverRequest();
        request.setReason("Hang khong dung mo ta");

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));
        when(handoverRepository.save(any(ShipmentHandover.class))).thenAnswer(inv -> inv.getArgument(0));

        HandoverResponse response = handoverService.reject(handoverId, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ShipmentHandoverStatus.REJECTED);
        assertThat(response.getCancelReason()).isEqualTo("Hang khong dung mo ta");
    }

    @Test
    void testGetById_Unauthorized() {
        UUID handoverId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();

        Organization otherOrg = Organization.builder()
                .organizationId(otherOrgId).name("Other Org").build();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(otherOrg)
                .toOrganization(otherOrg)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .createdBy(mock(User.class))
                .build();

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handoverService.getById(handoverId))
                .isInstanceOf(BusinessException.class);
    }
}
