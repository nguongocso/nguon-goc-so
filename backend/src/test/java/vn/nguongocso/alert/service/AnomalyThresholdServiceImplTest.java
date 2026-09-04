package vn.nguongocso.alert.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.nguongocso.alert.dto.request.CategoryThresholdOverrideRequest;
import vn.nguongocso.alert.dto.request.ImpactEstimationRequest;
import vn.nguongocso.alert.dto.request.UpdateGlobalThresholdRequest;
import vn.nguongocso.alert.dto.response.AnomalyThresholdResponse;
import vn.nguongocso.alert.dto.response.ImpactEstimationResponse;
import vn.nguongocso.alert.entity.AnomalyThreshold;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.alert.repository.AnomalyThresholdRepository;
import vn.nguongocso.alert.service.impl.AnomalyThresholdServiceImpl;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.TraceCode;

/**
 * Kiểm thử đơn vị cho dịch vụ cấu hình ngưỡng quét bất thường (NCL-08-CN-014).
 */
@ExtendWith(MockitoExtension.class)
class AnomalyThresholdServiceImplTest {

    @Mock
    private AnomalyThresholdRepository anomalyThresholdRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TraceCodeScanLogRepository scanLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AnomalyThresholdServiceImpl service;

    private User adminUser;
    private CustomUserDetails customUserDetails;
    private ProductCategory categorySsauRieng;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        adminUser = User.builder()
                .userId(userId)
                .userName("admin")
                .fullName("Quản trị viên")
                .email("admin@demo.test")
                .build();

        vn.nguongocso.auth.entity.Role role = new vn.nguongocso.auth.entity.Role();
        role.setCode("VT-01");
        role.setName("Quản trị viên hệ thống");

        vn.nguongocso.organization.entity.Organization org = vn.nguongocso.organization.entity.Organization.builder()
                .organizationId(UUID.randomUUID())
                .name("Hệ thống")
                .code("SYSTEM")
                .build();

        vn.nguongocso.organization.entity.OrganizationUser orgUser = new vn.nguongocso.organization.entity.OrganizationUser();
        orgUser.setOrganization(org);

        customUserDetails = new CustomUserDetails(adminUser, orgUser, role);

        categoryId = UUID.randomUUID();
        categorySsauRieng = ProductCategory.builder()
                .id(categoryId)
                .name("Sầu riêng Ri6")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Lấy cấu hình toàn cục - trả về mặc định khi chưa có trong cơ sở dữ liệu")
    void shouldReturnDefaultGlobal_whenNotInDatabase() {
        when(anomalyThresholdRepository.findByProductCategoryIsNullAndIsActiveTrue())
                .thenReturn(Optional.empty());
        when(anomalyThresholdRepository.findByProductCategoryIsNull())
                .thenReturn(Optional.empty());

        AnomalyThresholdResponse response = service.getGlobalThreshold();

        assertNotNull(response);
        assertEquals(5, response.getMaxScansPerHour());
        assertEquals(10, response.getMaxScansPerDay());
        assertEquals(new BigDecimal("50.00"), response.getMaxDistanceKmPer30Min());
        assertEquals(30, response.getMinTimeBetweenScansMinutes());
        assertEquals(365, response.getActivationAgeDays());
        assertTrue(response.getIsActive());
    }

    @Test
    @DisplayName("Lấy cấu hình toàn cục - trả về từ cơ sở dữ liệu khi đã tồn tại")
    void shouldReturnPersistedGlobal_whenExists() {
        AnomalyThreshold entity = AnomalyThreshold.builder()
                .id(UUID.randomUUID())
                .productCategory(null)
                .maxScansPerHour(8)
                .maxScansPerDay(15)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(60.0))
                .minTimeBetweenScansMinutes(25)
                .activationAgeDays(180)
                .isActive(true)
                .createdBy(adminUser)
                .updatedBy(adminUser)
                .build();

        when(anomalyThresholdRepository.findByProductCategoryIsNullAndIsActiveTrue())
                .thenReturn(Optional.of(entity));

        AnomalyThresholdResponse response = service.getGlobalThreshold();

        assertNotNull(response);
        assertEquals(8, response.getMaxScansPerHour());
        assertEquals(15, response.getMaxScansPerDay());
        assertEquals(BigDecimal.valueOf(60.0), response.getMaxDistanceKmPer30Min());
        assertEquals(25, response.getMinTimeBetweenScansMinutes());
        assertEquals(180, response.getActivationAgeDays());
    }

    @Test
    @DisplayName("Cập nhật cấu hình toàn cục - tạo mới và phát sự kiện ActivityLog")
    void shouldUpdateGlobalThreshold_andPublishActivityLog() {
        when(userRepository.findById(customUserDetails.getUserId())).thenReturn(Optional.of(adminUser));
        when(anomalyThresholdRepository.findByProductCategoryIsNull()).thenReturn(Optional.empty());
        when(anomalyThresholdRepository.save(any(AnomalyThreshold.class)))
                .thenAnswer(inv -> {
                    AnomalyThreshold saved = inv.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        UpdateGlobalThresholdRequest request = UpdateGlobalThresholdRequest.builder()
                .maxScansPerHour(6)
                .maxScansPerDay(12)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(70.0))
                .minTimeBetweenScansMinutes(20)
                .activationAgeDays(200)
                .build();

        AnomalyThresholdResponse response = service.updateGlobalThreshold(request, customUserDetails);

        assertNotNull(response);
        assertEquals(6, response.getMaxScansPerHour());
        assertEquals(12, response.getMaxScansPerDay());
        assertEquals(BigDecimal.valueOf(70.0), response.getMaxDistanceKmPer30Min());

        ArgumentCaptor<ActivityLogEvent> eventCaptor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("UPDATE_GLOBAL_ANOMALY_THRESHOLD", eventCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("Lưu cấu hình ghi đè danh mục - thành công và liên kết đúng loại nông sản")
    void shouldSaveCategoryOverride_success() {
        when(userRepository.findById(customUserDetails.getUserId())).thenReturn(Optional.of(adminUser));
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(categorySsauRieng));
        when(anomalyThresholdRepository.findByProductCategoryId(categoryId)).thenReturn(Optional.empty());
        when(anomalyThresholdRepository.save(any(AnomalyThreshold.class)))
                .thenAnswer(inv -> {
                    AnomalyThreshold saved = inv.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        CategoryThresholdOverrideRequest request = CategoryThresholdOverrideRequest.builder()
                .productCategoryId(categoryId)
                .maxScansPerHour(3)
                .maxScansPerDay(7)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(35.0))
                .minTimeBetweenScansMinutes(15)
                .activationAgeDays(90)
                .build();

        AnomalyThresholdResponse response = service.saveCategoryOverride(request, customUserDetails);

        assertNotNull(response);
        assertEquals(categoryId, response.getProductCategoryId());
        assertEquals("Sầu riêng Ri6", response.getProductCategoryName());
        assertEquals(3, response.getMaxScansPerHour());
        assertEquals(7, response.getMaxScansPerDay());

        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    @DisplayName("Lưu cấu hình ghi đè danh mục - ném ResourceNotFoundException nếu danh mục không tồn tại")
    void shouldThrowException_whenProductCategoryNotFound() {
        when(userRepository.findById(customUserDetails.getUserId())).thenReturn(Optional.of(adminUser));
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CategoryThresholdOverrideRequest request = CategoryThresholdOverrideRequest.builder()
                .productCategoryId(categoryId)
                .maxScansPerHour(3)
                .maxScansPerDay(7)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(35.0))
                .minTimeBetweenScansMinutes(15)
                .activationAgeDays(90)
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> service.saveCategoryOverride(request, customUserDetails));
    }

    @Test
    @DisplayName("Xóa cấu hình ghi đè danh mục - chuyển isActive = false")
    void shouldDeleteCategoryOverride_bySoftDelete() {
        AnomalyThreshold override = AnomalyThreshold.builder()
                .id(UUID.randomUUID())
                .productCategory(categorySsauRieng)
                .maxScansPerHour(3)
                .maxScansPerDay(7)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(35.0))
                .minTimeBetweenScansMinutes(15)
                .activationAgeDays(90)
                .isActive(true)
                .build();

        when(userRepository.findById(customUserDetails.getUserId())).thenReturn(Optional.of(adminUser));
        when(anomalyThresholdRepository.findById(override.getId())).thenReturn(Optional.of(override));

        service.deleteCategoryOverride(override.getId(), customUserDetails);

        assertFalse(override.getIsActive());
        verify(anomalyThresholdRepository).save(override);
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    @DisplayName("Xóa cấu hình toàn cục - ném BusinessException không cho phép")
    void shouldThrowException_whenDeletingGlobalConfig() {
        AnomalyThreshold global = AnomalyThreshold.builder()
                .id(UUID.randomUUID())
                .productCategory(null)
                .maxScansPerHour(5)
                .maxScansPerDay(10)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(50.0))
                .minTimeBetweenScansMinutes(30)
                .activationAgeDays(365)
                .isActive(true)
                .build();

        when(userRepository.findById(customUserDetails.getUserId())).thenReturn(Optional.of(adminUser));
        when(anomalyThresholdRepository.findById(global.getId())).thenReturn(Optional.of(global));

        assertThrows(BusinessException.class,
                () -> service.deleteCategoryOverride(global.getId(), customUserDetails));
    }
    @Test
    @DisplayName("Lấy cấu hình hiệu lực - ưu tiên cấu hình ghi đè danh mục khi có sẵn")
    void shouldReturnCategoryOverride_forEffectiveThreshold() {
        AnomalyThreshold override = AnomalyThreshold.builder()
                .id(UUID.randomUUID())
                .productCategory(categorySsauRieng)
                .maxScansPerHour(2)
                .maxScansPerDay(4)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(20.0))
                .minTimeBetweenScansMinutes(10)
                .activationAgeDays(60)
                .isActive(true)
                .build();

        when(anomalyThresholdRepository.findByProductCategoryIdAndIsActiveTrue(categoryId))
                .thenReturn(Optional.of(override));

        AnomalyThresholdResponse effective = service.getEffectiveThreshold(categoryId);

        assertNotNull(effective);
        assertEquals(2, effective.getMaxScansPerHour());
        assertEquals(4, effective.getMaxScansPerDay());
        assertEquals(BigDecimal.valueOf(20.0), effective.getMaxDistanceKmPer30Min());
    }

    @Test
    @DisplayName("Lấy cấu hình hiệu lực - rơi về toàn cục khi danh mục không có cấu hình ghi đè")
    void shouldFallbackToGlobal_whenCategoryOverrideNotPresent() {
        AnomalyThreshold global = AnomalyThreshold.builder()
                .id(UUID.randomUUID())
                .productCategory(null)
                .maxScansPerHour(7)
                .maxScansPerDay(14)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(55.0))
                .minTimeBetweenScansMinutes(30)
                .activationAgeDays(365)
                .isActive(true)
                .build();

        when(anomalyThresholdRepository.findByProductCategoryIdAndIsActiveTrue(categoryId))
                .thenReturn(Optional.empty());
        when(anomalyThresholdRepository.findByProductCategoryIsNullAndIsActiveTrue())
                .thenReturn(Optional.of(global));

        AnomalyThresholdResponse effective = service.getEffectiveThreshold(categoryId);

        assertNotNull(effective);
        assertEquals(7, effective.getMaxScansPerHour());
        assertEquals(14, effective.getMaxScansPerDay());
    }

    @Test
    @DisplayName("Ước lượng tác động (estimateImpact) - tính toán chính xác số vi phạm và không ghi đè DB")
    void shouldEstimateImpactAccurately_withoutModifyingDatabase() {
        LocalDateTime now = LocalDateTime.now();

        TraceCode code1 = new TraceCode();
        code1.setId(UUID.randomUUID());
        code1.setCodeValue("CODE-01");
        code1.setActivatedAt(now.minusDays(10));

        TraceCode code2 = new TraceCode();
        code2.setId(UUID.randomUUID());
        code2.setCodeValue("CODE-02");
        code2.setActivatedAt(now.minusDays(500)); // Quá 365 ngày

        List<TraceCodeScanLog> scans = new ArrayList<>();
        // Code 1 có 6 lượt quét trong 30 phút (vi phạm maxScansPerHour = 5)
        for (int i = 0; i < 6; i++) {
            scans.add(TraceCodeScanLog.builder()
                    .id(UUID.randomUUID())
                    .traceCode(code1)
                    .scannedAt(now.minusDays(2).plusMinutes(i * 5))
                    .latitude(BigDecimal.valueOf(21.0285))
                    .longitude(BigDecimal.valueOf(105.8542))
                    .isAbnormal(false)
                    .build());
        }

        // Code 2 có 1 lượt quét nhưng vi phạm activationAgeDays = 365
        scans.add(TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .traceCode(code2)
                .scannedAt(now.minusDays(1))
                .latitude(BigDecimal.valueOf(10.7769))
                .longitude(BigDecimal.valueOf(106.7009))
                .isAbnormal(false)
                .build());

        when(scanLogRepository.findByScannedAtGreaterThanEqualOrderByScannedAtAsc(any(LocalDateTime.class)))
                .thenReturn(scans);

        ImpactEstimationRequest request = ImpactEstimationRequest.builder()
                .maxScansPerHour(5)
                .maxScansPerDay(10)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(50.0))
                .minTimeBetweenScansMinutes(30)
                .activationAgeDays(365)
                .build();

        ImpactEstimationResponse result = service.estimateImpact(request);

        assertNotNull(result);
        assertEquals(7, result.getTotalScansAnalyzed());
        assertEquals(2, result.getTotalTraceCodesAnalyzed());
        assertEquals(2, result.getEstimatedAnomaliesCount());
        assertEquals(1, result.getHighFrequencyCount());
        assertEquals(1, result.getActivationAgeCount());

        // Đảm bảo không lưu thay đổi vào cơ sở dữ liệu (Dry-run)
        verify(scanLogRepository, never()).save(any(TraceCodeScanLog.class));
    }

}
