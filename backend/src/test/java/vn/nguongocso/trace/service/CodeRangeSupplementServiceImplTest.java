package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.dto.request.ApproveSupplementRequest;
import vn.nguongocso.trace.dto.request.CreateSupplementRequest;
import vn.nguongocso.trace.dto.request.RejectSupplementRequest;
import vn.nguongocso.trace.dto.response.CodeRangeSupplementResponse;
import vn.nguongocso.trace.dto.response.EvidenceEventResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.CodeRangeSupplementRequest;
import vn.nguongocso.trace.enums.CodeRangeSupplementStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.CodeRangeSupplementRepository;
import vn.nguongocso.trace.service.impl.CodeRangeSupplementServiceImpl;

/**
 * Kiểm thử dịch vụ yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 *
 * <p>Bao phủ TC-01..TC-07: tạo yêu cầu, duyệt một phần, chống trùng PENDING,
 * phân quyền, biên số lượng/trạng thái, tổ chức chưa có dải mã, biên tổ chức.</p>
 */
@ExtendWith(MockitoExtension.class)
class CodeRangeSupplementServiceImplTest {

    @Mock
    private CodeRangeSupplementRepository supplementRepository;

    @Mock
    private CodeRangeRepository codeRangeRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CodeRangeSupplementServiceImpl supplementService;

    private UUID organizationId;
    private UUID userId;
    private UUID adminId;
    private Organization organization;
    private User requester;
    private CustomUserDetails managerUser;
    private CustomUserDetails adminUser;
    private CustomUserDetails recorderUser;
    private CodeRange codeRange;
    private ChainEvent harvestEvent;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setName("HTX Nong San Xanh");

        requester = new User();
        requester.setUserId(userId);
        requester.setFullName("Nguyen Van A");

        managerUser = mock(CustomUserDetails.class);
        lenient().when(managerUser.getUserId()).thenReturn(userId);
        lenient().when(managerUser.getOrganizationId()).thenReturn(organizationId);
        lenient().when(managerUser.getRoleCode()).thenReturn("VT-02");

        adminUser = mock(CustomUserDetails.class);
        lenient().when(adminUser.getUserId()).thenReturn(adminId);
        lenient().when(adminUser.getOrganizationId()).thenReturn(UUID.randomUUID());
        lenient().when(adminUser.getRoleCode()).thenReturn("VT-01");

        recorderUser = mock(CustomUserDetails.class);
        lenient().when(recorderUser.getUserId()).thenReturn(UUID.randomUUID());
        lenient().when(recorderUser.getOrganizationId()).thenReturn(organizationId);
        lenient().when(recorderUser.getRoleCode()).thenReturn("VT-03");

        codeRange = new CodeRange();
        codeRange.setId(UUID.randomUUID());
        codeRange.setOrganization(organization);
        codeRange.setPrefix("893001");
        codeRange.setTotalLimit(1000L);
        codeRange.setUsedCount(950L);

        harvestEvent = harvestEventOf(UUID.randomUUID(), organization);
    }

    // TC-01: hạn mức còn 50, VT-02 gửi yêu cầu 500 mã + lý do + bằng chứng
    @Test
    void create_tc01_success() {
        stubCreateHappyPath(harvestEvent);

        CreateSupplementRequest request = new CreateSupplementRequest();
        request.setRequestedQuantity(500L);
        request.setReason("Vu thu dong san luong cao, can them tem.");
        request.setEvidenceEventIds(List.of(harvestEvent.getId()));

        CodeRangeSupplementResponse response = supplementService.create(request, managerUser);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getRequestedQuantity()).isEqualTo(500L);
        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        assertThat(response.getEvidenceEventIds()).containsExactly(harvestEvent.getId());
    }

    // TC-03: đã có yêu cầu PENDING thì chặn tạo mới
    @Test
    void create_tc03_blockedWhenPendingExists() {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(supplementRepository.existsByOrganization_OrganizationIdAndStatus(
                organizationId, CodeRangeSupplementStatus.PENDING)).thenReturn(true);

        CreateSupplementRequest request = new CreateSupplementRequest();
        request.setRequestedQuantity(500L);
        request.setReason("Xin them ma.");
        request.setEvidenceEventIds(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> supplementService.create(request, managerUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tổ chức đã có yêu cầu cấp bổ sung đang chờ duyệt.");
    }

    // TC-02: VT-01 duyệt một phần 300/500
    @Test
    void approve_tc02_partial() {
        CodeRangeSupplementRequest supplement = pendingSupplement(500L);

        when(supplementRepository.findById(supplement.getId())).thenReturn(Optional.of(supplement));
        when(codeRangeRepository.findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId))
                .thenReturn(Optional.of(codeRange));
        User admin = new User();
        admin.setUserId(adminId);
        admin.setFullName("Tran Thi B");
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(supplementRepository.save(any(CodeRangeSupplementRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(organizationUserRepository.findAllByOrganization_OrganizationIdAndRole_Code(organizationId, "VT-02"))
                .thenReturn(List.of());
        when(notificationService.sendCodeRangeSupplementNotification(anyString(), anyString(), anyList()))
                .thenReturn(1);

        ApproveSupplementRequest request = new ApproveSupplementRequest();
        request.setApprovedQuantity(300L);
        request.setRemarks("Cap truoc 300 ma.");

        CodeRangeSupplementResponse response = supplementService.approve(supplement.getId(), request, adminUser);

        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getApprovedQuantity()).isEqualTo(300L);
        assertThat(codeRange.getTotalLimit()).isEqualTo(1300L);
        assertThat(response.getNotifiedCount()).isEqualTo(1);
        verify(notificationService).sendCodeRangeSupplementNotification(anyString(), anyString(), anyList());
    }

    // TC-04: VT-03 gọi API tạo → từ chối (403 ở controller, service ném BusinessException)
    @Test
    void create_tc04_recorderForbidden() {
        CreateSupplementRequest request = new CreateSupplementRequest();
        request.setRequestedQuantity(100L);
        request.setReason("Xin them ma.");
        request.setEvidenceEventIds(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> supplementService.create(request, recorderUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không có quyền tạo yêu cầu cấp bổ sung dải mã.");
    }

    // TC-04 (bổ sung): VT-02 không được duyệt
    @Test
    void approve_tc04_managerForbidden() {
        ApproveSupplementRequest request = new ApproveSupplementRequest();
        request.setApprovedQuantity(100L);

        assertThatThrownBy(() -> supplementService.approve(UUID.randomUUID(), request, managerUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không có quyền xử lý yêu cầu cấp bổ sung dải mã.");
    }

    // TC-05 (biên): duyệt số lượng vượt quá đề nghị
    @Test
    void approve_tc05_quantityExceedsRequested() {
        CodeRangeSupplementRequest supplement = pendingSupplement(500L);
        when(supplementRepository.findById(supplement.getId())).thenReturn(Optional.of(supplement));

        ApproveSupplementRequest request = new ApproveSupplementRequest();
        request.setApprovedQuantity(600L);

        assertThatThrownBy(() -> supplementService.approve(supplement.getId(), request, adminUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Số lượng thực cấp phải lớn hơn 0 và không vượt quá số lượng đề nghị.");
    }

    // TC-05 (biên): duyệt/từ chối yêu cầu không còn PENDING
    @Test
    void approve_tc05_notPending() {
        CodeRangeSupplementRequest supplement = pendingSupplement(500L);
        supplement.setStatus(CodeRangeSupplementStatus.APPROVED);
        when(supplementRepository.findById(supplement.getId())).thenReturn(Optional.of(supplement));

        ApproveSupplementRequest approveRequest = new ApproveSupplementRequest();
        approveRequest.setApprovedQuantity(100L);

        assertThatThrownBy(() -> supplementService.approve(supplement.getId(), approveRequest, adminUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chỉ có thể xử lý yêu cầu ở trạng thái PENDING.");

        RejectSupplementRequest rejectRequest = new RejectSupplementRequest();
        rejectRequest.setRejectionReason("Ly do.");

        assertThatThrownBy(() -> supplementService.reject(supplement.getId(), rejectRequest, adminUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chỉ có thể xử lý yêu cầu ở trạng thái PENDING.");
    }

    // TC-05 (biên): từ chối thiếu lý do
    @Test
    void reject_tc05_missingReason() {
        CodeRangeSupplementRequest supplement = pendingSupplement(500L);
        when(supplementRepository.findById(supplement.getId())).thenReturn(Optional.of(supplement));

        RejectSupplementRequest request = new RejectSupplementRequest();
        request.setRejectionReason("  ");

        assertThatThrownBy(() -> supplementService.reject(supplement.getId(), request, adminUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lý do từ chối không được để trống.");
    }

    // TC-06 (biên): tổ chức chưa có dải mã tạo yêu cầu
    @Test
    void create_tc06_noCodeRange() {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(supplementRepository.existsByOrganization_OrganizationIdAndStatus(
                organizationId, CodeRangeSupplementStatus.PENDING)).thenReturn(false);
        when(codeRangeRepository.findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId))
                .thenReturn(Optional.empty());

        CreateSupplementRequest request = new CreateSupplementRequest();
        request.setRequestedQuantity(100L);
        request.setReason("Xin them ma.");
        request.setEvidenceEventIds(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> supplementService.create(request, managerUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tổ chức chưa được cấp dải mã truy xuất.");
    }

    // TC-06 (biên): bằng chứng sai loại (không phải HARVEST/PREPROCESSING)
    @Test
    void create_tc06_evidenceWrongType() {
        ChainEvent transportEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipmentOf(organization))
                .eventType(ChainEventType.TRANSPORT)
                .build();
        stubCreateHappyPath(transportEvent);

        CreateSupplementRequest request = new CreateSupplementRequest();
        request.setRequestedQuantity(100L);
        request.setReason("Xin them ma.");
        request.setEvidenceEventIds(List.of(transportEvent.getId()));

        assertThatThrownBy(() -> supplementService.create(request, managerUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bằng chứng chỉ chấp nhận sự kiện thu hoạch (HARVEST) hoặc sơ chế (PREPROCESSING).");
    }

    // TC-07 (quyền): VT-02 xem yêu cầu của tổ chức khác
    @Test
    void getById_tc07_crossOrgForbidden() {
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());
        otherOrg.setName("HTX Khac");

        CodeRangeSupplementRequest supplement = pendingSupplement(200L);
        supplement.setOrganization(otherOrg);
        when(supplementRepository.findById(supplement.getId())).thenReturn(Optional.of(supplement));

        assertThatThrownBy(() -> supplementService.getById(supplement.getId(), managerUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không có quyền thao tác với yêu cầu của tổ chức khác.");
    }

    // TC-07 (biên tổ chức): bằng chứng là sự kiện của tổ chức khác
    @Test
    void create_tc07_evidenceOtherOrg() {
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());
        otherOrg.setName("HTX Khac");
        ChainEvent otherEvent = harvestEventOf(UUID.randomUUID(), otherOrg);
        stubCreateHappyPath(otherEvent);

        CreateSupplementRequest request = new CreateSupplementRequest();
        request.setRequestedQuantity(100L);
        request.setReason("Xin them ma.");
        request.setEvidenceEventIds(List.of(otherEvent.getId()));

        assertThatThrownBy(() -> supplementService.create(request, managerUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bằng chứng phải là sự kiện của tổ chức bạn.");
    }

    // Bằng chứng là sự kiện thu hoạch chưa gắn lô hàng (productionLotId trong eventData)
    @Test
    void create_unassignedHarvestEvent_ok() {
        UUID lotId = UUID.randomUUID();
        ProductionLot lot = new ProductionLot();
        lot.setId(lotId);
        lot.setName("Lo lua vu he");
        lot.setOrganization(organization);

        ChainEvent unassigned = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + lotId + "\"}")
                .recordedBy(requester)
                .build();

        stubCreateHappyPath(unassigned);
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        CreateSupplementRequest request = new CreateSupplementRequest();
        request.setRequestedQuantity(100L);
        request.setReason("Xin them ma.");
        request.setEvidenceEventIds(List.of(unassigned.getId()));

        CodeRangeSupplementResponse response = supplementService.create(request, managerUser);

        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    // Từ chối thành công kèm thông báo
    @Test
    void reject_success() {
        CodeRangeSupplementRequest supplement = pendingSupplement(500L);
        when(supplementRepository.findById(supplement.getId())).thenReturn(Optional.of(supplement));
        User admin = new User();
        admin.setUserId(adminId);
        admin.setFullName("Tran Thi B");
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(supplementRepository.save(any(CodeRangeSupplementRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(organizationUserRepository.findAllByOrganization_OrganizationIdAndRole_Code(organizationId, "VT-02"))
                .thenReturn(List.of());
        when(notificationService.sendCodeRangeSupplementNotification(anyString(), anyString(), anyList()))
                .thenReturn(1);

        RejectSupplementRequest request = new RejectSupplementRequest();
        request.setRejectionReason("Chua du co so san luong.");

        CodeRangeSupplementResponse response = supplementService.reject(supplement.getId(), request, adminUser);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Chua du co so san luong.");
        assertThat(response.getNotifiedCount()).isEqualTo(1);
    }

    // VT-02 xem danh sách yêu cầu của tổ chức mình
    @Test
    void listMine_scopedToOrganization() {
        CodeRangeSupplementRequest supplement = pendingSupplement(500L);
        Page<CodeRangeSupplementRequest> page = new PageImpl<>(List.of(supplement));
        when(supplementRepository.findByOrganization_OrganizationId(any(UUID.class), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<CodeRangeSupplementResponse> response =
                supplementService.listMine(null, 0, 20, managerUser);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getOrganizationId()).isEqualTo(organizationId);
    }

    private void stubCreateHappyPath(ChainEvent event) {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(supplementRepository.existsByOrganization_OrganizationIdAndStatus(
                organizationId, CodeRangeSupplementStatus.PENDING)).thenReturn(false);
        when(codeRangeRepository.findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(organizationId))
                .thenReturn(Optional.of(codeRange));
        when(chainEventRepository.findAllById(List.of(event.getId()))).thenReturn(List.of(event));
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        lenient().when(supplementRepository.save(any(CodeRangeSupplementRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private CodeRangeSupplementRequest pendingSupplement(long requestedQuantity) {
        CodeRangeSupplementRequest supplement = new CodeRangeSupplementRequest();
        supplement.setId(UUID.randomUUID());
        supplement.setOrganization(organization);
        supplement.setRequestedBy(requester);
        supplement.setRequestedQuantity(requestedQuantity);
        supplement.setReason("Vu thu dong san luong cao.");
        supplement.setEvidenceEventIds("[\"" + harvestEvent.getId() + "\"]");
        supplement.setStatus(CodeRangeSupplementStatus.PENDING);
        return supplement;
    }

    private vn.nguongocso.trace.entity.Shipment shipmentOf(Organization org) {
        vn.nguongocso.trace.entity.Shipment shipment = new vn.nguongocso.trace.entity.Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setOrganization(org);
        shipment.setName("Lo hang test");
        return shipment;
    }

    private ChainEvent harvestEventOf(UUID eventId, Organization org) {
        return ChainEvent.builder()
                .id(eventId)
                .shipment(shipmentOf(org))
                .eventType(ChainEventType.HARVEST)
                .recordedBy(requester)
                .build();
    }

    // TC-08: VT-02 lấy danh sách bằng chứng — sự kiện đã gắn lô hàng của tổ chức
    @Test
    void listEvidenceEvents_tc08_returnsShipmentEventsOfOrg() {
        List<ChainEventType> evidenceTypes = List.of(ChainEventType.HARVEST, ChainEventType.PREPROCESSING);
        when(chainEventRepository.findByEventTypeInAndShipment_Organization_OrganizationId(
                evidenceTypes, organizationId))
                .thenReturn(List.of(harvestEvent));
        when(chainEventRepository.findByShipmentIsNullAndEventTypeIn(evidenceTypes))
                .thenReturn(List.of());

        List<EvidenceEventResponse> result = supplementService.listEvidenceEvents(managerUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventId()).isEqualTo(harvestEvent.getId());
        assertThat(result.get(0).getEventType()).isEqualTo("HARVEST");
        assertThat(result.get(0).getRecordedByName()).isEqualTo("Nguyen Van A");
        assertThat(result.get(0).getShipmentId()).isEqualTo(harvestEvent.getShipment().getId());
    }

    // TC-08: bao gồm cả sự kiện tự do (chưa gắn lô hàng) có productionLotId trong eventData
    @Test
    void listEvidenceEvents_tc08_includesUnassignedEventsWithLotName() {
        UUID lotId = UUID.randomUUID();
        ChainEvent unassigned = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PREPROCESSING)
                .eventData("{\"productionLotId\":\"" + lotId + "\"}")
                .recordedBy(requester)
                .build();

        List<ChainEventType> evidenceTypes = List.of(ChainEventType.HARVEST, ChainEventType.PREPROCESSING);
        when(chainEventRepository.findByEventTypeInAndShipment_Organization_OrganizationId(
                evidenceTypes, organizationId))
                .thenReturn(List.of());
        when(chainEventRepository.findByShipmentIsNullAndEventTypeIn(evidenceTypes))
                .thenReturn(List.of(unassigned));

        ProductionLot lot = new ProductionLot();
        lot.setId(lotId);
        lot.setOrganization(organization);
        lot.setName("Lo Nho 01");
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(productionLotRepository.findAllById(anyCollection())).thenReturn(List.of(lot));

        List<EvidenceEventResponse> result = supplementService.listEvidenceEvents(managerUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventType()).isEqualTo("PREPROCESSING");
        assertThat(result.get(0).getProductionLotId()).isEqualTo(lotId);
        assertThat(result.get(0).getProductionLotName()).isEqualTo("Lo Nho 01");
    }

    // TC-08: sai vai trò (VT-03) bị chặn
    @Test
    void listEvidenceEvents_tc08_blockedForNonManager() {
        assertThatThrownBy(() -> supplementService.listEvidenceEvents(recorderUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không có quyền tạo yêu cầu cấp bổ sung dải mã.");
    }
}
