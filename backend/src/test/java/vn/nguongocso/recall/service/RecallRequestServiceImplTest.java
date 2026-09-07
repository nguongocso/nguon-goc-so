package vn.nguongocso.recall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.dto.request.ApproveRecallRequest;
import vn.nguongocso.recall.entity.RecallRequest;
import vn.nguongocso.recall.enums.RecallRequestStatus;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.recall.service.impl.RecallRequestServiceImpl;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.service.ShipmentRecallService;

class RecallRequestServiceImplTest {

    private RecallRequestRepository recallRequestRepository;
    private ShipmentRepository shipmentRepository;
    private ShipmentRecallService shipmentRecallService;
    private UserRepository userRepository;
    private ChainEventRepository chainEventRepository;
    private RecallRequestServiceImpl service;
    private CustomUserDetails currentUser;
    private UUID organizationId;
    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        recallRequestRepository = mock(RecallRequestRepository.class);
        shipmentRepository = mock(ShipmentRepository.class);
        shipmentRecallService = mock(ShipmentRecallService.class);
        userRepository = mock(UserRepository.class);
        chainEventRepository = mock(ChainEventRepository.class);
        service = new RecallRequestServiceImpl(
                recallRequestRepository,
                mock(ProductFeedbackRepository.class),
                shipmentRepository,
                shipmentRecallService,
                userRepository,
                mock(OrganizationUserRepository.class),
                chainEventRepository,
                mock(NotificationService.class));

        organizationId = UUID.randomUUID();
        currentUserId = UUID.randomUUID();
        currentUser = mock(CustomUserDetails.class);
        when(currentUser.getOrganizationId()).thenReturn(organizationId);
        when(currentUser.getUserId()).thenReturn(currentUserId);
        when(recallRequestRepository.save(any(RecallRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createFromFeedback_shouldUseShipmentContainingLinkedTraceCode() {
        ProductionLot lot = productionLot();
        Shipment shipment = shipment(lot, "Lô hàng chứa tem");
        TraceCode traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setShipment(shipment);
        ProductFeedback feedback = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .productionLot(lot)
                .traceCode(traceCode)
                .build();
        User requester = User.builder().userId(currentUserId).fullName("Người tạo").build();
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(requester));

        service.createFromFeedback(feedback, null, "Nghi ngờ tem giả", null, currentUser);

        ArgumentCaptor<RecallRequest> captor = ArgumentCaptor.forClass(RecallRequest.class);
        verify(recallRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getShipment()).isSameAs(shipment);
        assertThat(captor.getValue().getProductionLot()).isSameAs(lot);
    }

    @Test
    void approve_shouldRecallOnlySelectedShipmentAndKeepProductionLotStatus() {
        ProductionLot lot = productionLot();
        Shipment selected = shipment(lot, "Lô hàng cần thu hồi");
        Shipment other = shipment(lot, "Lô hàng không liên quan");
        User requester = User.builder().userId(UUID.randomUUID()).fullName("Người tạo").build();
        User approver = User.builder().userId(currentUserId).fullName("Người duyệt").build();
        RecallRequest request = new RecallRequest();
        request.setId(UUID.randomUUID());
        request.setProductionLot(lot);
        request.setShipment(selected);
        request.setRequestedBy(requester);
        request.setReason("Kết quả xác minh không đạt");
        request.setStatus(RecallRequestStatus.PENDING);

        when(recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                request.getId(), organizationId)).thenReturn(Optional.of(request));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(approver));
        when(chainEventRepository.findDistinctProcurementRecorderIdsByShipmentIds(List.of(selected.getId())))
                .thenReturn(List.of());

        service.approve(request.getId(), new ApproveRecallRequest(), currentUser);

        verify(shipmentRecallService).recallShipment(
                org.mockito.ArgumentMatchers.eq(selected.getId()),
                any(vn.nguongocso.trace.dto.request.RecallRequest.class),
                org.mockito.ArgumentMatchers.isNull());
        verify(shipmentRepository, never()).save(other);
        assertThat(lot.getStatus()).isEqualTo(ProductionLotStatus.APPROVED);
        assertThat(other.getStatus()).isEqualTo(ShipmentStatus.ACTIVATED);
        assertThat(request.getStatus()).isEqualTo(RecallRequestStatus.APPROVED);
    }

    private ProductionLot productionLot() {
        Organization organization = Organization.builder()
                .organizationId(organizationId)
                .name("HTX kiểm thử")
                .build();
        return ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô sản xuất")
                .organization(organization)
                .status(ProductionLotStatus.APPROVED)
                .build();
    }

    private Shipment shipment(ProductionLot lot, String name) {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName(name);
        shipment.setProductionLot(lot);
        shipment.setOrganization(lot.getOrganization());
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        return shipment;
    }
}
