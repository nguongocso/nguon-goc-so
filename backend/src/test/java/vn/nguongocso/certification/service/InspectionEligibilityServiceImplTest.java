package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.certification.dto.response.InspectionEligibilityResult;
import vn.nguongocso.certification.entity.CategoryCriterion;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionBlockReasonCode;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.service.impl.InspectionEligibilityServiceImpl;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;

/**
 * Unit test cho gate QTN-30 (NCL-11-CN-005): lô chưa đạt kiểm nghiệm
 * không được tạo lô hàng.
 */
@ExtendWith(MockitoExtension.class)
class InspectionEligibilityServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 5);

    @Mock
    private InspectionCriterionResultRepository resultRepository;

    @Mock
    private CategoryCriterionRepository categoryCriterionRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private Clock clock;

    private InspectionEligibilityServiceImpl service;

    private UUID lotId;
    private UUID categoryId;
    private ProductionLot lot;

    @BeforeEach
    void setUp() {
        service = new InspectionEligibilityServiceImpl(
                resultRepository,
                categoryCriterionRepository,
                inspectionRequestRepository,
                clock);

        lotId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        ProductCategory category = new ProductCategory();
        category.setId(categoryId);
        category.setRequiresInspection(true);

        lot = new ProductionLot();
        lot.setId(lotId);
        lot.setProductCategory(category);

        lenient().when(clock.instant()).thenReturn(
                Instant.from(TODAY.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh"))));
        lenient().when(clock.getZone())
                .thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Test
    void evaluate_shouldBeEligible_whenCategoryNotRequiresInspection() {
        lot.getProductCategory().setRequiresInspection(false);

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getReasonCode()).isNull();
    }

    @Test
    void evaluate_shouldBeInspectionMissing_whenNoCriteriaConfigured() {
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE")).thenReturn(List.of());

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasonCode())
                .isEqualTo(InspectionBlockReasonCode.INSPECTION_MISSING);
    }

    @Test
    void evaluate_shouldBeInspectionFailed_whenLatestConclusionIsFailed() {
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(assignment("PESTICIDE"), assignment("HEAVY_METAL")));

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of());

        when(inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.FAILED),
                        request(InspectionRequestStatus.PASSED)));
        when(inspectionRequestRepository
                .existsByProductionLot_IdAndStatus(
                        lotId, InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(false);

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasonCode())
                .isEqualTo(InspectionBlockReasonCode.INSPECTION_FAILED);
    }

    @Test
    void evaluate_shouldBeInspectionFailed_whenLatestCriterionResultIsFailed() {
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(assignment("PESTICIDE"), assignment("HEAVY_METAL")));

        InspectionCriterionResult failed = result(
                "PESTICIDE", false, null, null);

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of(failed));

        when(inspectionRequestRepository
                .existsByProductionLot_IdAndStatus(
                        lotId, InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(false);
        when(inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of());

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasonCode())
                .isEqualTo(InspectionBlockReasonCode.INSPECTION_FAILED);
    }

    @Test
    void evaluate_shouldBeInspectionPending_whenRequestPendingResult() {
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(assignment("PESTICIDE")));

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of());

        when(inspectionRequestRepository
                .existsByProductionLot_IdAndStatus(
                        lotId, InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(true);
        when(inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.PENDING_RESULT),
                        request(InspectionRequestStatus.FAILED)));

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasonCode())
                .isEqualTo(InspectionBlockReasonCode.INSPECTION_PENDING);
    }

    @Test
    void evaluate_shouldBeInspectionExpired_whenPassedResultExpired() {
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(assignment("PESTICIDE")));

        InspectionCriterionResult passedExpired = result(
                "PESTICIDE", true, TODAY.minusDays(10), TODAY.minusDays(5));

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of(passedExpired));

        when(inspectionRequestRepository
                .existsByProductionLot_IdAndStatus(
                        lotId, InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(false);
        when(inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.PASSED)));

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasonCode())
                .isEqualTo(InspectionBlockReasonCode.INSPECTION_EXPIRED);
    }

    @Test
    void evaluate_shouldBeEligible_whenAllLatestResultsPassedAndValid() {
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(assignment("PESTICIDE"), assignment("HEAVY_METAL")));

        InspectionCriterionResult pest = result(
                "PESTICIDE", true, TODAY.minusDays(3), TODAY.plusDays(60));
        InspectionCriterionResult metal = result(
                "HEAVY_METAL", true, TODAY.minusDays(1), TODAY.plusDays(90));

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of(pest, metal));

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getPassedCriteria()).isEqualTo(2);
        assertThat(result.getEarliestExpiryDate()).isEqualTo(TODAY.plusDays(60));
    }

    @Test
    void evaluate_shouldReinspectionPassedReplaceFailed() {
        // TC-02: vòng 1 FAILED, vòng 2 PASSED → lô đủ điều kiện trở lại
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(assignment("PESTICIDE")));

        InspectionCriterionResult round2 = result(
                "PESTICIDE", true, TODAY.minusDays(1), TODAY.plusDays(90));

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of(round2));

        lenient().when(inspectionRequestRepository
                        .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.PASSED),
                        request(InspectionRequestStatus.FAILED)));
        lenient().when(inspectionRequestRepository
                        .existsByProductionLot_IdAndStatus(
                                lotId, InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(false);

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isTrue();
    }

    @Test
    void evaluate_shouldBeEligible_whenHistoricalFailedResultSupersededByPass() {
        // Test 3/4 (QTN-30 NCL-11-CN-005): lịch sử FAIL được giữ lại trong CSDL
        // nhưng kết quả PASS mới nhất thay thế → N/N PASS → UNBLOCK.
        // 5 chỉ tiêu: C1..C4 PASS, C5 từng FAIL (#1) sau đó PASS (#2/re-inspection).
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(
                        assignment("PESTICIDE"),
                        assignment("HEAVY_METAL"),
                        assignment("MOISTURE"),
                        assignment("CAKE"),
                        assignment("RESIDUE")));

        // Kết quả mới nhất (re-inspection) — tất cả đạt, còn hạn
        InspectionCriterionResult c1 = result(
                "PESTICIDE", true, TODAY.minusDays(2), TODAY.plusDays(60));
        InspectionCriterionResult c2 = result(
                "HEAVY_METAL", true, TODAY.minusDays(2), TODAY.plusDays(60));
        InspectionCriterionResult c3 = result(
                "MOISTURE", true, TODAY.minusDays(2), TODAY.plusDays(60));
        InspectionCriterionResult c4 = result(
                "CAKE", true, TODAY.minusDays(2), TODAY.plusDays(60));
        InspectionCriterionResult c5Pass = result(
                "RESIDUE", true, TODAY.minusDays(1), TODAY.plusDays(60));

        // Lịch sử FAIL cũ (inspection #1) — resultDate = null, passed = false.
        // Phải được GIỮ LẠI và KHÔNG gây BLOCK khi đã có PASS mới hơn.
        InspectionCriterionResult c5Fail = result(
                "RESIDUE", false, null, null);

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of(c1, c2, c3, c4, c5Pass, c5Fail));

        lenient().when(inspectionRequestRepository
                        .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.PASSED),
                        request(InspectionRequestStatus.FAILED)));
        lenient().when(inspectionRequestRepository
                        .existsByProductionLot_IdAndStatus(
                                lotId, InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(false);

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        // N/N PASS → UNBLOCK dù lịch sử FAIL vẫn tồn tại.
        assertThat(result.isEligible()).isTrue();
        assertThat(result.getTotalCriteria()).isEqualTo(5);
        assertThat(result.getPassedCriteria()).isEqualTo(5);
        assertThat(result.getFailedOrExpiredCriteria()).isEqualTo(0);
        assertThat(result.getReasonCode()).isNull();
    }

    @Test
    void evaluate_shouldBeBlocked_whenOneCriterionStillFailedAfterReInspection() {
        // Test 5: re-inspection vẫn FAIL → 4/5 PASS + 1 FAIL → BLOCK.
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(
                        assignment("PESTICIDE"),
                        assignment("HEAVY_METAL"),
                        assignment("MOISTURE"),
                        assignment("CAKE"),
                        assignment("RESIDUE")));

        InspectionCriterionResult c1 = result(
                "PESTICIDE", true, TODAY.minusDays(2), TODAY.plusDays(60));
        InspectionCriterionResult c2 = result(
                "HEAVY_METAL", true, TODAY.minusDays(2), TODAY.plusDays(60));
        InspectionCriterionResult c3 = result(
                "MOISTURE", true, TODAY.minusDays(2), TODAY.plusDays(60));
        InspectionCriterionResult c4 = result(
                "CAKE", true, TODAY.minusDays(2), TODAY.plusDays(60));
        InspectionCriterionResult c5Fail = result(
                "RESIDUE", false, null, null);

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of(c1, c2, c3, c4, c5Fail));

        lenient().when(inspectionRequestRepository
                        .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.FAILED),
                        request(InspectionRequestStatus.FAILED)));
        lenient().when(inspectionRequestRepository
                        .existsByProductionLot_IdAndStatus(
                                lotId, InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(false);

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasonCode())
                .isEqualTo(InspectionBlockReasonCode.INSPECTION_FAILED);
        assertThat(result.getPassedCriteria()).isEqualTo(4);
        assertThat(result.getTotalCriteria()).isEqualTo(5);
    }

    @Test
    void evaluate_shouldBeBlocked_whenReInspectionPending() {
        // Test 6: re-inspection đang PENDING (PENDING_RESULT) → BLOCK.
        when(categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(
                categoryId, "ACTIVE"))
                .thenReturn(List.of(assignment("PESTICIDE")));

        when(resultRepository.findAllByProductionLotId(lotId))
                .thenReturn(List.of());

        when(inspectionRequestRepository
                .existsByProductionLot_IdAndStatus(
                        lotId, InspectionRequestStatus.PENDING_RESULT))
                .thenReturn(true);
        lenient().when(inspectionRequestRepository
                        .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(request(InspectionRequestStatus.FAILED)));

        InspectionEligibilityResult result = service.evaluateForShipment(lot);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReasonCode())
                .isEqualTo(InspectionBlockReasonCode.INSPECTION_PENDING);
    }

    @Test
    void hasLatestFailedConclusion_shouldBeFalse_whenNoCompletedRequest() {
        when(inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.PENDING_RESULT),
                        request(InspectionRequestStatus.CANCELLED)));

        assertThat(service.hasLatestFailedConclusion(lot)).isFalse();
    }

    @Test
    void hasLatestFailedConclusion_shouldBeTrue_whenLatestCompletedIsFailed() {
        when(inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.FAILED),
                        request(InspectionRequestStatus.PASSED)));

        assertThat(service.hasLatestFailedConclusion(lot)).isTrue();
    }

    @Test
    void hasLatestFailedConclusion_shouldBeFalse_whenLatestCompletedIsPassed() {
        when(inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(
                        request(InspectionRequestStatus.PASSED),
                        request(InspectionRequestStatus.FAILED)));

        assertThat(service.hasLatestFailedConclusion(lot)).isFalse();
    }

    // ==================== Helpers ====================

    private CategoryCriterion assignment(String name) {
        InspectionCriterionCatalog catalog = InspectionCriterionCatalog.builder()
                .id(1L)
                .name(name)
                .unit("mg/kg")
                .maxThreshold(java.math.BigDecimal.ONE)
                .status("ACTIVE")
                .build();

        return CategoryCriterion.builder()
                .id(UUID.randomUUID())
                .category(lot.getProductCategory())
                .criterion(catalog)
                .build();
    }

    private InspectionCriterionResult result(
            String code, boolean passed, LocalDate resultDate, LocalDate expiryDate) {

        InspectionCriterion criterion = InspectionCriterion.builder()
                .id(UUID.randomUUID())
                .criterionCode(code)
                .criterionName(code)
                .build();

        return InspectionCriterionResult.builder()
                .id(UUID.randomUUID())
                .inspectionCriterion(criterion)
                .passed(passed)
                .resultDate(resultDate)
                .expiryDate(expiryDate)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private InspectionRequest request(InspectionRequestStatus status) {
        InspectionRequest req = new InspectionRequest();
        req.setId(UUID.randomUUID());
        req.setStatus(status);
        req.setProductionLot(lot);
        return req;
    }
}
