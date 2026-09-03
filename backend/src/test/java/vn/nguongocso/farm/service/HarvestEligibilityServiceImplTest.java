package vn.nguongocso.farm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.InputMaterial;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.MaterialGroup;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.InputMaterialRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.HarvestEligibilityServiceImpl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HarvestEligibilityServiceImplTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private InputMaterialRepository inputMaterialRepository;

    @InjectMocks
    private HarvestEligibilityServiceImpl harvestEligibilityService;

    private UUID lotId;
    private ProductionLot lot;

    @BeforeEach
    void setUp() {
        lotId = UUID.randomUUID();
        lot = new ProductionLot();
        lot.setId(lotId);
        lot.setName("Lô sầu riêng 01");
    }

    @Test
    @DisplayName("1. Lô không có bất kỳ nhật ký PESTICIDE nào -> determined = true, eligibleHarvestDate = null, unmatched = []")
    void calculate_noPesticideLogs_shouldBeDeterminedWithNullDate() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);
        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(Collections.emptyList());

        HarvestEligibilityResponse result = harvestEligibilityService.calculateHarvestEligibility(lotId);

        assertThat(result.isDetermined()).isTrue();
        assertThat(result.getEligibleHarvestDate()).isNull();
        assertThat(result.getUnmatchedMaterials()).isEmpty();
    }

    @Test
    @DisplayName("2. Một nhật ký PESTICIDE khớp vật tư -> eligibleHarvestDate = executedDate + quarantineDays")
    void calculate_singlePesticideLog_shouldCalculateCorrectDate() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        LocalDate executedDate = LocalDate.of(2026, 8, 20);
        FarmLog log1 = FarmLog.builder()
                .id(UUID.randomUUID())
                .activityType(FarmActivityType.PESTICIDE)
                .material("Brightin 4.0EC")
                .executedDate(executedDate)
                .build();

        InputMaterial material = InputMaterial.builder()
                .name("Brightin 4.0EC")
                .materialGroup(MaterialGroup.PESTICIDE)
                .quarantineDays(7)
                .isActive(true)
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1));
        when(inputMaterialRepository.findByNameNormalized("Brightin 4.0EC"))
                .thenReturn(List.of(material));

        HarvestEligibilityResponse result = harvestEligibilityService.calculateHarvestEligibility(lotId);

        assertThat(result.isDetermined()).isTrue();
        assertThat(result.getEligibleHarvestDate()).isEqualTo(LocalDate.of(2026, 8, 27)); // 20 + 7
        assertThat(result.getUnmatchedMaterials()).isEmpty();
    }

    @Test
    @DisplayName("3. Nhiều nhật ký PESTICIDE -> eligibleHarvestDate = MAX(executedDate + quarantineDays)")
    void calculate_multiplePesticideLogs_shouldTakeMaximumEligibleDate() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        // Log 1: Phun ngày 10/8, cách ly 14 ngày -> đủ điều kiện ngày 24/8
        FarmLog log1 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Dithane M-45 80WP")
                .executedDate(LocalDate.of(2026, 8, 10))
                .build();
        InputMaterial mat1 = InputMaterial.builder()
                .name("Dithane M-45 80WP")
                .quarantineDays(14)
                .build();

        // Log 2: Phun ngày 22/8, cách ly 3 ngày -> đủ điều kiện ngày 25/8
        FarmLog log2 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Prevathon 35WG")
                .executedDate(LocalDate.of(2026, 8, 22))
                .build();
        InputMaterial mat2 = InputMaterial.builder()
                .name("Prevathon 35WG")
                .quarantineDays(3)
                .build();

        // Log 3: Phun ngày 15/8, cách ly 7 ngày -> đủ điều kiện ngày 22/8
        FarmLog log3 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Amistar Top 325SC")
                .executedDate(LocalDate.of(2026, 8, 15))
                .build();
        InputMaterial mat3 = InputMaterial.builder()
                .name("Amistar Top 325SC")
                .quarantineDays(7)
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1, log2, log3));
        when(inputMaterialRepository.findByNameNormalized("Dithane M-45 80WP")).thenReturn(List.of(mat1));
        when(inputMaterialRepository.findByNameNormalized("Prevathon 35WG")).thenReturn(List.of(mat2));
        when(inputMaterialRepository.findByNameNormalized("Amistar Top 325SC")).thenReturn(List.of(mat3));

        HarvestEligibilityResponse result = harvestEligibilityService.calculateHarvestEligibility(lotId);

        assertThat(result.isDetermined()).isTrue();
        // MAX(24/8, 25/8, 22/8) = 25/8
        assertThat(result.getEligibleHarvestDate()).isEqualTo(LocalDate.of(2026, 8, 25));
        assertThat(result.getUnmatchedMaterials()).isEmpty();
    }

    @Test
    @DisplayName("4. Khớp vật tư không phân biệt hoa thường (Case-insensitive matching)")
    void calculate_caseInsensitiveMaterialMatch_shouldSucceed() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        FarmLog log1 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("brightin 4.0ec")
                .executedDate(LocalDate.of(2026, 8, 10))
                .build();

        InputMaterial mat = InputMaterial.builder()
                .name("Brightin 4.0EC")
                .quarantineDays(7)
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1));
        when(inputMaterialRepository.findByNameNormalized("brightin 4.0ec"))
                .thenReturn(List.of(mat));

        HarvestEligibilityResponse result = harvestEligibilityService.calculateHarvestEligibility(lotId);

        assertThat(result.isDetermined()).isTrue();
        assertThat(result.getEligibleHarvestDate()).isEqualTo(LocalDate.of(2026, 8, 17));
    }

    @Test
    @DisplayName("5. Khớp vật tư có khoảng trắng đầu/cuối (Trim material)")
    void calculate_trimmedMaterialMatch_shouldSucceed() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        FarmLog log1 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("   Amistar Top 325SC   ")
                .executedDate(LocalDate.of(2026, 8, 10))
                .build();

        InputMaterial mat = InputMaterial.builder()
                .name("Amistar Top 325SC")
                .quarantineDays(7)
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1));
        when(inputMaterialRepository.findByNameNormalized("Amistar Top 325SC"))
                .thenReturn(List.of(mat));

        HarvestEligibilityResponse result = harvestEligibilityService.calculateHarvestEligibility(lotId);

        assertThat(result.isDetermined()).isTrue();
        assertThat(result.getEligibleHarvestDate()).isEqualTo(LocalDate.of(2026, 8, 17));
    }

    @Test
    @DisplayName("6. Vật tư không khớp danh mục -> determined = false, eligibleHarvestDate = null, unmatchedMaterials = [material]")
    void calculate_unmatchedMaterial_shouldReturnUndetermined() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        FarmLog log1 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Thuốc trừ sâu thảo mộc tự chế")
                .executedDate(LocalDate.of(2026, 8, 10))
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1));
        when(inputMaterialRepository.findByNameNormalized("Thuốc trừ sâu thảo mộc tự chế"))
                .thenReturn(Collections.emptyList());

        HarvestEligibilityResponse result = harvestEligibilityService.calculateHarvestEligibility(lotId);

        assertThat(result.isDetermined()).isFalse();
        assertThat(result.getEligibleHarvestDate()).isNull();
        assertThat(result.getUnmatchedMaterials()).containsExactly("Thuốc trừ sâu thảo mộc tự chế");
    }

    @Test
    @DisplayName("7. Nhiều vật tư không khớp danh mục -> unmatchedMaterials trả về danh sách distinct")
    void calculate_multipleUnmatchedMaterials_shouldReturnDistinctList() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        FarmLog log1 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Thuốc A")
                .executedDate(LocalDate.of(2026, 8, 10))
                .build();
        FarmLog log2 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Thuốc B")
                .executedDate(LocalDate.of(2026, 8, 12))
                .build();
        FarmLog log3 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Thuốc A")
                .executedDate(LocalDate.of(2026, 8, 15))
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1, log2, log3));
        when(inputMaterialRepository.findByNameNormalized("Thuốc A")).thenReturn(Collections.emptyList());
        when(inputMaterialRepository.findByNameNormalized("Thuốc B")).thenReturn(Collections.emptyList());

        HarvestEligibilityResponse result = harvestEligibilityService.calculateHarvestEligibility(lotId);

        assertThat(result.isDetermined()).isFalse();
        assertThat(result.getEligibleHarvestDate()).isNull();
        assertThat(result.getUnmatchedMaterials()).containsExactly("Thuốc A", "Thuốc B");
    }

    @Test
    @DisplayName("8. ProductionLot không tồn tại -> ném BusinessException")
    void calculate_lotNotFound_shouldThrowBusinessException() {
        when(productionLotRepository.existsById(lotId)).thenReturn(false);

        assertThatThrownBy(() -> harvestEligibilityService.calculateHarvestEligibility(lotId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất");
    }

    @Test
    @DisplayName("9. Nhật ký PESTICIDE thiếu executedDate -> ném BusinessException bắt buộc bổ sung trước (B-02)")
    void calculate_missingExecutedDate_shouldThrowBusinessException() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        FarmLog log1 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Brightin 4.0EC")
                .executedDate(null)
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1));

        assertThatThrownBy(() -> harvestEligibilityService.calculateHarvestEligibility(lotId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện");
    }

    @Test
    @DisplayName("11. Nhiều nhật ký PESTICIDE nhưng có 1 log thiếu executedDate -> ném BusinessException (B-02)")
    void calculate_multiplePesticideLogsOneMissingExecutedDate_shouldThrowBusinessException() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        FarmLog log1 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Brightin 4.0EC")
                .executedDate(LocalDate.of(2026, 8, 10))
                .build();
        FarmLog log2 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Dithane M-45 80WP")
                .executedDate(null)
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1, log2));

        assertThatThrownBy(() -> harvestEligibilityService.calculateHarvestEligibility(lotId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện");
    }

    @Test
    @DisplayName("10. Vật tư có quarantineDays = 0 -> eligibleHarvestDate = executedDate + 0")
    void calculate_zeroQuarantineDays_shouldAddZeroDays() {
        when(productionLotRepository.existsById(lotId)).thenReturn(true);

        LocalDate executedDate = LocalDate.of(2026, 8, 20);
        FarmLog log1 = FarmLog.builder()
                .activityType(FarmActivityType.PESTICIDE)
                .material("Chế phẩm vi sinh an toàn")
                .executedDate(executedDate)
                .build();

        InputMaterial mat = InputMaterial.builder()
                .name("Chế phẩm vi sinh an toàn")
                .quarantineDays(0)
                .build();

        when(farmLogRepository.findByProductionLotIdAndActivityType(lotId, FarmActivityType.PESTICIDE))
                .thenReturn(List.of(log1));
        when(inputMaterialRepository.findByNameNormalized("Chế phẩm vi sinh an toàn"))
                .thenReturn(List.of(mat));

        HarvestEligibilityResponse result = harvestEligibilityService.calculateHarvestEligibility(lotId);

        assertThat(result.isDetermined()).isTrue();
        assertThat(result.getEligibleHarvestDate()).isEqualTo(executedDate);
        assertThat(result.getUnmatchedMaterials()).isEmpty();
    }
}
