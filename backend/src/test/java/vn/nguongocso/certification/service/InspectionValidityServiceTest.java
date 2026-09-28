package vn.nguongocso.certification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.certification.entity.CategoryCriterion;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.enums.InspectionValidityStatus;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.service.impl.InspectionValidityServiceImpl;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test cho InspectionValidityService (NCL-11-CN-004).
 */
@ExtendWith(MockitoExtension.class)
class InspectionValidityServiceTest {

    @Mock
    private CategoryCriterionRepository categoryCriterionRepository;

    @Mock
    private InspectionCriterionResultRepository resultRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @InjectMocks
    private InspectionValidityServiceImpl inspectionValidityService;

    private ProductionLot lot;
    private ProductCategory category;
    private Organization organization;
    private final LocalDate today = LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inspectionValidityService, "warningThresholdDays", 15);

        organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        organization.setName("Hợp tác xã Nông sản Sạch");

        category = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Xoài Cát Chu")
                .requiresInspection(true)
                .build();

        lot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô xoài xuất khẩu 01")
                .productCategory(category)
                .organization(organization)
                .status(ProductionLotStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("Loại nông sản không bắt buộc kiểm nghiệm -> trạng thái NOT_REQUIRED và canActivate=true")
    void testCalculateValidity_notRequired() {
        category.setRequiresInspection(false);

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionValidityStatus.NOT_REQUIRED);
        assertThat(response.getRequiresInspection()).isFalse();
        assertThat(response.getCanActivate()).isTrue();
        assertThat(response.getCanCreateNewRequest()).isFalse();
    }

    @Test
    @DisplayName("Loại nông sản chưa có chỉ tiêu ACTIVE nào -> trạng thái NO_VALID_RESULT và canActivate=false")
    void testCalculateValidity_noActiveCriteria() {
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(category.getId(), "ACTIVE"))
                .thenReturn(List.of());

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionValidityStatus.NO_VALID_RESULT);
        assertThat(response.getRequiresInspection()).isTrue();
        assertThat(response.getCanActivate()).isFalse();
    }

    @Test
    @DisplayName("Có chỉ tiêu bị kết quả KHÔNG ĐẠT -> trạng thái NO_VALID_RESULT")
    void testCalculateValidity_hasFailedResult() {
        InspectionCriterionCatalog catalogItem = InspectionCriterionCatalog.builder()
                .id(1L)
                .name("Dư lượng thuốc BVTV")
                .build();

        CategoryCriterion assignment = CategoryCriterion.builder()
                .category(category)
                .criterion(catalogItem)
                .build();

        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(category.getId(), "ACTIVE"))
                .thenReturn(List.of(assignment));

        InspectionCriterion criterion = InspectionCriterion.builder()
                .criterionCode("CRIT_01")
                .criterionName("Dư lượng thuốc BVTV")
                .build();

        InspectionCriterionResult failedResult = InspectionCriterionResult.builder()
                .inspectionCriterion(criterion)
                .passed(false)
                .resultDate(today.minusDays(1))
                .expiryDate(today.plusDays(30))
                .build();

        when(resultRepository.findAllByProductionLotId(lot.getId()))
                .thenReturn(List.of(failedResult));

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionValidityStatus.NO_VALID_RESULT);
        assertThat(response.getCanActivate()).isFalse();
    }

    @Test
    @DisplayName("TC-06: Tất cả chỉ tiêu ĐẠT và còn hạn dài (20 ngày > ngưỡng 15) -> trạng thái VALID")
    void testCalculateValidity_valid() {
        InspectionCriterionCatalog catalogItem = InspectionCriterionCatalog.builder()
                .id(1L)
                .name("Dư lượng thuốc BVTV")
                .build();

        CategoryCriterion assignment = CategoryCriterion.builder()
                .category(category)
                .criterion(catalogItem)
                .build();

        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(category.getId(), "ACTIVE"))
                .thenReturn(List.of(assignment));

        LocalDate expiryDate = today.plusDays(20);
        InspectionCriterion criterion = InspectionCriterion.builder()
                .criterionCode("CRIT_01")
                .criterionName("Dư lượng thuốc BVTV")
                .build();

        InspectionCriterionResult passedResult = InspectionCriterionResult.builder()
                .inspectionCriterion(criterion)
                .passed(true)
                .resultDate(today.minusDays(5))
                .expiryDate(expiryDate)
                .build();

        when(resultRepository.findAllByProductionLotId(lot.getId()))
                .thenReturn(List.of(passedResult));

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionValidityStatus.VALID);
        assertThat(response.getDaysRemaining()).isEqualTo(20L);
        assertThat(response.getEarliestExpiryDate()).isEqualTo(expiryDate);
        assertThat(response.getCanActivate()).isTrue();
    }

    @Test
    @DisplayName("TC-01: Kết quả kiểm nghiệm đạt còn 5 ngày (< 15 ngày) -> trạng thái EXPIRING")
    void testCalculateValidity_expiring_TC01() {
        InspectionCriterionCatalog catalogItem = InspectionCriterionCatalog.builder()
                .id(1L)
                .name("Kim loại nặng")
                .build();

        CategoryCriterion assignment = CategoryCriterion.builder()
                .category(category)
                .criterion(catalogItem)
                .build();

        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(category.getId(), "ACTIVE"))
                .thenReturn(List.of(assignment));

        LocalDate expiryDate = today.plusDays(5);
        InspectionCriterion criterion = InspectionCriterion.builder()
                .criterionCode("CRIT_01")
                .criterionName("Kim loại nặng")
                .build();

        InspectionCriterionResult passedResult = InspectionCriterionResult.builder()
                .inspectionCriterion(criterion)
                .passed(true)
                .resultDate(today.minusDays(10))
                .expiryDate(expiryDate)
                .build();

        when(resultRepository.findAllByProductionLotId(lot.getId()))
                .thenReturn(List.of(passedResult));

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionValidityStatus.EXPIRING);
        assertThat(response.getDaysRemaining()).isEqualTo(5L);
        assertThat(response.getEarliestExpiryDate()).isEqualTo(expiryDate);
        assertThat(response.getCanActivate()).isTrue();
    }

    @Test
    @DisplayName("TC-02: Kết quả kiểm nghiệm đã qua ngày hết hạn 2 ngày (-2 < 0) -> trạng thái EXPIRED")
    void testCalculateValidity_expired_TC02() {
        InspectionCriterionCatalog catalogItem = InspectionCriterionCatalog.builder()
                .id(1L)
                .name("Vi sinh vật")
                .build();

        CategoryCriterion assignment = CategoryCriterion.builder()
                .category(category)
                .criterion(catalogItem)
                .build();

        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(category.getId(), "ACTIVE"))
                .thenReturn(List.of(assignment));

        LocalDate expiryDate = today.minusDays(2);
        InspectionCriterion criterion = InspectionCriterion.builder()
                .criterionCode("CRIT_01")
                .criterionName("Vi sinh vật")
                .build();

        InspectionCriterionResult expiredResult = InspectionCriterionResult.builder()
                .inspectionCriterion(criterion)
                .passed(true)
                .resultDate(today.minusDays(30))
                .expiryDate(expiryDate)
                .build();

        when(resultRepository.findAllByProductionLotId(lot.getId()))
                .thenReturn(List.of(expiredResult));

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionValidityStatus.EXPIRED);
        assertThat(response.getDaysOverdue()).isEqualTo(2L);
        assertThat(response.getDaysRemaining()).isNull();
        assertThat(response.getEarliestExpiryDate()).isEqualTo(expiryDate);
        assertThat(response.getCanActivate()).isFalse();
        assertThat(response.getCanCreateNewRequest()).isTrue();
    }

    @Test
    @DisplayName("TC-07: Lô có nhiều chỉ tiêu, lấy ngày hết hạn sớm nhất (min) để đánh giá")
    void testCalculateValidity_multiCriteriaEarliestExpiry_TC07() {
        InspectionCriterionCatalog catalogA = InspectionCriterionCatalog.builder()
                .id(1L)
                .name("Dư lượng thuốc trừ sâu")
                .build();

        InspectionCriterionCatalog catalogB = InspectionCriterionCatalog.builder()
                .id(2L)
                .name("Hàm lượng chì")
                .build();

        CategoryCriterion assignA = CategoryCriterion.builder()
                .category(category)
                .criterion(catalogA)
                .build();

        CategoryCriterion assignB = CategoryCriterion.builder()
                .category(category)
                .criterion(catalogB)
                .build();

        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(category.getId(), "ACTIVE"))
                .thenReturn(List.of(assignA, assignB));

        LocalDate expiryA = today.plusDays(5);  // sắp hết hạn (< 15)
        LocalDate expiryB = today.plusDays(20); // còn hạn dài (> 15)

        InspectionCriterion critA = InspectionCriterion.builder()
                .criterionCode("CRIT_A")
                .criterionName("Dư lượng thuốc trừ sâu")
                .build();

        InspectionCriterion critB = InspectionCriterion.builder()
                .criterionCode("CRIT_B")
                .criterionName("Hàm lượng chì")
                .build();

        InspectionCriterionResult resultA = InspectionCriterionResult.builder()
                .inspectionCriterion(critA)
                .passed(true)
                .resultDate(today.minusDays(2))
                .expiryDate(expiryA)
                .build();

        InspectionCriterionResult resultB = InspectionCriterionResult.builder()
                .inspectionCriterion(critB)
                .passed(true)
                .resultDate(today.minusDays(2))
                .expiryDate(expiryB)
                .build();

        when(resultRepository.findAllByProductionLotId(lot.getId()))
                .thenReturn(List.of(resultA, resultB));

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionValidityStatus.EXPIRING);
        assertThat(response.getEarliestExpiryDate()).isEqualTo(expiryA);
        assertThat(response.getDaysRemaining()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Kiểm tra đếm tem chưa kích hoạt và tổng tem")
    void testCalculateValidity_stampCounts() {
        category.setRequiresInspection(false);
        when(traceCodeRepository.countTotalByProductionLotId(lot.getId())).thenReturn(100L);
        when(traceCodeRepository.countInactiveByProductionLotId(lot.getId())).thenReturn(40L);

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getTotalStamps()).isEqualTo(100L);
        assertThat(response.getInactiveStampCount()).isEqualTo(40L);
    }

    @Test
    @DisplayName("Nhiều chỉ tiêu cùng tên nhưng khác tiêu chuẩn: chỉ các chỉ tiêu thực sự sắp hết hạn mới được tính vào expiringCriteria")
    void testCalculateValidity_multipleSameNameCriteriaDistinctStandards() {
        InspectionCriterionCatalog cat1 = InspectionCriterionCatalog.builder()
                .id(1L)
                .name("Aflatoxin B1")
                .referenceStandard("FSSC 22000")
                .build();
        InspectionCriterionCatalog cat2 = InspectionCriterionCatalog.builder()
                .id(2L)
                .name("Aflatoxin B1")
                .referenceStandard("BRCGS Food Safety")
                .build();
        InspectionCriterionCatalog cat3 = InspectionCriterionCatalog.builder()
                .id(3L)
                .name("Aflatoxin B1")
                .referenceStandard("IFS Food")
                .build();

        CategoryCriterion assign1 = CategoryCriterion.builder().category(category).criterion(cat1).build();
        CategoryCriterion assign2 = CategoryCriterion.builder().category(category).criterion(cat2).build();
        CategoryCriterion assign3 = CategoryCriterion.builder().category(category).criterion(cat3).build();

        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(category.getId(), "ACTIVE"))
                .thenReturn(List.of(assign1, assign2, assign3));

        InspectionCriterion crit1 = InspectionCriterion.builder()
                .criterionId(1L)
                .criterionCode("Aflatoxin B1")
                .criterionName("Aflatoxin B1")
                .build();
        InspectionCriterion crit2 = InspectionCriterion.builder()
                .criterionId(2L)
                .criterionCode("Aflatoxin B1")
                .criterionName("Aflatoxin B1")
                .build();
        InspectionCriterion crit3 = InspectionCriterion.builder()
                .criterionId(3L)
                .criterionCode("Aflatoxin B1")
                .criterionName("Aflatoxin B1")
                .build();

        // Chỉ tiêu 1: hết hạn hôm nay (còn 0 ngày <= 15 -> EXPIRING)
        InspectionCriterionResult res1 = InspectionCriterionResult.builder()
                .inspectionCriterion(crit1)
                .passed(true)
                .resultDate(today)
                .expiryDate(today)
                .build();

        // Chỉ tiêu 2: còn hạn 20 ngày (> 15 -> VALID, không sắp hết hạn)
        InspectionCriterionResult res2 = InspectionCriterionResult.builder()
                .inspectionCriterion(crit2)
                .passed(true)
                .resultDate(today)
                .expiryDate(today.plusDays(20))
                .build();

        // Chỉ tiêu 3: còn hạn 10 ngày (<= 15 -> EXPIRING)
        InspectionCriterionResult res3 = InspectionCriterionResult.builder()
                .inspectionCriterion(crit3)
                .passed(true)
                .resultDate(today)
                .expiryDate(today.plusDays(10))
                .build();

        when(resultRepository.findAllByProductionLotId(lot.getId()))
                .thenReturn(List.of(res1, res2, res3));

        InspectionValidityResponse response = inspectionValidityService.calculateValidity(lot, today);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(InspectionValidityStatus.EXPIRING);
        assertThat(response.getEarliestExpiryDate()).isEqualTo(today);
        assertThat(response.getDaysRemaining()).isEqualTo(0L);

        // Đảm bảo chỉ đúng 2 chỉ tiêu sắp hết hạn, không bị đếm thành 3
        assertThat(response.getExpiringCriteria())
                .hasSize(2)
                .containsExactly("Aflatoxin B1 (FSSC 22000)", "Aflatoxin B1 (IFS Food)")
                .doesNotContain("Aflatoxin B1 (BRCGS Food Safety)");
        assertThat(response.getExpiredCriteria()).isEmpty();

        // Kiểm tra chi tiết 3 tiêu chí trong danh sách criteria
        assertThat(response.getCriteria()).hasSize(3);
        assertThat(response.getCriteria().get(0).getStatus()).isEqualTo(InspectionValidityStatus.EXPIRING);
        assertThat(response.getCriteria().get(1).getStatus()).isEqualTo(InspectionValidityStatus.VALID);
        assertThat(response.getCriteria().get(2).getStatus()).isEqualTo(InspectionValidityStatus.EXPIRING);
    }
}
