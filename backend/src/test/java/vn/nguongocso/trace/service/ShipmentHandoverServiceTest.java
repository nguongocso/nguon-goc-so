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
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
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
import vn.nguongocso.trace.service.HandoverExpiryService;
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

    @Mock
    private HandoverExpiryService handoverExpiryService;

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
        fromOrganization.setStatus(OrganizationStatus.ACTIVE);

        toOrganization = new Organization();
        toOrganization.setOrganizationId(toOrgId);
        toOrganization.setName("To Org");
        toOrganization.setStatus(OrganizationStatus.ACTIVE);

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
    void testCreateHandover_DraftShipmentBlocked() {
        shipment.setStatus(ShipmentStatus.DRAFT);

        CreateHandoverRequest request = new CreateHandoverRequest();
        request.setShipmentId(shipmentId);
        request.setToOrganizationId(toOrgId);
        request.setQuantity(800L);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> handoverService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kích hoạt tem");
    }

    @Test
    void testCreateHandover_InactiveReceiverBlocked() {
        toOrganization.setStatus(OrganizationStatus.INACTIVE);

        CreateHandoverRequest request = new CreateHandoverRequest();
        request.setShipmentId(shipmentId);
        request.setToOrganizationId(toOrgId);
        request.setQuantity(800L);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(organizationRepository.findById(toOrgId)).thenReturn(Optional.of(toOrganization));
        when(traceCodeRepository.existsByShipmentIdAndStatus(shipmentId, TraceCodeStatus.LOCKED))
                .thenReturn(false);

        assertThatThrownBy(() -> handoverService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không còn hoạt động");
    }

    @Test
    void testCreateHandover_PersistsAttachmentAndNotifiesReceiver() {
        CreateHandoverRequest request = new CreateHandoverRequest();
        request.setShipmentId(shipmentId);
        request.setToOrganizationId(toOrgId);
        request.setQuantity(800L);
        request.setAttachmentPath("handover-docs/phieu-xuat-kho.pdf");

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(organizationRepository.findById(toOrgId)).thenReturn(Optional.of(toOrganization));
        when(traceCodeRepository.existsByShipmentIdAndStatus(shipmentId, TraceCodeStatus.LOCKED))
                .thenReturn(false);
        when(handoverRepository.sumQuantityByShipmentIdAndStatusIn(eq(shipmentId), any()))
                .thenReturn(0L);
        when(handoverRepository.save(any(ShipmentHandover.class))).thenAnswer(inv -> inv.getArgument(0));

        HandoverResponse response = handoverService.create(request);

        assertThat(response.getAttachmentPath()).isEqualTo("handover-docs/phieu-xuat-kho.pdf");
        verify(notificationService).sendHandoverNotification(any(), any(), any(), eq(toOrgId));
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
        assertThat(response.getConfirmedAt()).isNotNull();
        assertThat(response.getConfirmedBy()).isEqualTo(userId);
        // QTN-31: Xác nhận phải ghi nhận sự kiện HANDOVER trên timeline và gửi thông báo về bên giao.
        verify(chainEventService).saveWithChainHash(argThat(event ->
                event.getEventType() == ChainEventType.HANDOVER &&
                event.getEventData().contains("fromOrganizationName") &&
                event.getEventData().contains(fromOrganization.getName()) &&
                event.getEventData().contains("toOrganizationName") &&
                event.getEventData().contains(toOrganization.getName())
        ));
        // Thông báo xác nhận gửi về tổ chức GIAO với entityId = phiếu bàn giao.
        verify(notificationService)
                .sendHandoverNotification(any(), any(), eq(handoverId), eq(toOrganization.getOrganizationId()));
    }

    @Test
    void testAcceptHandover_NotReceiverOrg_Forbidden() {
        UUID handoverId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();

        Organization otherOrg = Organization.builder()
                .organizationId(otherOrgId).name("Other Org").build();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(fromOrganization)
                .toOrganization(otherOrg)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdBy(mock(User.class))
                .build();

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handoverService.accept(handoverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không có quyền xác nhận");
        verify(chainEventService, never()).saveWithChainHash(any());
        verify(notificationService, never()).sendHandoverNotification(any(), any(), any(), any());
    }

    @Test
    void testAcceptHandover_NotPending_Blocked() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(toOrganization)
                .toOrganization(fromOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.ACCEPTED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdBy(mock(User.class))
                .build();

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handoverService.accept(handoverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ có thể xác nhận phiếu đang chờ");
        verify(chainEventService, never()).saveWithChainHash(any());
    }

    @Test
    void testAcceptHandover_Expired_Blocked() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(toOrganization)
                .toOrganization(fromOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .createdBy(mock(User.class))
                .build();

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handoverService.accept(handoverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Phiếu bàn giao đã hết hạn");
        verify(chainEventService, never()).saveWithChainHash(any());
        // Quá hạn → chuyển EXPIRED (qua HandoverExpiryService) rồi chặn accept.
        verify(handoverExpiryService).expireOverdueHandovers();
    }

    @Test
    void testRejectHandover_Expired_Blocked() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(toOrganization)
                .toOrganization(fromOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .createdBy(mock(User.class))
                .build();

        CancelHandoverRequest request = new CancelHandoverRequest();
        request.setReason("Hang khong dat chat luong");

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handoverService.reject(handoverId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Phiếu bàn giao đã hết hạn");
        verify(handoverExpiryService).expireOverdueHandovers();
        verify(notificationService, never()).sendHandoverNotification(any(), any(), any(), any());
    }

    @Test
    void testCancelHandover_Expired_Blocked() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(fromOrganization)
                .toOrganization(toOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .expiresAt(LocalDateTime.now().minusHours(2))
                .createdBy(mock(User.class))
                .build();

        CancelHandoverRequest request = new CancelHandoverRequest();
        request.setReason("Khong can nua");

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handoverService.cancel(handoverId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Phiếu bàn giao đã hết hạn");
        verify(handoverExpiryService).expireOverdueHandovers();
        verify(handoverRepository, never()).save(any());
        verify(notificationService, never()).sendHandoverNotification(any(), any(), any(), any());
    }

    @Test
    void testGetById_Expired_ConvertsToExpired() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(fromOrganization)
                .toOrganization(toOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .createdBy(mock(User.class))
                .build();

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        HandoverResponse response = handoverService.getById(handoverId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ShipmentHandoverStatus.EXPIRED);
        // Lazy expire: chuyển EXPIRED qua HandoverExpiryService để DB + thông báo kịp
        // thời ngay cả khi scheduler chưa tới giờ chạy.
        verify(handoverExpiryService).expireOverdueHandovers();
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
        assertThat(response.getRejectedAt()).isNotNull();
        // Reject không ghi sự kiện chuỗi (chỉ accept mới chuyển trách nhiệm).
        verify(chainEventService, never()).saveWithChainHash(any());
        // Thông báo từ chối gửi về tổ chức GIAO kèm lý do, entityId = phiếu.
        verify(notificationService)
                .sendHandoverNotification(any(), any(), eq(handoverId), eq(toOrganization.getOrganizationId()));
    }

    @Test
    void testRejectHandover_NotReceiverOrg_Forbidden() {
        UUID handoverId = UUID.randomUUID();

        Organization otherOrg = Organization.builder()
                .organizationId(UUID.randomUUID()).name("Other Org").build();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(fromOrganization)
                .toOrganization(otherOrg)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .createdBy(mock(User.class))
                .build();

        CancelHandoverRequest request = new CancelHandoverRequest();
        request.setReason("Hang khong dat chat luong");

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handoverService.reject(handoverId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không có quyền từ chối");
        verify(notificationService, never()).sendHandoverNotification(any(), any(), any(), any());
    }

    @Test
    void testRejectHandover_NotPending_Blocked() {
        UUID handoverId = UUID.randomUUID();

        ShipmentHandover handover = ShipmentHandover.builder()
                .id(handoverId)
                .shipment(shipment)
                .fromOrganization(toOrganization)
                .toOrganization(fromOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.CANCELLED)
                .createdBy(mock(User.class))
                .build();

        CancelHandoverRequest request = new CancelHandoverRequest();
        request.setReason("Hang khong dat chat luong");

        when(handoverRepository.findById(handoverId)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> handoverService.reject(handoverId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ có thể từ chối phiếu đang chờ");
        verify(notificationService, never()).sendHandoverNotification(any(), any(), any(), any());
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
