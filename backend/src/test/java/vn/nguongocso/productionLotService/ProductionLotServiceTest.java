package vn.nguongocso.productionLotService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.dto.response.PackagingCheckResult;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.event.PackagingValidationFailedEvent;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.ProductionLotServiceImpl;
import vn.nguongocso.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class ProductionLotServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductionLotServiceImpl productionLotService;

    private UUID lotId;
    private UUID orgId;
    private CustomUserDetails userDetails;
    private ProductionLot productionLot;
    private Organization organization;
    private ProductCategory productCategory;

    @BeforeEach
    void setUp() {
        lotId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("HTX Nông Nghiệp Xanh");

        productCategory = new ProductCategory();
        productCategory.setName("Cải Ngọt");

        productionLot = new ProductionLot();
        productionLot.setId(lotId);
        productionLot.setName("Lô Cải Ngọt hữu cơ");
        productionLot.setOrganization(organization);
        productionLot.setProductCategory(productCategory);
        productionLot.setStatus(ProductionLotStatus.HARVESTED);

        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getOrganizationId()).thenReturn(orgId);
    }

    // ========== checkPackagingReadiness Tests ==========

    @Test
    void checkPackagingReadiness_shouldReturnTrue_whenAllRequiredLogsExist() {
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));

        List<FarmLog> logs = Arrays.asList(
                createFarmLog(FarmActivityType.PLANTING),
                createFarmLog(FarmActivityType.WATERING),
                createFarmLog(FarmActivityType.FERTILIZING),
                createFarmLog(FarmActivityType.PESTICIDE)
        );
        when(farmLogRepository.findByProductionLotId(productionLot)).thenReturn(logs);

        PackagingCheckResult result = productionLotService.checkPackagingReadiness(lotId);

        assertThat(result.isCanPackage()).isTrue();
        assertThat(result.getMissingLogs()).isEmpty();
        assertThat(result.getLotId()).isEqualTo(lotId);
        assertThat(result.getStatus()).isEqualTo(ProductionLotStatus.HARVESTED);
    }

    @Test
    void checkPackagingReadiness_shouldReturnFalse_whenRequiredLogsAreMissing() {
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));

        List<FarmLog> logs = Arrays.asList(
                createFarmLog(FarmActivityType.PLANTING),
                createFarmLog(FarmActivityType.WATERING)
        );
        when(farmLogRepository.findByProductionLotId(productionLot)).thenReturn(logs);

        PackagingCheckResult result = productionLotService.checkPackagingReadiness(lotId);

        assertThat(result.isCanPackage()).isFalse();
        assertThat(result.getMissingLogs())
                .containsExactlyInAnyOrder("FERTILIZING", "PESTICIDE");
    }

    @Test
    void checkPackagingReadiness_shouldThrowException_whenLotNotHarvested() {
        productionLot.setStatus(ProductionLotStatus.APPROVED);
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));

        assertThatThrownBy(() -> productionLotService.checkPackagingReadiness(lotId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HARVESTED");
    }

    @Test
    void checkPackagingReadiness_shouldThrowException_whenLotNotFound() {
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productionLotService.checkPackagingReadiness(lotId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất");
    }

    @Test
    void checkPackagingReadiness_shouldHandleDuplicateActivityTypes() {
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));

        // Duplicate PLANTING, only WATERING — missing FERTILIZING and PESTICIDE
        List<FarmLog> logs = Arrays.asList(
                createFarmLog(FarmActivityType.PLANTING),
                createFarmLog(FarmActivityType.PLANTING),
                createFarmLog(FarmActivityType.WATERING)
        );
        when(farmLogRepository.findByProductionLotId(productionLot)).thenReturn(logs);

        PackagingCheckResult result = productionLotService.checkPackagingReadiness(lotId);

        assertThat(result.isCanPackage()).isFalse();
        assertThat(result.getMissingLogs())
                .containsExactlyInAnyOrder("FERTILIZING", "PESTICIDE");
    }

    // ========== packageLot Tests ==========

    @Test
    void packageLot_shouldChangeStatusToPackaged_whenAllRequiredLogsExist() {
        when(productionLotRepository.findById(lotId))
                .thenReturn(Optional.of(productionLot))
                .thenReturn(Optional.of(productionLot)); // called twice: check + package

        List<FarmLog> logs = Arrays.asList(
                createFarmLog(FarmActivityType.PLANTING),
                createFarmLog(FarmActivityType.WATERING),
                createFarmLog(FarmActivityType.FERTILIZING),
                createFarmLog(FarmActivityType.PESTICIDE)
        );
        when(farmLogRepository.findByProductionLotId(productionLot)).thenReturn(logs);

        when(productionLotRepository.save(any(ProductionLot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateProductionLotResponse response = productionLotService.packageLot(lotId, userDetails);

        assertThat(response.getStatus()).isEqualTo(ProductionLotStatus.PACKAGED.name());

        ArgumentCaptor<ProductionLot> captor = ArgumentCaptor.forClass(ProductionLot.class);
        verify(productionLotRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductionLotStatus.PACKAGED);
    }

    @Test
    void packageLot_shouldThrowException_whenMissingRequiredLogs() {
        when(productionLotRepository.findById(lotId))
                .thenReturn(Optional.of(productionLot))
                .thenReturn(Optional.of(productionLot));

        List<FarmLog> logs = Arrays.asList(
                createFarmLog(FarmActivityType.PLANTING),
                createFarmLog(FarmActivityType.WATERING)
        );
        when(farmLogRepository.findByProductionLotId(productionLot)).thenReturn(logs);

        assertThatThrownBy(() -> productionLotService.packageLot(lotId, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể đóng gói");

        // Verify event was published
        verify(eventPublisher).publishEvent(any(PackagingValidationFailedEvent.class));
        // Verify save was never called
        verify(productionLotRepository, never()).save(any(ProductionLot.class));
    }

    @Test
    void packageLot_shouldThrowException_whenLotNotHarvested() {
        productionLot.setStatus(ProductionLotStatus.APPROVED);
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));

        assertThatThrownBy(() -> productionLotService.packageLot(lotId, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HARVESTED");
    }

    @Test
    void packageLot_shouldThrowException_whenLotNotFound() {
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productionLotService.packageLot(lotId, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất");
    }

    // ========== Helper ==========

    private FarmLog createFarmLog(FarmActivityType activityType) {
        FarmLog log = new FarmLog();
        log.setId(UUID.randomUUID());
        log.setProductionLotId(productionLot);
        log.setActivityType(activityType);
        return log;
    }
}