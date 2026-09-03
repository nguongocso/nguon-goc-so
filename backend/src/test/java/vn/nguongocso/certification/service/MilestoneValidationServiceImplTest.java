package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.CultivationMilestoneCatalog;
import vn.nguongocso.certification.entity.ProductCategoryMilestone;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.ProductCategoryMilestoneRepository;
import vn.nguongocso.certification.service.impl.MilestoneValidationServiceImpl;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.repository.FarmLogRepository;

/**
 * Tests for MilestoneValidationServiceImpl — QTN-35 milestone completion
 * check before packaging. Story: NCL-09-CN-011.
 */
@ExtendWith(MockitoExtension.class)
class MilestoneValidationServiceImplTest {

    @Mock
    private ProductCategoryMilestoneRepository categoryMilestoneRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    private MilestoneValidationServiceImpl service;

    private UUID categoryId;
    private UUID lotId;
    private ProductCategory category;
    private ProductionLot lot;

    @BeforeEach
    void setUp() {
        service = new MilestoneValidationServiceImpl(categoryMilestoneRepository, farmLogRepository);
        categoryId = UUID.randomUUID();
        lotId = UUID.randomUUID();
        category = ProductCategory.builder().id(categoryId).name("Rau ăn lá").isActive(true).build();
        lot = ProductionLot.builder().id(lotId).productCategory(category).build();
    }

    private CultivationMilestoneCatalog milestone(Long id, String name, String activityType) {
        return CultivationMilestoneCatalog.builder()
                .id(id)
                .name(name)
                .activityType(activityType)
                .status("ACTIVE")
                .build();
    }

    private ProductCategoryMilestone assignment(CultivationMilestoneCatalog m, Standard s) {
        return ProductCategoryMilestone.builder().milestone(m).standard(s).isMandatory(true).build();
    }

    private ProductionLotCertification certWithStandard(Standard standard) {
        Certification certification = Certification.builder().standard(standard).build();
        return ProductionLotCertification.builder().certification(certification).build();
    }

    private FarmLog log(FarmActivityType type, boolean corrected) {
        FarmLog fl = FarmLog.builder()
                .activityType(type)
                .productionLotId(lot)
                .isCorrected(corrected)
                .build();
        return fl;
    }

    // TC-01: đủ log cho mọi mốc bắt buộc -> rỗng
    @Test
    void validate_shouldReturnEmptyWhenAllMilestonesSatisfied() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("VietGAP").build();
        lot.setCertifications(List.of(certWithStandard(standard)));

        CultivationMilestoneCatalog planting = milestone(1L, "Gieo trồng", "PLANTING");
        CultivationMilestoneCatalog fertilizing = milestone(2L, "Bón phân đợt 1", "FERTILIZING");
        when(categoryMilestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(assignment(planting, standard), assignment(fertilizing, null)));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(
                        log(FarmActivityType.PLANTING, false),
                        log(FarmActivityType.FERTILIZING, false)));

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).isEmpty();
    }

    // TC-02: thiếu nhật ký mốc -> liệt kê đích danh tên mốc thiếu
    @Test
    void validate_shouldReturnMissingMilestoneNames() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("VietGAP").build();
        lot.setCertifications(List.of(certWithStandard(standard)));

        CultivationMilestoneCatalog planting = milestone(1L, "Gieo trồng", "PLANTING");
        CultivationMilestoneCatalog fertilizing = milestone(2L, "Bón phân đợt hai", "FERTILIZING");
        when(categoryMilestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(assignment(planting, standard), assignment(fertilizing, null)));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(log(FarmActivityType.PLANTING, false)));

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).containsExactly("Bón phân đợt hai");
    }

    // Lot không có certifications -> chỉ xét GLOBAL (standard IS NULL)
    @Test
    void validate_shouldOnlyConsiderGlobalWhenNoCertifications() {
        lot.setCertifications(List.of());

        CultivationMilestoneCatalog global = milestone(1L, "Gieo trồng", "PLANTING");
        CultivationMilestoneCatalog standardScoped = milestone(2L, "Mốc theo chuẩn", "FERTILIZING");
        // Repository trả về chỉ GLOBAL vì standardIds rỗng
        when(categoryMilestoneRepository.findMandatoryMilestonesForValidation(categoryId, List.of()))
                .thenReturn(List.of(assignment(global, null)));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(log(FarmActivityType.PLANTING, false)));

        List<String> missing = service.validateMilestoneCompletion(lot);

        // standardScoped không được xét
        assertThat(missing).isEmpty();
    }

    // 1:1 — 2 mốc cùng FERTILIZING nhưng chỉ 1 log -> thiếu 1
    @Test
    void validate_shouldEnforceOneToOneMatching() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("GlobalGAP").build();
        lot.setCertifications(List.of(certWithStandard(standard)));

        CultivationMilestoneCatalog a = milestone(1L, "Bón phân đợt 1", "FERTILIZING");
        CultivationMilestoneCatalog b = milestone(2L, "Bón phân đợt 2", "FERTILIZING");
        when(categoryMilestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(assignment(a, standard), assignment(b, standard)));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(log(FarmActivityType.FERTILIZING, false)));

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).containsExactly("Bón phân đợt 2");
    }

    // Log bị đính chính (corrected) không được tính
    @Test
    void validate_shouldIgnoreCorrectedLogs() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("VietGAP").build();
        lot.setCertifications(List.of(certWithStandard(standard)));

        CultivationMilestoneCatalog planting = milestone(1L, "Gieo trồng", "PLANTING");
        when(categoryMilestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(assignment(planting, standard)));
        // Log PLANTING bị đính chính -> không tính
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(log(FarmActivityType.PLANTING, true)));

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).containsExactly("Gieo trồng");
    }

    // activityType không hợp lệ -> coi là missing
    @Test
    void validate_shouldTreatUnknownActivityTypeAsMissing() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("VietGAP").build();
        lot.setCertifications(List.of(certWithStandard(standard)));

        CultivationMilestoneCatalog weird = milestone(1L, "Mốc lạ", "NOT_A_REAL_TYPE");
        when(categoryMilestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(assignment(weird, standard)));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of());

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).containsExactly("Mốc lạ");
    }

    // Không có mốc bắt buộc -> rỗng (không cần query farm logs)
    @Test
    void validate_shouldReturnEmptyWhenNoMandatoryMilestones() {
        lot.setCertifications(List.of());
        when(categoryMilestoneRepository.findMandatoryMilestonesForValidation(categoryId, List.of()))
                .thenReturn(List.of());

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).isEmpty();
    }
}
