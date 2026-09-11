package vn.nguongocso.recall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.dto.request.ApproveBulkRecallRequest;
import vn.nguongocso.recall.dto.request.CreateBulkRecallRequest;
import vn.nguongocso.recall.dto.request.RejectBulkRecallRequest;
import vn.nguongocso.recall.dto.response.BulkRecallRequestResponse;
import vn.nguongocso.recall.dto.response.BulkRecallShipmentItem;
import vn.nguongocso.recall.entity.BulkRecallRequest;
import vn.nguongocso.recall.entity.BulkRecallShipment;
import vn.nguongocso.recall.enums.BulkRecallRequestStatus;
import vn.nguongocso.recall.repository.BulkRecallRequestRepository;
import vn.nguongocso.recall.repository.BulkRecallShipmentRepository;
import vn.nguongocso.recall.service.BulkRecallNotificationService;
import vn.nguongocso.recall.service.impl.BulkRecallRequestServiceImpl;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.dto.request.RecallRequest;
import vn.nguongocso.trace.service.ShipmentRecallService;

/**
 * Unit tests cho BulkRecallRequestServiceImpl (NCL-08-CN-011).
 */
class BulkRecallRequestServiceImplTest {

    private BulkRecallRequestRepository bulkRecallRequestRepository;
    private BulkRecallShipmentRepository bulkRecallShipmentRepository;
    private ProductionLotRepository productionLotRepository;
    private ShipmentRepository shipmentRepository;
    private UserRepository userRepository;
    private OrganizationUserRepository organizationUserRepository;
    private ChainEventRepository chainEventRepository;
    private ShipmentRecallService shipmentRecallService;
    private NotificationService notificationService;
    private BulkRecallNotificationService bulkRecallNotificationService;
    private ActivityLogService activityLogService;
    private vn.nguongocso.trace.recall.repository.RecallCaseRepository recallCaseRepository;
    private vn.nguongocso.trace.recall.repository.RecallLotResultRepository recallLotResultRepository;
    private vn.nguongocso.trace.repository.TraceCodeRepository traceCodeRepository;
    private vn.nguongocso.trace.repository.CodeRangeRepository codeRangeRepository;
    private vn.nguongocso.recall.repository.RecallEvidenceFileRepository recallEvidenceFileRepository;
    private BulkRecallRequestServiceImpl service;

    private UUID organizationId;
    private UUID userId;
    private UUID otherUserId;
    private UUID productionLotId;
    private UUID shipmentId1;
    private UUID shipmentId2;
    private UUID shipmentId3;
    private CustomUserDetails currentUser;
    private CustomUserDetails otherManager;
    private Organization organization;
    private ProductionLot productionLot;
    private Shipment shipment1;
    private Shipment shipment2;
    private Shipment shipment3;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        bulkRecallRequestRepository = mock(BulkRecallRequestRepository.class);
        bulkRecallShipmentRepository = mock(BulkRecallShipmentRepository.class);
        productionLotRepository = mock(ProductionLotRepository.class);
        shipmentRepository = mock(ShipmentRepository.class);
        userRepository = mock(UserRepository.class);
        organizationUserRepository = mock(OrganizationUserRepository.class);
        chainEventRepository = mock(ChainEventRepository.class);
        shipmentRecallService = mock(ShipmentRecallService.class);
        notificationService = mock(NotificationService.class);
        bulkRecallNotificationService = mock(BulkRecallNotificationService.class);
        activityLogService = mock(ActivityLogService.class);
        recallCaseRepository = mock(vn.nguongocso.trace.recall.repository.RecallCaseRepository.class);
        recallLotResultRepository = mock(vn.nguongocso.trace.recall.repository.RecallLotResultRepository.class);
        traceCodeRepository = mock(vn.nguongocso.trace.repository.TraceCodeRepository.class);
        codeRangeRepository = mock(vn.nguongocso.trace.repository.CodeRangeRepository.class);
        recallEvidenceFileRepository = mock(vn.nguongocso.recall.repository.RecallEvidenceFileRepository.class);

        service = new BulkRecallRequestServiceImpl(
                bulkRecallRequestRepository,
                bulkRecallShipmentRepository,
                productionLotRepository,
                shipmentRepository,
                userRepository,
                organizationUserRepository,
                chainEventRepository,
                shipmentRecallService,
                notificationService,
                bulkRecallNotificationService,
                activityLogService,
                recallCaseRepository,
                recallLotResultRepository,
                traceCodeRepository,
                codeRangeRepository,
                recallEvidenceFileRepository);

        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        productionLotId = UUID.randomUUID();
        shipmentId1 = UUID.randomUUID();
        shipmentId2 = UUID.randomUUID();
        shipmentId3 = UUID.randomUUID();

        currentUser = mock(CustomUserDetails.class);
        when(currentUser.getUserId()).thenReturn(userId);
        when(currentUser.getOrganizationId()).thenReturn(organizationId);
        when(currentUser.getRoleCode()).thenReturn("VT-02");

        otherManager = mock(CustomUserDetails.class);
        when(otherManager.getUserId()).thenReturn(otherUserId);
        when(otherManager.getOrganizationId()).thenReturn(organizationId);
        when(otherManager.getRoleCode()).thenReturn("VT-02");

        organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setName("HTX Test");

        user = new User();
        user.setUserId(userId);
        user.setFullName("Manager A");
        user.setUserName("manager_a");

        otherUser = new User();
        otherUser.setUserId(otherUserId);
        otherUser.setFullName("Manager B");
        otherUser.setUserName("manager_b");

        productionLot = new ProductionLot();
        productionLot.setId(productionLotId);
        productionLot.setName("Lo san xuat test");
        productionLot.setOrganization(organization);

        shipment1 = createShipment(shipmentId1, "SHIP-001", ShipmentStatus.ACTIVATED);
        shipment2 = createShipment(shipmentId2, "SHIP-002", ShipmentStatus.ACTIVATED);
        shipment3 = createShipment(shipmentId3, "SHIP-003", ShipmentStatus.ACTIVATED);

        // Mock OrganizationUserRepository for logActivity
        OrganizationUser orgUser = mock(OrganizationUser.class);
        when(orgUser.getOrganization()).thenReturn(organization);
        when(organizationUserRepository.findFirstByUser(any(User.class))).thenReturn(Optional.of(orgUser));
    }

    private Shipment createShipment(UUID id, String name, ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setId(id);
        shipment.setName(name);
        shipment.setStatus(status);
        shipment.setProductionLot(productionLot);
        shipment.setOrganization(organization);
        shipment.setTotalQuantity(100L);
        return shipment;
    }

    // =========================================================
    // Test: createBulkRecallRequest
    // =========================================================

    @Test
    @DisplayName("TC-01: Tao de nghi thanh cong voi nhieu lo hang")
    void createBulkRecallRequest_Success_WithMultipleShipments() {
        CreateBulkRecallRequest request = new CreateBulkRecallRequest();
        request.setProductionLotId(productionLotId);
        request.setReason("Phat hien nhieu khuan");
        request.setEvidence("Bien ban kiem nghiem");
        request.setIncludedShipmentIds(List.of(shipmentId1, shipmentId2, shipmentId3));

        when(productionLotRepository.findById(productionLotId)).thenReturn(Optional.of(productionLot));
        when(bulkRecallRequestRepository.existsByProductionLot_IdAndStatus(
                productionLotId, BulkRecallRequestStatus.PENDING)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bulkRecallRequestRepository.save(any(BulkRecallRequest.class)))
                .thenAnswer(invocation -> {
                    BulkRecallRequest saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    saved.setCreatedAt(LocalDateTime.now());
                    saved.setUpdatedAt(LocalDateTime.now());
                    return saved;
                });
        when(shipmentRepository.findById(shipmentId1)).thenReturn(Optional.of(shipment1));
        when(shipmentRepository.findById(shipmentId2)).thenReturn(Optional.of(shipment2));
        when(shipmentRepository.findById(shipmentId3)).thenReturn(Optional.of(shipment3));
        // Mock findByBulkRecallRequestId to return empty list - we'll verify the save happened
        when(bulkRecallShipmentRepository.findByBulkRecallRequestId(any()))
                .thenReturn(List.of());

        BulkRecallRequestResponse response = service.createBulkRecallRequest(request, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getReason()).isEqualTo("Phat hien nhieu khuan");
        assertThat(response.getProductionLotId()).isEqualTo(productionLotId);

        verify(bulkRecallRequestRepository).save(any(BulkRecallRequest.class));
        verify(bulkRecallShipmentRepository).saveAll(any());
    }

    @Test
    @DisplayName("TC-02: Tao de nghi that bai - ly do trong")
    void createBulkRecallRequest_Fail_EmptyReason() {
        CreateBulkRecallRequest request = new CreateBulkRecallRequest();
        request.setProductionLotId(productionLotId);
        request.setReason("");
        request.setIncludedShipmentIds(List.of(shipmentId1));

        assertThatThrownBy(() -> service.createBulkRecallRequest(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lý do thu hồi không được để trống");

        verify(bulkRecallRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Tao de nghi that bai - khong co lo hang nao")
    void createBulkRecallRequest_Fail_EmptyShipments() {
        CreateBulkRecallRequest request = new CreateBulkRecallRequest();
        request.setProductionLotId(productionLotId);
        request.setReason("Ly do test");
        request.setIncludedShipmentIds(List.of());

        assertThatThrownBy(() -> service.createBulkRecallRequest(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Phải chọn ít nhất một lô hàng");

        verify(bulkRecallRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Tao de nghi that bai - lo hang da RECALLED")
    void createBulkRecallRequest_Fail_AlreadyRecalled() {
        Shipment recalledShipment = createShipment(shipmentId1, "SHIP-001", ShipmentStatus.RECALLED);

        CreateBulkRecallRequest request = new CreateBulkRecallRequest();
        request.setProductionLotId(productionLotId);
        request.setReason("Ly do test");
        request.setIncludedShipmentIds(List.of(shipmentId1));

        when(productionLotRepository.findById(productionLotId)).thenReturn(Optional.of(productionLot));
        when(bulkRecallRequestRepository.existsByProductionLot_IdAndStatus(
                productionLotId, BulkRecallRequestStatus.PENDING)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentRepository.findById(shipmentId1)).thenReturn(Optional.of(recalledShipment));

        assertThatThrownBy(() -> service.createBulkRecallRequest(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã được thu hồi trước đó");

        verify(bulkRecallRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04b: Tao de nghi that bai - lo hang dang RECALLING")
    void createBulkRecallRequest_Fail_AlreadyRecalling() {
        Shipment recallingShipment = createShipment(shipmentId1, "SHIP-001", ShipmentStatus.RECALLING);

        CreateBulkRecallRequest request = new CreateBulkRecallRequest();
        request.setProductionLotId(productionLotId);
        request.setReason("Ly do test");
        request.setIncludedShipmentIds(List.of(shipmentId1));

        when(productionLotRepository.findById(productionLotId)).thenReturn(Optional.of(productionLot));
        when(bulkRecallRequestRepository.existsByProductionLot_IdAndStatus(
                productionLotId, BulkRecallRequestStatus.PENDING)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentRepository.findById(shipmentId1)).thenReturn(Optional.of(recallingShipment));

        assertThatThrownBy(() -> service.createBulkRecallRequest(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã được thu hồi trước đó");

        verify(bulkRecallRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-05: Tao de nghi that bai - to chuc khac")
    void createBulkRecallRequest_Fail_OrganizationMismatch() {
        UUID otherOrgId = UUID.randomUUID();
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(otherOrgId);

        ProductionLot otherLot = new ProductionLot();
        otherLot.setId(productionLotId);
        otherLot.setName("Lo khac");
        otherLot.setOrganization(otherOrg);

        CreateBulkRecallRequest request = new CreateBulkRecallRequest();
        request.setProductionLotId(productionLotId);
        request.setReason("Ly do test");
        request.setIncludedShipmentIds(List.of(shipmentId1));

        when(productionLotRepository.findById(productionLotId)).thenReturn(Optional.of(otherLot));

        assertThatThrownBy(() -> service.createBulkRecallRequest(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tổ chức khác");

        verify(bulkRecallRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-06: Tao de nghi that bai - da co de nghi PENDING")
    void createBulkRecallRequest_Fail_PendingExists() {
        CreateBulkRecallRequest request = new CreateBulkRecallRequest();
        request.setProductionLotId(productionLotId);
        request.setReason("Ly do test");
        request.setIncludedShipmentIds(List.of(shipmentId1));

        when(productionLotRepository.findById(productionLotId)).thenReturn(Optional.of(productionLot));
        when(bulkRecallRequestRepository.existsByProductionLot_IdAndStatus(
                productionLotId, BulkRecallRequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.createBulkRecallRequest(request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Đã có yêu cầu thu hồi đang chờ duyệt");

        verify(bulkRecallRequestRepository, never()).save(any());
    }

    // =========================================================
    // Test: approveBulkRecallRequest
    // =========================================================

    @Test
    @DisplayName("TC-07: Phep duyet that bai - tu phep duyet (QTN-22)")
    void approveBulkRecallRequest_Fail_SelfApproval() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.PENDING);

        ApproveBulkRecallRequest request = new ApproveBulkRecallRequest();

        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));

        assertThatThrownBy(() -> service.approveBulkRecallRequest(requestId, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do chính mình tạo");

        verify(shipmentRecallService, never()).recallShipment(any(), any(), any());
    }

    @Test
    @DisplayName("TC-08: Phep duyet that bai - trang thai khong phai PENDING")
    void approveBulkRecallRequest_Fail_NotPending() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.APPROVED);

        ApproveBulkRecallRequest request = new ApproveBulkRecallRequest();

        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));

        assertThatThrownBy(() -> service.approveBulkRecallRequest(requestId, request, otherManager))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING");

        verify(shipmentRecallService, never()).recallShipment(any(), any(), any());
    }

    @Test
    @DisplayName("TC-08b: Phe duyet that bai - lo hang dang trong trang thai RECALLING")
    void approveBulkRecallRequest_Fail_ShipmentAlreadyRecalling() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.PENDING);

        ApproveBulkRecallRequest request = new ApproveBulkRecallRequest();
        Shipment recallingShipment = createShipment(shipmentId1, "SHIP-001", ShipmentStatus.RECALLING);
        BulkRecallShipment record1 = createBulkRecallShipment(bulkRequest, recallingShipment, true);

        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(bulkRecallShipmentRepository.findByBulkRecallRequestIdAndIncluded(requestId, true))
                .thenReturn(List.of(record1));
        when(shipmentRepository.findOwnedByIdForRecallUpdate(shipmentId1, organizationId))
                .thenReturn(Optional.of(recallingShipment));

        assertThatThrownBy(() -> service.approveBulkRecallRequest(requestId, request, otherManager))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bởi một yêu cầu khác");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST 1: Manager B cung to chuc phe duyet thanh cong - cac lo hang chuyen RECALLING")
    void approveBulkRecallRequest_Success_ByOtherManagerSameOrganization() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.PENDING);

        ApproveBulkRecallRequest request = new ApproveBulkRecallRequest();
        request.setRemarks("Đã xác minh phạm vi ảnh hưởng");

        BulkRecallShipment record1 = createBulkRecallShipment(bulkRequest, shipment1, true);
        BulkRecallShipment record2 = createBulkRecallShipment(bulkRequest, shipment2, true);

        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));
        when(bulkRecallShipmentRepository.findByBulkRecallRequestIdAndIncluded(requestId, true))
                .thenReturn(List.of(record1, record2));
        when(shipmentRepository.findOwnedByIdForRecallUpdate(shipmentId1, organizationId))
                .thenReturn(Optional.of(shipment1));
        when(shipmentRepository.findOwnedByIdForRecallUpdate(shipmentId2, organizationId))
                .thenReturn(Optional.of(shipment2));
        when(chainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(any()))
                .thenReturn(List.of());
        when(bulkRecallShipmentRepository.findByBulkRecallRequestId(requestId))
                .thenReturn(List.of(record1, record2));
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(recallCaseRepository.existsByProductionLotId(any())).thenReturn(false);

        BulkRecallRequestResponse response =
                service.approveBulkRecallRequest(requestId, request, otherManager);

        // Yêu cầu chuyển APPROVED, người duyệt là Manager B (khác người tạo)
        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getApprovedBy().getUserId()).isEqualTo(otherUserId);

        ArgumentCaptor<BulkRecallRequest> savedCaptor = ArgumentCaptor.forClass(BulkRecallRequest.class);
        verify(bulkRecallRequestRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(BulkRecallRequestStatus.APPROVED);

        // Các lô included chuyển sang trạng thái RECALLING và được lưu
        assertThat(shipment1.getStatus()).isEqualTo(ShipmentStatus.RECALLING);
        assertThat(shipment2.getStatus()).isEqualTo(ShipmentStatus.RECALLING);
        verify(shipmentRepository).save(shipment1);
        verify(shipmentRepository).save(shipment2);

        // Tạo mới RecallCase ở trạng thái OPEN
        verify(recallCaseRepository).save(any(vn.nguongocso.trace.recall.entity.RecallCase.class));
    }

    @Test
    @DisplayName("TEST 4: Manager thuoc to chuc khac phe duyet - that bai, khong thu hồi lô nào")
    void approveBulkRecallRequest_Fail_OtherOrganization() {
        UUID requestId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();

        Organization otherOrganization = new Organization();
        otherOrganization.setOrganizationId(otherOrgId);
        otherOrganization.setName("HTX Khác");

        ProductionLot otherLot = new ProductionLot();
        otherLot.setId(productionLotId);
        otherLot.setName("Lo cua to chuc khac");
        otherLot.setOrganization(otherOrganization);

        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.PENDING);
        bulkRequest.setProductionLot(otherLot);

        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));

        assertThatThrownBy(() -> service
                .approveBulkRecallRequest(requestId, new ApproveBulkRecallRequest(), otherManager))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tổ chức khác");

        verify(shipmentRecallService, never()).recallShipment(any(), any(), any());
        verify(bulkRecallRequestRepository, never()).save(any());
    }

    // =========================================================
    // Test: rejectBulkRecallRequest
    // =========================================================

    @Test
    @DisplayName("TC-09: Tu choi thanh cong")
    void rejectBulkRecallRequest_Success() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.PENDING);

        RejectBulkRecallRequest request = new RejectBulkRecallRequest();
        request.setReason("Khong du bang chung");

        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        BulkRecallRequestResponse response = service.rejectBulkRecallRequest(requestId, request, otherManager);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Khong du bang chung");
        assertThat(response.getRejectedBy().getUserId()).isEqualTo(otherUserId);

        verify(shipmentRecallService, never()).recallShipment(any(), any(), any());
    }

    @Test
    @DisplayName("TC-10: Tu choi that bai - ly do trong")
    void rejectBulkRecallRequest_Fail_EmptyReason() {
        UUID requestId = UUID.randomUUID();
        RejectBulkRecallRequest request = new RejectBulkRecallRequest();
        request.setReason("");

        assertThatThrownBy(() -> service.rejectBulkRecallRequest(requestId, request, otherManager))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lý do từ chối");

        verify(bulkRecallRequestRepository, never()).findByIdWithLock(any());
    }

    // =========================================================
    // NCL-08-CN-012: Tests cho closeBulkRecallRequest
    // =========================================================

    @Test
    @DisplayName("TC-11: Ket thuc vu viec thanh cong - trang thai chuyen sang COMPLETED")
    void closeBulkRecallRequest_Success() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.APPROVED);

        BulkRecallShipment bs1 = createBulkRecallShipment(bulkRequest, shipment1, true);
        BulkRecallShipment bs2 = createBulkRecallShipment(bulkRequest, shipment2, true);

        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));
        when(bulkRecallShipmentRepository.findByBulkRecallRequestIdAndIncluded(requestId, true))
                .thenReturn(List.of(bs1, bs2));
        when(bulkRecallShipmentRepository.findByBulkRecallRequestId(requestId))
                .thenReturn(List.of(bs1, bs2));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        RecallCase mockCase = RecallCase.builder()
                .id(UUID.randomUUID())
                .caseCode("RC-20260911-001")
                .productionLot(productionLot)
                .organizationId(organizationId)
                .status(RecallCaseStatus.OPEN)
                .build();
        when(recallCaseRepository.findByProductionLotId(productionLotId)).thenReturn(Optional.of(mockCase));
        when(recallCaseRepository.save(any(RecallCase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bulkRecallRequestRepository.save(any(BulkRecallRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CloseRecallCaseRequest request = new CloseRecallCaseRequest();
        request.setRemediationMeasures("Thực hiện kiểm tra dư lượng trước khi thu hoạch");

        CloseRecallCaseRequest.LotResultItem item1 = new CloseRecallCaseRequest.LotResultItem();
        item1.setShipmentId(shipmentId1);
        item1.setResolution(vn.nguongocso.trace.recall.enums.LotResolution.DESTROYED);
        item1.setRecoveredQuantity(BigDecimal.valueOf(50));

        CloseRecallCaseRequest.LotResultItem item2 = new CloseRecallCaseRequest.LotResultItem();
        item2.setShipmentId(shipmentId2);
        item2.setResolution(vn.nguongocso.trace.recall.enums.LotResolution.RETURNED);
        item2.setRecoveredQuantity(BigDecimal.valueOf(50));

        request.setLotResults(List.of(item1, item2));

        BulkRecallRequestResponse response = service.closeBulkRecallRequest(requestId, request, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BulkRecallRequestStatus.COMPLETED.name());
        assertThat(bulkRequest.getStatus()).isEqualTo(BulkRecallRequestStatus.COMPLETED);
        assertThat(shipment1.getStatus()).isEqualTo(ShipmentStatus.RECALLED);
        assertThat(shipment2.getStatus()).isEqualTo(ShipmentStatus.RECALLED);
        assertThat(mockCase.getStatus()).isEqualTo(RecallCaseStatus.CLOSED);

        verify(recallLotResultRepository).saveAll(any());
        verify(bulkRecallRequestRepository).save(bulkRequest);
    }

    @Test
    @DisplayName("TC-12: Ket thuc vu viec that bai - khong phai vai tro VT-02")
    void closeBulkRecallRequest_Fail_NotVT02() {
        UUID requestId = UUID.randomUUID();
        CustomUserDetails nonManager = mock(CustomUserDetails.class);
        when(nonManager.getRoleCode()).thenReturn("VT-03");

        CloseRecallCaseRequest request = new CloseRecallCaseRequest();

        assertThatThrownBy(() -> service.closeBulkRecallRequest(requestId, request, nonManager))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không có quyền");
    }

    @Test
    @DisplayName("TC-13: Ket thuc vu viec that bai - yeu cau chua duoc duyet")
    void closeBulkRecallRequest_Fail_NotApproved() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.PENDING);
        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));

        CloseRecallCaseRequest request = new CloseRecallCaseRequest();

        assertThatThrownBy(() -> service.closeBulkRecallRequest(requestId, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ có thể kết thúc vụ việc khi yêu cầu ở trạng thái Đã duyệt");
    }

    @Test
    @DisplayName("TC-14: Ket thuc vu viec that bai - thieu bien phap khac phuc (QTN-27)")
    void closeBulkRecallRequest_Fail_EmptyRemediation() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.APPROVED);
        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));

        CloseRecallCaseRequest request = new CloseRecallCaseRequest();
        request.setRemediationMeasures("   ");

        assertThatThrownBy(() -> service.closeBulkRecallRequest(requestId, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Biện pháp khắc phục phòng ngừa là bắt buộc");
    }

    @Test
    @DisplayName("TC-15: Ket thuc vu viec that bai - con thieu ket qua xu ly cho lo trong pham vi")
    void closeBulkRecallRequest_Fail_MissingLotResult() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.APPROVED);

        BulkRecallShipment bs1 = createBulkRecallShipment(bulkRequest, shipment1, true);
        BulkRecallShipment bs2 = createBulkRecallShipment(bulkRequest, shipment2, true);

        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));
        when(bulkRecallShipmentRepository.findByBulkRecallRequestIdAndIncluded(requestId, true))
                .thenReturn(List.of(bs1, bs2));

        CloseRecallCaseRequest request = new CloseRecallCaseRequest();
        request.setRemediationMeasures("Biện pháp kiểm tra định kỳ");

        // Chỉ nhập cho shipment1, bỏ sót shipment2
        CloseRecallCaseRequest.LotResultItem item1 = new CloseRecallCaseRequest.LotResultItem();
        item1.setShipmentId(shipmentId1);
        item1.setResolution(vn.nguongocso.trace.recall.enums.LotResolution.DESTROYED);
        item1.setRecoveredQuantity(BigDecimal.valueOf(50));

        request.setLotResults(List.of(item1));

        assertThatThrownBy(() -> service.closeBulkRecallRequest(requestId, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Còn 1 lô chưa có kết quả xử lý");
    }

    @Test
    @DisplayName("closeBulkRecallRequest - Thất bại khi vượt quá 5 tệp biên bản đính kèm")
    void closeBulkRecallRequest_Fail_ExceedMaxEvidenceFiles() {
        UUID requestId = UUID.randomUUID();
        BulkRecallRequest bulkRequest = createBulkRecallRequestEntity(requestId, BulkRecallRequestStatus.APPROVED);
        when(bulkRecallRequestRepository.findByIdWithLock(requestId)).thenReturn(Optional.of(bulkRequest));

        CloseRecallCaseRequest request = new CloseRecallCaseRequest();
        request.setRemediationMeasures("Biện pháp phòng ngừa hợp lệ");
        request.setEvidenceFileIds(List.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        ));

        assertThatThrownBy(() -> service.closeBulkRecallRequest(requestId, request, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ được đính kèm tối đa 5 tệp biên bản thu hồi.");
    }

    @Test
    @DisplayName("getEvidenceFile - Thất bại khi tệp không tồn tại trong DB")
    void getEvidenceFile_Fail_NotFound() {
        UUID fileId = UUID.randomUUID();
        when(recallEvidenceFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEvidenceFile(fileId, currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy tệp biên bản thu hồi.");
    }

    // =========================================================
    // Helper methods
    // =========================================================

    private BulkRecallRequest createBulkRecallRequestEntity(UUID id, BulkRecallRequestStatus status) {
        BulkRecallRequest entity = new BulkRecallRequest();
        entity.setId(id);
        entity.setProductionLot(productionLot);
        entity.setReason("Ly do test");
        entity.setStatus(status);
        entity.setRequestedBy(user);
        entity.setRequestedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private BulkRecallShipment createBulkRecallShipment(BulkRecallRequest request, Shipment shipment, boolean included) {
        BulkRecallShipment record = new BulkRecallShipment();
        record.setId(UUID.randomUUID());
        record.setBulkRecallRequest(request);
        record.setShipment(shipment);
        record.setIncluded(included);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }
}
