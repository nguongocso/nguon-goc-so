package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.ChainProgressBoardResponse;
import vn.nguongocso.farm.dto.response.ChainProgressItemResponse;
import vn.nguongocso.farm.dto.response.ChainProgressStageGroupResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ChainProgressStage;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.ProductionLotServiceImpl;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
public class ProductionLotChainProgressTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private vn.nguongocso.event.repository.ChainEventRepository chainEventRepository;

    @Mock
    private HarvestEligibilityService harvestEligibilityService;

    @Mock
    private vn.nguongocso.trace.repository.CodeRangeRepository codeRangeRepository;

    @InjectMocks
    private ProductionLotServiceImpl productionLotService;

    private UUID orgId;
    private Organization organization;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("Hợp tác xã Nông nghiệp Xanh");

        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getOrganizationId()).thenReturn(orgId);
        lenient().when(userDetails.getUsername()).thenReturn("quanly_htx");
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_VT-02"))).when(userDetails).getAuthorities();
    }

    @Test
    @DisplayName("NCL-10-CN-013-TC-01: Hệ thống xếp đúng 8 lô vào 5 cột giai đoạn tương ứng")
    void testGetChainProgressBoard_Success_CategorizesCorrectly() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        FarmArea farmArea = new FarmArea();
        farmArea.setId(UUID.randomUUID());
        farmArea.setName("Vùng trồng A");

        ProductCategory category = new ProductCategory();
        category.setId(UUID.randomUUID());
        category.setName("Dưa lưới");

        ProductionLot lot1 = createLot("Lô 1 - Draft", ProductionLotStatus.DRAFT, 2, farmArea, category);
        ProductionLot lot2 = createLot("Lô 2 - Pending", ProductionLotStatus.PENDING, 3, farmArea, category);
        ProductionLot lot3 = createLot("Lô 3 - Approved", ProductionLotStatus.APPROVED, 1, farmArea, category);
        ProductionLot lot4 = createLot("Lô 4 - Harvested", ProductionLotStatus.HARVESTED, 4, farmArea, category);
        ProductionLot lot5 = createLot("Lô 5 - Preprocessed", ProductionLotStatus.PREPROCESSED, 5, farmArea, category);
        ProductionLot lot6 = createLot("Lô 6 - Harvested 2", ProductionLotStatus.HARVESTED, 1, farmArea, category);
        ProductionLot lot7 = createLot("Lô 7 - Approved 2", ProductionLotStatus.APPROVED, 2, farmArea, category);
        ProductionLot lot8 = createLot("Lô 8 - Draft 2", ProductionLotStatus.DRAFT, 1, farmArea, category);

        List<ProductionLot> lots = List.of(lot1, lot2, lot3, lot4, lot5, lot6, lot7, lot8);
        when(productionLotRepository.findByOrganization_OrganizationId(orgId)).thenReturn(lots);
        when(shipmentRepository.findByProductionLotIdIn(any())).thenReturn(Collections.emptyList());

        ChainProgressBoardResponse response = productionLotService.getChainProgressBoard(orgId, 10, null, userDetails);

        assertThat(response).isNotNull();
        assertThat(response.getOrganizationId()).isEqualTo(orgId);
        assertThat(response.getTotalOpenLots()).isEqualTo(8);
        assertThat(response.getStages()).hasSize(9);

        ChainProgressStageGroupResponse draftStage = getStageGroup(response, ChainProgressStage.DRAFT);
        assertThat(draftStage.getCount()).isEqualTo(2);

        ChainProgressStageGroupResponse pendingStage = getStageGroup(response, ChainProgressStage.PENDING);
        assertThat(pendingStage.getCount()).isEqualTo(1);

        ChainProgressStageGroupResponse approvedStage = getStageGroup(response, ChainProgressStage.APPROVED);
        assertThat(approvedStage.getCount()).isEqualTo(2);

        ChainProgressStageGroupResponse harvestedStage = getStageGroup(response, ChainProgressStage.HARVESTED);
        assertThat(harvestedStage.getCount()).isEqualTo(2);

        ChainProgressStageGroupResponse preprocessedStage = getStageGroup(response, ChainProgressStage.PREPROCESSED);
        assertThat(preprocessedStage.getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("NCL-10-CN-013-TC-02: Lô đã thu hoạch chưa có KQ kiểm nghiệm đạt -> gợi ý việc cần làm tiếp theo")
    void testGetChainProgressBoard_HarvestedWithoutPassedInspection_ShowsNextAction() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        FarmArea farmArea = new FarmArea();
        farmArea.setName("Vùng trồng B");
        ProductCategory category = new ProductCategory();
        category.setName("Xoài Cát Chu");

        ProductionLot harvestedLot = createLot("Lô Xoài Thu Hoạch", ProductionLotStatus.HARVESTED, 3, farmArea, category);

        when(productionLotRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of(harvestedLot));
        when(shipmentRepository.findByProductionLotIdIn(any())).thenReturn(Collections.emptyList());
        lenient().when(inspectionRequestRepository.existsByProductionLot_IdAndStatus(harvestedLot.getId(), InspectionRequestStatus.PENDING_RESULT)).thenReturn(false);
        lenient().when(inspectionRequestRepository.existsByProductionLot_IdAndStatus(harvestedLot.getId(), InspectionRequestStatus.PASSED)).thenReturn(false);

        ChainProgressBoardResponse response = productionLotService.getChainProgressBoard(orgId, 10, null, userDetails);

        ChainProgressStageGroupResponse harvestedGroup = getStageGroup(response, ChainProgressStage.HARVESTED);
        assertThat(harvestedGroup.getItems()).hasSize(1);

        ChainProgressItemResponse item = harvestedGroup.getItems().get(0);
        assertThat(item.getNextActionRequired()).contains("kiểm nghiệm");
        assertThat(item.getTargetScreen()).contains("/production-lots/");
        assertThat(item.getTargetScreen()).contains("/inspection");
    }

    @Test
    @DisplayName("NCL-10-CN-013-TC-03: Lô nằm ở giai đoạn chờ duyệt quá 10 ngày được đánh dấu tồn đọng")
    void testGetChainProgressBoard_OverdueLot_MarkedStagnant() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        FarmArea farmArea = new FarmArea();
        farmArea.setName("Vùng trồng C");
        ProductCategory category = new ProductCategory();
        category.setName("Bưởi Da Xanh");

        ProductionLot stagnantLot = createLot("Lô Bưởi Tồn Đọng", ProductionLotStatus.PENDING, 12, farmArea, category);

        when(productionLotRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of(stagnantLot));
        when(shipmentRepository.findByProductionLotIdIn(any())).thenReturn(Collections.emptyList());

        ChainProgressBoardResponse response = productionLotService.getChainProgressBoard(orgId, 10, null, userDetails);

        assertThat(response.getStagnantLotsCount()).isEqualTo(1);

        ChainProgressStageGroupResponse pendingGroup = getStageGroup(response, ChainProgressStage.PENDING);
        ChainProgressItemResponse item = pendingGroup.getItems().get(0);

        assertThat(item.isStagnant()).isTrue();
        assertThat(item.getDaysInStage()).isEqualTo(12);
    }

    @Test
    @DisplayName("NCL-10-CN-013-TC-04: Đăng nhập VT-02 truy cập tổ chức khác bị từ chối 403 / BusinessException")
    void testGetChainProgressBoard_AccessOtherOrg_Forbidden() {
        UUID otherOrgId = UUID.randomUUID();

        assertThatThrownBy(() -> productionLotService.getChainProgressBoard(otherOrgId, 10, null, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Từ chối truy cập: Bạn không có quyền xem dữ liệu của tổ chức này.");
    }

    private ProductionLot createLot(String name, ProductionLotStatus status, int daysAgo, FarmArea farmArea, ProductCategory category) {
        ProductionLot lot = new ProductionLot();
        lot.setId(UUID.randomUUID());
        lot.setName(name);
        lot.setStatus(status);
        lot.setOrganization(organization);
        lot.setFarmArea(farmArea);
        lot.setProductCategory(category);
        LocalDateTime pastDate = LocalDateTime.now().minusDays(daysAgo);
        lot.setCreatedAt(pastDate);
        lot.setUpdatedAt(pastDate);
        return lot;
    }

    private ChainProgressStageGroupResponse getStageGroup(ChainProgressBoardResponse response, ChainProgressStage stage) {
        return response.getStages().stream()
                .filter(s -> s.getStage() == stage)
                .findFirst()
                .orElseThrow();
    }
}
