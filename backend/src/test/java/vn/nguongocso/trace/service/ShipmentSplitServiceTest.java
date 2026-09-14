package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.service.InspectionEligibilityService;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventHashService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.SplitShipmentAllocationRequest;
import vn.nguongocso.trace.dto.request.SplitShipmentRequest;
import vn.nguongocso.trace.dto.response.SplitShipmentResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.ShipmentServiceImpl;

class ShipmentSplitServiceTest {
    private ShipmentRepository shipments = mock(ShipmentRepository.class);
    private TraceCodeRepository codes = mock(TraceCodeRepository.class);
    private OrganizationRepository organizations = mock(OrganizationRepository.class);
    private ChainEventRepository events = mock(ChainEventRepository.class);
    private UserRepository users = mock(UserRepository.class);
    private PermissionChecker permissions = mock(PermissionChecker.class);
    private ShipmentServiceImpl service;
    private CustomUserDetails actor;
    private Shipment parent;
    private UUID sourceOrgId = UUID.randomUUID();
    private UUID parentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ShipmentServiceImpl(shipments, codes, mock(CodeRangeRepository.class),
                mock(ProductionLotRepository.class), mock(QRCodeService.class), users,
                mock(ApplicationEventPublisher.class), mock(NotificationService.class), permissions,
                mock(InspectionEligibilityService.class), organizations, mock(ShipmentHandoverRepository.class), events,
                new EventHashService(new ObjectMapper()), new ObjectMapper());
        actor = mock(CustomUserDetails.class);
        when(actor.getRoleCode()).thenReturn("VT-02"); when(actor.getOrganizationId()).thenReturn(sourceOrgId);
        when(actor.getUserId()).thenReturn(UUID.randomUUID()); when(actor.getFullName()).thenReturn("Quản lý HTX");
        when(actor.getUsername()).thenReturn("manager");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        when(users.getReferenceById(any())).thenReturn(new User());
        parent = parent();
        when(shipments.findOwnedByIdForSplitUpdate(parentId, sourceOrgId)).thenReturn(java.util.Optional.of(parent));
        when(shipments.existsByParentShipment_Id(parentId)).thenReturn(false);
        when(events.findTopByShipmentIdOrderByCreatedAtDesc(parentId)).thenReturn(java.util.Optional.empty());
        when(shipments.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0); if (shipment.getId() == null) shipment.setId(UUID.randomUUID()); return shipment;
        });
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void tc01_tc02_splits_all_codes_without_changing_code_range_usage() {
        List<TraceCode> traceCodes = List.of(code("HTX00000001"), code("HTX00000002"), code("HTX00000003"), code("HTX00000004"));
        when(codes.findAllByShipmentIdForSplitUpdate(parentId)).thenReturn(traceCodes);
        Organization first = partner(), second = partner();
        when(organizations.findById(first.getOrganizationId())).thenReturn(java.util.Optional.of(first));
        when(organizations.findById(second.getOrganizationId())).thenReturn(java.util.Optional.of(second));

        SplitShipmentResponse response = service.splitShipment(parentId, request(first, second, 2, 2));

        assertThat(response.getTotalChildren()).isEqualTo(2);
        assertThat(parent.getStatus()).isEqualTo(ShipmentStatus.SPLIT);
        assertThat(traceCodes).allMatch(code -> code.getShipment().getParentShipment() == parent);
        assertThat(parent.getCodeRange().getUsedCount()).isEqualTo(4);
        ArgumentCaptor<ChainEvent> eventCaptor = ArgumentCaptor.forClass(ChainEvent.class);
        verify(events, times(2)).save(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).allSatisfy(event -> {
            assertThat(event.getEventType()).isEqualTo(vn.nguongocso.event.enums.ChainEventType.SPLIT);
            assertThat(event.getEventData()).contains("sourceShipmentId", "sourceShipmentName",
                    "recipientOrganizationId", "recipientOrganizationName", "allocatedQuantity",
                    "sourceLastEventHash");
        });
    }

    @Test
    void tc02_rejects_non_full_allocation_before_persisting_children() {
        when(codes.findAllByShipmentIdForSplitUpdate(parentId)).thenReturn(List.of(code("HTX00000001"), code("HTX00000002"), code("HTX00000003"), code("HTX00000004")));
        Organization first = partner(), second = partner();
        when(organizations.findById(first.getOrganizationId())).thenReturn(java.util.Optional.of(first));
        when(organizations.findById(second.getOrganizationId())).thenReturn(java.util.Optional.of(second));

        assertThatThrownBy(() -> service.splitShipment(parentId, request(first, second, 1, 2)))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails()).isEqualTo(java.util.Map.of("code", "SPLIT_004")));
        verify(shipments, never()).save(any(Shipment.class));
    }

    @Test
    void tc04_recalled_source_is_rejected_without_writes() {
        parent.setStatus(ShipmentStatus.RECALLED);
        assertThatThrownBy(() -> service.splitShipment(parentId, new SplitShipmentRequest()))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails()).isEqualTo(java.util.Map.of("code", "INVALID_SHIPMENT_STATUS")));
        verify(codes, never()).findAllByShipmentIdForSplitUpdate(any());
        verify(shipments, never()).save(any(Shipment.class));
    }

    @Test
    void rejects_cross_tenant_source_as_not_found_in_locked_query() {
        when(shipments.findOwnedByIdForSplitUpdate(parentId, sourceOrgId)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.splitShipment(parentId, new SplitShipmentRequest()))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails()).isEqualTo(java.util.Map.of("code", "SHIPMENT_NOT_FOUND")));
    }

    @Test
    void rejects_missing_split_permission() {
        doThrow(new BusinessException("denied")).when(permissions).check("shipment", "SPLIT");
        assertThatThrownBy(() -> service.splitShipment(parentId, new SplitShipmentRequest()))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails()).isEqualTo(java.util.Map.of("code", "ACCESS_DENIED")));
    }

    @Test
    void rejects_wrong_role_even_when_permission_check_would_pass() {
        when(actor.getRoleCode()).thenReturn("VT-04");

        assertThatThrownBy(() -> service.splitShipment(parentId, new SplitShipmentRequest()))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails())
                                .isEqualTo(java.util.Map.of("code", "ACCESS_DENIED")));
        verify(permissions, never()).check("shipment", "SPLIT");
    }

    @Test
    void rejects_child_shipment_before_locking_codes() {
        parent.setParentShipment(new Shipment());

        assertThatThrownBy(() -> service.splitShipment(parentId, new SplitShipmentRequest()))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails())
                                .isEqualTo(java.util.Map.of("code", "CHILD_SPLIT_NOT_ALLOWED")));
        verify(codes, never()).findAllByShipmentIdForSplitUpdate(any());
    }

    @Test
    void rejects_when_any_trace_code_is_not_inactive() {
        TraceCode activeCode = code("HTX00000004");
        activeCode.setStatus(TraceCodeStatus.ACTIVE);
        when(codes.findAllByShipmentIdForSplitUpdate(parentId)).thenReturn(List.of(
                code("HTX00000001"), code("HTX00000002"), code("HTX00000003"), activeCode));

        assertThatThrownBy(() -> service.splitShipment(parentId, new SplitShipmentRequest()))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails())
                                .isEqualTo(java.util.Map.of("code", "TRACE_CODE_STATE_CHANGED")));
        verify(shipments, never()).save(any(Shipment.class));
    }

    @Test
    void rejects_duplicate_recipient_before_persisting_children() {
        when(codes.findAllByShipmentIdForSplitUpdate(parentId)).thenReturn(List.of(
                code("HTX00000001"), code("HTX00000002"), code("HTX00000003"), code("HTX00000004")));
        Organization recipient = partner();
        when(organizations.findById(recipient.getOrganizationId()))
                .thenReturn(java.util.Optional.of(recipient));
        SplitShipmentRequest duplicate = request(recipient, recipient, 2, 2);

        assertThatThrownBy(() -> service.splitShipment(parentId, duplicate))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails())
                                .isEqualTo(java.util.Map.of("code", "SPLIT_003")));
        verify(shipments, never()).save(any(Shipment.class));
    }

    @Test
    void rejects_invalid_recipient_and_invalid_range_before_writes() {
        List<TraceCode> traceCodes = List.of(code("HTX00000001"), code("HTX00000002"), code("HTX00000003"), code("HTX00000004"));
        when(codes.findAllByShipmentIdForSplitUpdate(parentId)).thenReturn(traceCodes);
        Organization invalid = partner(); invalid.setType(OrganizationType.COOPERATIVE);
        Organization second = partner();
        when(organizations.findById(invalid.getOrganizationId())).thenReturn(java.util.Optional.of(invalid));
        when(organizations.findById(second.getOrganizationId())).thenReturn(java.util.Optional.of(second));
        assertThatThrownBy(() -> service.splitShipment(parentId, request(invalid, second, 2, 2)))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails()).isEqualTo(java.util.Map.of("code", "SPLIT_005")));
        SplitShipmentRequest badRange = request(second, partner(), 2, 2);
        badRange.getAllocations().get(0).setFromCode("OUTSIDE");
        assertThatThrownBy(() -> service.splitShipment(parentId, badRange))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails()).isEqualTo(java.util.Map.of("code", "SPLIT_007")));
        verify(shipments, never()).save(any(Shipment.class));
    }

    @Test
    void rejects_overlapping_ranges_before_writes() {
        when(codes.findAllByShipmentIdForSplitUpdate(parentId)).thenReturn(List.of(code("HTX00000001"), code("HTX00000002"), code("HTX00000003"), code("HTX00000004")));
        Organization first = partner(), second = partner();
        when(organizations.findById(first.getOrganizationId())).thenReturn(java.util.Optional.of(first));
        SplitShipmentRequest overlap = request(first, second, 2, 2);
        overlap.getAllocations().get(1).setFromCode("HTX00000002");
        overlap.getAllocations().get(1).setToCode("HTX00000003");
        assertThatThrownBy(() -> service.splitShipment(parentId, overlap))
                .isInstanceOf(BusinessException.class).satisfies(error ->
                        assertThat(((BusinessException) error).getDetails()).isEqualTo(java.util.Map.of("code", "SPLIT_008")));
        verify(shipments, never()).save(any(Shipment.class));
    }

    private Shipment parent() {
        Organization source = new Organization(); source.setOrganizationId(sourceOrgId);
        ProductionLot lot = new ProductionLot(); lot.setId(UUID.randomUUID()); lot.setName("Lô sản xuất");
        CodeRange range = new CodeRange(); range.setUsedCount(4L);
        Shipment value = new Shipment(); value.setId(parentId); value.setOrganization(source); value.setProductionLot(lot); value.setCodeRange(range);
        value.setName("Lô cha"); value.setTotalQuantity(4); value.setStatus(ShipmentStatus.CODE_PRINTED); return value;
    }
    private TraceCode code(String value) { TraceCode code = new TraceCode(); code.setId(UUID.randomUUID()); code.setCodeValue(value); code.setStatus(TraceCodeStatus.INACTIVE); code.setShipment(parent); return code; }
    private Organization partner() { Organization o = new Organization(); o.setOrganizationId(UUID.randomUUID()); o.setCode("DN"); o.setName("Doanh nghiệp"); o.setType(OrganizationType.ENTERPRISE); o.setStatus(OrganizationStatus.ACTIVE); return o; }
    private SplitShipmentRequest request(Organization first, Organization second, long firstQuantity, long secondQuantity) {
        SplitShipmentAllocationRequest a = allocation(first.getOrganizationId(), firstQuantity, "HTX00000001", firstQuantity == 2 ? "HTX00000002" : "HTX00000001");
        SplitShipmentAllocationRequest b = allocation(second.getOrganizationId(), secondQuantity, "HTX00000003", "HTX00000004");
        SplitShipmentRequest request = new SplitShipmentRequest(); request.setAllocations(List.of(a, b)); return request;
    }
    private SplitShipmentAllocationRequest allocation(UUID recipient, long quantity, String from, String to) { SplitShipmentAllocationRequest a = new SplitShipmentAllocationRequest(); a.setRecipientOrganizationId(recipient); a.setName("Lô con"); a.setQuantity(quantity); a.setFromCode(from); a.setToCode(to); return a; }
}
