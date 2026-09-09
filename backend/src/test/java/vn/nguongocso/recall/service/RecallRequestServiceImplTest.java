package vn.nguongocso.recall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.dto.request.ApproveRecallRequest;
import vn.nguongocso.recall.dto.request.CreateRecallRequest;
import vn.nguongocso.recall.dto.request.RejectRecallRequest;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;
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
    private ProductFeedbackRepository productFeedbackRepository;
    private ShipmentRepository shipmentRepository;
    private ShipmentRecallService shipmentRecallService;
    private UserRepository userRepository;
    private ChainEventRepository chainEventRepository;
    private NotificationService notificationService;
    private OrganizationUserRepository organizationUserRepository;
    private RecallRequestServiceImpl service;
    private CustomUserDetails currentUser;
    private UUID organizationId;
    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        recallRequestRepository = mock(RecallRequestRepository.class);
        productFeedbackRepository = mock(ProductFeedbackRepository.class);
        shipmentRepository = mock(ShipmentRepository.class);
        shipmentRecallService = mock(ShipmentRecallService.class);
        userRepository = mock(UserRepository.class);
        chainEventRepository = mock(ChainEventRepository.class);
        notificationService = mock(NotificationService.class);
        organizationUserRepository = mock(OrganizationUserRepository.class);

        service = new RecallRequestServiceImpl(
                recallRequestRepository,
                productFeedbackRepository,
                shipmentRepository,
                shipmentRecallService,
                userRepository,
                organizationUserRepository,
                chainEventRepository,
                notificationService);

        organizationId = UUID.randomUUID();
        currentUserId = UUID.randomUUID();
        currentUser = mock(CustomUserDetails.class);
        when(currentUser.getOrganizationId()).thenReturn(organizationId);
        when(currentUser.getUserId()).thenReturn(currentUserId);
        when(recallRequestRepository.save(any(RecallRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_shouldSucceed_whenShipmentIsValid() {
        ProductionLot lot = productionLot();
        Shipment shipment = shipment(lot, "Lô hàng đạt chuẩn");
        CreateRecallRequest request = new CreateRecallRequest();
        request.setShipmentId(shipment.getId());
        request.setReason("Phát hiện lỗi bao bì");
        request.setEvidence("Ảnh chụp tem rách");

        User requester = User.builder().userId(currentUserId).fullName("Người tạo").build();
        when(shipmentRepository.findOwnedByIdForRecallUpdate(shipment.getId(), organizationId))
                .thenReturn(Optional.of(shipment));
        when(recallRequestRepository.existsByShipment_IdAndStatus(shipment.getId(), RecallRequestStatus.PENDING))
                .thenReturn(false);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(requester));

        RecallRequestResponse response = service.create(request, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getShipmentId()).isEqualTo(shipment.getId());
        assertThat(response.getShipmentName()).isEqualTo("Lô hàng đạt chuẩn");
        assertThat(response.getLotId()).isEqualTo(lot.getId());
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getReason()).isEqualTo("Phát hiện lỗi bao bì");
    }

    @Test
    void create_shouldThrow_whenShipmentAlreadyRecalled() {
        ProductionLot lot = productionLot();
        Shipment shipment = shipment(lot, "Lô hàng đã thu hồi");
        shipment.setStatus(ShipmentStatus.RECALLED);
        CreateRecallRequest request = new CreateRecallRequest();
        request.setShipmentId(shipment.getId());
        request.setReason("Thu hồi lại");

        when(shipmentRepository.findOwnedByIdForRecallUpdate(shipment.getId(), organizationId))
                .thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.create(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô hàng đã bị thu hồi trước đó.");
    }

    @Test
    void create_shouldThrow_whenPendingRecallExistsForShipment() {
        ProductionLot lot = productionLot();
        Shipment shipment = shipment(lot, "Lô hàng đang chờ duyệt");
        CreateRecallRequest request = new CreateRecallRequest();
        request.setShipmentId(shipment.getId());
        request.setReason("Lý do trùng");

        when(shipmentRepository.findOwnedByIdForRecallUpdate(shipment.getId(), organizationId))
                .thenReturn(Optional.of(shipment));
        when(recallRequestRepository.existsByShipment_IdAndStatus(shipment.getId(), RecallRequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô hàng này đã có yêu cầu thu hồi đang chờ duyệt.");
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
        when(shipmentRepository.findOwnedByIdForRecallUpdate(shipment.getId(), organizationId))
                .thenReturn(Optional.of(shipment));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(requester));

        service.createFromFeedback(feedback, null, "Nghi ngờ tem giả", null, currentUser);

        ArgumentCaptor<RecallRequest> captor = ArgumentCaptor.forClass(RecallRequest.class);
        verify(recallRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getShipment()).isSameAs(shipment);
        assertThat(captor.getValue().getProductionLot()).isSameAs(lot);
        assertThat(captor.getValue().getSourceFeedback()).isSameAs(feedback);
    }

    @Test
    void createFromFeedback_shouldThrow_whenTraceCodeShipmentDiffersFromRequested() {
        ProductionLot lot = productionLot();
        Shipment shipmentA = shipment(lot, "Lô hàng A");
        Shipment shipmentB = shipment(lot, "Lô hàng B");
        TraceCode traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setShipment(shipmentA);

        ProductFeedback feedback = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .productionLot(lot)
                .traceCode(traceCode)
                .build();

        assertThatThrownBy(() -> service.createFromFeedback(feedback, shipmentB.getId(), "Lý do", null, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô hàng phải là lô chứa mã tem của phản ánh.");
    }

    @Test
    void createFromFeedback_shouldThrow_whenNoTraceCodeAndNoShipmentSpecified() {
        ProductionLot lot = productionLot();
        ProductFeedback feedback = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .productionLot(lot)
                .traceCode(null)
                .build();

        assertThatThrownBy(() -> service.createFromFeedback(feedback, null, "Lý do", null, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Phải xác định lô hàng cần thu hồi.");
    }

    @Test
    void createFromFeedback_shouldThrow_whenShipmentDoesNotBelongToFeedbackLot() {
        ProductionLot lotA = productionLot();
        ProductionLot lotB = productionLot();
        lotB.setId(UUID.randomUUID());
        Shipment shipmentB = shipment(lotB, "Lô hàng thuộc lot B");

        ProductFeedback feedback = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .productionLot(lotA)
                .traceCode(null)
                .build();

        when(shipmentRepository.findOwnedByIdForRecallUpdate(shipmentB.getId(), organizationId))
                .thenReturn(Optional.of(shipmentB));

        assertThatThrownBy(() -> service.createFromFeedback(feedback, shipmentB.getId(), "Lý do", null, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô hàng không thuộc lô sản xuất của phản ánh.");
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
        when(chainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(List.of(selected.getId())))
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

    @Test
    void approve_shouldThrow_whenApproverIsRequester() {
        ProductionLot lot = productionLot();
        Shipment shipment = shipment(lot, "Lô hàng");
        User sameUser = User.builder().userId(currentUserId).fullName("Người tạo kiêm duyệt").build();
        RecallRequest request = new RecallRequest();
        request.setId(UUID.randomUUID());
        request.setProductionLot(lot);
        request.setShipment(shipment);
        request.setRequestedBy(sameUser);
        request.setStatus(RecallRequestStatus.PENDING);

        when(recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                request.getId(), organizationId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(request.getId(), new ApproveRecallRequest(), currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không thể duyệt yêu cầu do chính mình tạo.");
    }

    @Test
    void approve_shouldUpdateLinkedFeedbackStatusToEscalated() {
        ProductionLot lot = productionLot();
        Shipment shipment = shipment(lot, "Lô hàng");
        User requester = User.builder().userId(UUID.randomUUID()).fullName("Người tạo").build();
        User approver = User.builder().userId(currentUserId).fullName("Người duyệt").build();

        ProductFeedback feedback = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .status(ProductFeedbackStatus.IN_PROGRESS)
                .build();

        RecallRequest request = new RecallRequest();
        request.setId(UUID.randomUUID());
        request.setProductionLot(lot);
        request.setShipment(shipment);
        request.setSourceFeedback(feedback);
        request.setRequestedBy(requester);
        request.setReason("Lý do thu hồi");
        request.setStatus(RecallRequestStatus.PENDING);

        when(recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                request.getId(), organizationId)).thenReturn(Optional.of(request));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(approver));
        when(chainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(List.of(shipment.getId())))
                .thenReturn(List.of());

        service.approve(request.getId(), new ApproveRecallRequest(), currentUser);

        verify(productFeedbackRepository).save(feedback);
        assertThat(feedback.getStatus()).isEqualTo(ProductFeedbackStatus.ESCALATED_TO_RECALL);
    }

    @Test
    void approve_shouldNotifyOrganizationRecordedOnProcurementEvent() {
        ProductionLot lot = productionLot();
        Shipment shipment = shipment(lot, "Lô hàng");
        UUID buyerOrganizationId = UUID.randomUUID();
        UUID buyerUserId = UUID.randomUUID();
        User requester = User.builder().userId(UUID.randomUUID()).fullName("Người tạo").build();
        User approver = User.builder().userId(currentUserId).fullName("Người duyệt").build();
        User buyer = User.builder().userId(buyerUserId).fullName("Người mua").build();
        OrganizationUser buyerMembership = mock(OrganizationUser.class);
        when(buyerMembership.getUser()).thenReturn(buyer);

        RecallRequest request = new RecallRequest();
        request.setId(UUID.randomUUID());
        request.setProductionLot(lot);
        request.setShipment(shipment);
        request.setRequestedBy(requester);
        request.setReason("Lý do thu hồi");
        request.setStatus(RecallRequestStatus.PENDING);

        when(recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                request.getId(), organizationId)).thenReturn(Optional.of(request));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(approver));
        when(chainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(List.of(shipment.getId())))
                .thenReturn(List.of(buyerOrganizationId));
        when(organizationUserRepository.findByOrganization_OrganizationIdAndStatus(
                buyerOrganizationId, OrganizationUserStatus.ACTIVE))
                .thenReturn(List.of(buyerMembership));
        when(notificationService.sendRecallNotification(
                shipment.getName(), request.getReason(), List.of(buyerUserId))).thenReturn(1);

        RecallRequestResponse response = service.approve(
                request.getId(), new ApproveRecallRequest(), currentUser);

        assertThat(response.getNotifiedBuyerCount()).isEqualTo(1);
        verify(notificationService).sendRecallNotification(
                shipment.getName(), request.getReason(), List.of(buyerUserId));
    }

    @Test
    void reject_shouldRevertFeedbackStatusToInProgress_whenEscalated() {
        ProductionLot lot = productionLot();
        Shipment shipment = shipment(lot, "Lô hàng");
        User requester = User.builder().userId(UUID.randomUUID()).fullName("Người tạo").build();
        User rejecter = User.builder().userId(currentUserId).fullName("Người từ chối").build();

        ProductFeedback feedback = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .status(ProductFeedbackStatus.ESCALATED_TO_RECALL)
                .build();

        RecallRequest request = new RecallRequest();
        request.setId(UUID.randomUUID());
        request.setProductionLot(lot);
        request.setShipment(shipment);
        request.setSourceFeedback(feedback);
        request.setRequestedBy(requester);
        request.setStatus(RecallRequestStatus.PENDING);

        RejectRecallRequest rejectRequest = new RejectRecallRequest();
        rejectRequest.setRejectionReason("Chưa đủ bằng chứng xác thực");

        when(recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                request.getId(), organizationId)).thenReturn(Optional.of(request));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(rejecter));

        RecallRequestResponse response = service.reject(request.getId(), rejectRequest, currentUser);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Chưa đủ bằng chứng xác thực");
        verify(productFeedbackRepository).save(feedback);
        assertThat(feedback.getStatus()).isEqualTo(ProductFeedbackStatus.IN_PROGRESS);
    }

    @Test
    void reject_shouldThrow_whenRejectionReasonIsBlank() {
        RecallRequest request = new RecallRequest();
        request.setId(UUID.randomUUID());
        request.setStatus(RecallRequestStatus.PENDING);

        when(recallRequestRepository.findByIdAndProductionLot_Organization_OrganizationId(
                request.getId(), organizationId)).thenReturn(Optional.of(request));

        RejectRecallRequest rejectRequest = new RejectRecallRequest();
        rejectRequest.setRejectionReason("   ");

        assertThatThrownBy(() -> service.reject(request.getId(), rejectRequest, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lý do từ chối không được để trống.");
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
