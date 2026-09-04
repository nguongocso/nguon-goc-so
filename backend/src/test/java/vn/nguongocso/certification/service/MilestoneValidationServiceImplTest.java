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
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.CultivationMilestoneRepository;
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
    private CultivationMilestoneRepository milestoneRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    private MilestoneValidationServiceImpl service;

    private UUID categoryId;
    private UUID lotId;
    private ProductCategory category;
    private ProductionLot lot;

    @BeforeEach
    void setUp() {
        service = new MilestoneValidationServiceImpl(milestoneRepository, farmLogRepository);
        categoryId = UUID.randomUUID();
        lotId = UUID.randomUUID();
        category = ProductCategory.builder().id(categoryId).name("Rau ăn lá").isActive(true).build();
        lot = ProductionLot.builder().id(lotId).productCategory(category).build();
    }

    private CultivationMilestone milestone(Long id, String name, String activityType) {
        return CultivationMilestone.builder()
                .id(id)
                .name(name)
                .activityType(activityType)
                .isMandatory(true)
                .build();
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

        CultivationMilestone planting = milestone(1L, "Gieo trồng", "PLANTING");
        CultivationMilestone fertilizing = milestone(2L, "Bón phân đợt 1", "FERTILIZING");
        when(milestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(planting, fertilizing));
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

        CultivationMilestone planting = milestone(1L, "Gieo trồng", "PLANTING");
        CultivationMilestone fertilizing = milestone(2L, "Bón phân đợt hai", "FERTILIZING");
        when(milestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(planting, fertilizing));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(log(FarmActivityType.PLANTING, false)));

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).containsExactly("Bón phân đợt hai");
    }

    // Lot không có certifications -> chỉ xét GLOBAL (standard IS NULL)
    @Test
    void validate_shouldOnlyConsiderGlobalWhenNoCertifications() {
        lot.setCertifications(List.of());

        CultivationMilestone global = milestone(1L, "Gieo trồng", "PLANTING");
        // Repository trả về chỉ GLOBAL vì standardIds rỗng
        when(milestoneRepository.findMandatoryMilestonesForValidation(categoryId, List.of()))
                .thenReturn(List.of(global));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(log(FarmActivityType.PLANTING, false)));

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).isEmpty();
    }

    // 1:1 — 2 mốc cùng FERTILIZING nhưng chỉ 1 log -> thiếu 1
    @Test
    void validate_shouldEnforceOneToOneMatching() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("GlobalGAP").build();
        lot.setCertifications(List.of(certWithStandard(standard)));

        CultivationMilestone a = milestone(1L, "Bón phân đợt 1", "FERTILIZING");
        CultivationMilestone b = milestone(2L, "Bón phân đợt 2", "FERTILIZING");
        when(milestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(a, b));
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

        CultivationMilestone planting = milestone(1L, "Gieo trồng", "PLANTING");
        when(milestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(planting));
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

        CultivationMilestone weird = milestone(1L, "Mốc lạ", "NOT_A_REAL_TYPE");
        when(milestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(weird));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of());

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).containsExactly("Mốc lạ");
    }

    // Không có mốc bắt buộc -> rỗng (không cần query farm logs)
    @Test
    void validate_shouldReturnEmptyWhenNoMandatoryMilestones() {
        lot.setCertifications(List.of());
        when(milestoneRepository.findMandatoryMilestonesForValidation(categoryId, List.of()))
                .thenReturn(List.of());

        List<String> missing = service.validateMilestoneCompletion(lot);

        assertThat(missing).isEmpty();
    }

    // ===== NCL-09-CN-011 mở rộng: findMissingMilestones trả entity =====

    // 1:1 — 2 mốc cùng FERTILIZING nhưng chỉ 1 log -> trả entity "Bón phân đợt 2"
    @Test
    void findMissing_shouldReturnEntitiesForMissingMilestones() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("GlobalGAP").build();
        lot.setCertifications(List.of(certWithStandard(standard)));

        CultivationMilestone a = milestone(1L, "Bón phân đợt 1", "FERTILIZING");
        CultivationMilestone b = milestone(2L, "Bón phân đợt 2", "FERTILIZING");
        when(milestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(a, b));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(log(FarmActivityType.FERTILIZING, false)));

        List<CultivationMilestone> missing = service.findMissingMilestones(lot);

        assertThat(missing).hasSize(1);
        assertThat(missing.get(0).getName()).isEqualTo("Bón phân đợt 2");
        assertThat(missing.get(0).getActivityType()).isEqualTo("FERTILIZING");
    }

    // Đủ log cho mọi mốc -> rỗng
    @Test
    void findMissing_shouldReturnEmptyWhenAllSatisfied() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("VietGAP").build();
        lot.setCertifications(List.of(certWithStandard(standard)));

        CultivationMilestone planting = milestone(1L, "Gieo trồng", "PLANTING");
        when(milestoneRepository.findMandatoryMilestonesForValidation(
                        categoryId, List.of(standard.getId())))
                .thenReturn(List.of(planting));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
                .thenReturn(List.of(log(FarmActivityType.PLANTING, false)));

        assertThat(service.findMissingMilestones(lot)).isEmpty();
    }
}