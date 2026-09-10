package vn.nguongocso.certification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.certification.dto.response.InspectionScanResult;
import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.certification.enums.InspectionValidityStatus;
import vn.nguongocso.certification.service.impl.InspectionExpiryServiceImpl;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test cho InspectionExpiryService (NCL-11-CN-004) bao phủ 8 Test Case (TC-01 đến TC-08).
 */
@ExtendWith(MockitoExtension.class)
class InspectionExpiryServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private InspectionValidityService inspectionValidityService;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InspectionExpiryServiceImpl inspectionExpiryService;

    private ProductionLot lot;
    private Organization organization;
    private final LocalDate today = LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inspectionExpiryService, "warningThresholdDays", 15);

        organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        organization.setName("Hợp tác xã Nông sản An Toàn");

        ProductCategory category = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Thanh long")
                .requiresInspection(true)
                .build();

        lot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô thanh long ruột đỏ 01")
                .productCategory(category)
                .organization(organization)
                .status(ProductionLotStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("TC-01: Lô có kết quả kiểm nghiệm đạt còn 5 ngày (< 15), còn 100 tem INACTIVE, chưa thu hồi -> tạo 1 cảnh báo INSPECTION_EXPIRING + 1 notification")
    void testTC01_expiringInspectionWithInactiveStamps_createsAlertAndNotification() {
        // Given
        when(productionLotRepository.findAll()).thenReturn(List.of(lot));

        InspectionValidityResponse validity = InspectionValidityResponse.builder()
                .requiresInspection(true)
                .status(InspectionValidityStatus.EXPIRING)
                .earliestExpiryDate(today.plusDays(5))
                .daysRemaining(5L)
                .inactiveStampCount(100L)
                .totalStamps(100L)
                .canActivate(true)
                .build();

        when(inspectionValidityService.calculateValidity(lot, today)).thenReturn(validity);
        when(alertRepository.existsAlertToday(eq(lot.getId()), eq(AlertType.INSPECTION_EXPIRING), any(), any()))
                .thenReturn(false);

        // When
        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections(today);

        // Then
        assertThat(result.getTotalLotsScanned()).isEqualTo(1);
        assertThat(result.getExpiringCount()).isEqualTo(1);
        assertThat(result.getAlertsCreated()).isEqualTo(1);
        assertThat(result.getNotificationsSent()).isEqualTo(1);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertCaptor.capture());
        Alert savedAlert = alertCaptor.getValue();
        assertThat(savedAlert.getType()).isEqualTo(AlertType.INSPECTION_EXPIRING);
        assertThat(savedAlert.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.PENDING);
        assertThat(savedAlert.getOrganization()).isEqualTo(organization);

        verify(notificationService, times(1)).sendInspectionExpiryNotification(eq(savedAlert), eq(lot), eq(validity));
    }

    @Test
    @DisplayName("TC-02: Lô có kết quả kiểm nghiệm đã qua ngày hết hạn 2 ngày (-2 < 0) -> trạng thái EXPIRED, tạo cảnh báo INSPECTION_EXPIRED")
    void testTC02_expiredInspection_createsExpiredAlert() {
        // Given
        when(productionLotRepository.findAll()).thenReturn(List.of(lot));

        InspectionValidityResponse validity = InspectionValidityResponse.builder()
                .requiresInspection(true)
                .status(InspectionValidityStatus.EXPIRED)
                .earliestExpiryDate(today.minusDays(2))
                .daysOverdue(2L)
                .inactiveStampCount(50L)
                .totalStamps(100L)
                .canActivate(false)
                .build();

        when(inspectionValidityService.calculateValidity(lot, today)).thenReturn(validity);
        when(alertRepository.existsAlertToday(eq(lot.getId()), eq(AlertType.INSPECTION_EXPIRED), any(), any()))
                .thenReturn(false);
        when(alertRepository.findByRelatedEntityIdAndTypeAndStatus(lot.getId(), AlertType.INSPECTION_EXPIRING, AlertStatus.PENDING))
                .thenReturn(List.of());

        // When
        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections(today);

        // Then
        assertThat(result.getTotalLotsScanned()).isEqualTo(1);
        assertThat(result.getExpiredCount()).isEqualTo(1);
        assertThat(result.getAlertsCreated()).isEqualTo(1);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertCaptor.capture());
        Alert savedAlert = alertCaptor.getValue();
        assertThat(savedAlert.getType()).isEqualTo(AlertType.INSPECTION_EXPIRED);
        assertThat(savedAlert.getSeverity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.PENDING);

        verify(notificationService, times(1)).sendInspectionExpiryNotification(eq(savedAlert), eq(lot), eq(validity));
    }

    @Test
    @DisplayName("TC-03: Lô đã được cảnh báo trong ngày hôm nay -> lần quét tiếp theo trong ngày KHÔNG tạo cảnh báo trùng")
    void testTC03_alreadyAlertedToday_doesNotCreateDuplicate() {
        // Given
        when(productionLotRepository.findAll()).thenReturn(List.of(lot));

        InspectionValidityResponse validity = InspectionValidityResponse.builder()
                .requiresInspection(true)
                .status(InspectionValidityStatus.EXPIRING)
                .earliestExpiryDate(today.plusDays(5))
                .daysRemaining(5L)
                .inactiveStampCount(100L)
                .totalStamps(100L)
                .canActivate(true)
                .build();

        when(inspectionValidityService.calculateValidity(lot, today)).thenReturn(validity);
        // Đã tạo alert hôm nay
        when(alertRepository.existsAlertToday(eq(lot.getId()), eq(AlertType.INSPECTION_EXPIRING), any(), any()))
                .thenReturn(true);

        // When
        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections(today);

        // Then
        assertThat(result.getTotalLotsScanned()).isEqualTo(1);
        assertThat(result.getAlertsCreated()).isEqualTo(0);
        assertThat(result.getNotificationsSent()).isEqualTo(0);
        assertThat(result.getSkippedCount()).isEqualTo(1);

        verify(alertRepository, never()).save(any(Alert.class));
        verify(notificationService, never()).sendInspectionExpiryNotification(any(), any(), any());
    }

    @Test
    @DisplayName("TC-04: Lô bị RECALLED (hoặc trạng thái loại trừ) -> bỏ qua, không tạo cảnh báo")
    void testTC04_recalledLot_isSkipped() {
        lot.setStatus(ProductionLotStatus.RECALLED);
        when(productionLotRepository.findAll()).thenReturn(List.of(lot));

        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections(today);

        assertThat(result.getTotalLotsScanned()).isEqualTo(0);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getAlertsCreated()).isEqualTo(0);

        verify(inspectionValidityService, never()).calculateValidity(any(), any());
        verify(alertRepository, never()).save(any());
        verify(notificationService, never()).sendInspectionExpiryNotification(any(), any(), any());
    }

    @Test
    @DisplayName("TC-05: Lô có kết quả sắp hết hạn nhưng tất cả tem đã ACTIVE (0 tem INACTIVE) -> bỏ qua")
    void testTC05_allStampsActive_isSkipped() {
        when(productionLotRepository.findAll()).thenReturn(List.of(lot));

        // Tất cả 100 tem đã active -> inactiveStampCount = 0
        InspectionValidityResponse validity = InspectionValidityResponse.builder()
                .requiresInspection(true)
                .status(InspectionValidityStatus.EXPIRING)
                .earliestExpiryDate(today.plusDays(5))
                .daysRemaining(5L)
                .inactiveStampCount(0L)
                .totalStamps(100L)
                .canActivate(true)
                .build();

        when(inspectionValidityService.calculateValidity(lot, today)).thenReturn(validity);

        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections(today);

        assertThat(result.getTotalLotsScanned()).isEqualTo(1);
        assertThat(result.getAlertsCreated()).isEqualTo(0);
        assertThat(result.getSkippedCount()).isEqualTo(1);

        verify(alertRepository, never()).save(any());
        verify(notificationService, never()).sendInspectionExpiryNotification(any(), any(), any());
    }

    @Test
    @DisplayName("TC-06: Lô có kết quả còn hạn dài (20 ngày > 15) -> bỏ qua, không tạo cảnh báo")
    void testTC06_validInspection_isSkipped() {
        when(productionLotRepository.findAll()).thenReturn(List.of(lot));

        InspectionValidityResponse validity = InspectionValidityResponse.builder()
                .requiresInspection(true)
                .status(InspectionValidityStatus.VALID)
                .earliestExpiryDate(today.plusDays(20))
                .daysRemaining(20L)
                .inactiveStampCount(100L)
                .totalStamps(100L)
                .canActivate(true)
                .build();

        when(inspectionValidityService.calculateValidity(lot, today)).thenReturn(validity);

        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections(today);

        assertThat(result.getTotalLotsScanned()).isEqualTo(1);
        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getAlertsCreated()).isEqualTo(0);

        verify(alertRepository, never()).save(any());
        verify(notificationService, never()).sendInspectionExpiryNotification(any(), any(), any());
    }

    @Test
    @DisplayName("TC-07: Lô có nhiều tiêu chí, tiêu chí A hết hạn sau 5 ngày (< 15) -> tạo cảnh báo EXPIRING")
    void testTC07_multiCriteriaEarliestExpiry_triggersExpiringAlert() {
        when(productionLotRepository.findAll()).thenReturn(List.of(lot));

        // Earliest expiry là 5 ngày (tiêu chí A hết trước)
        InspectionValidityResponse validity = InspectionValidityResponse.builder()
                .requiresInspection(true)
                .status(InspectionValidityStatus.EXPIRING)
                .earliestExpiryDate(today.plusDays(5))
                .daysRemaining(5L)
                .inactiveStampCount(80L)
                .totalStamps(100L)
                .canActivate(true)
                .build();

        when(inspectionValidityService.calculateValidity(lot, today)).thenReturn(validity);
        when(alertRepository.existsAlertToday(eq(lot.getId()), eq(AlertType.INSPECTION_EXPIRING), any(), any()))
                .thenReturn(false);

        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections(today);

        assertThat(result.getTotalLotsScanned()).isEqualTo(1);
        assertThat(result.getExpiringCount()).isEqualTo(1);
        assertThat(result.getAlertsCreated()).isEqualTo(1);

        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(notificationService, times(1)).sendInspectionExpiryNotification(any(), eq(lot), eq(validity));
    }

    @Test
    @DisplayName("TC-08: Gửi notification cho Quản lý HTX thuộc tổ chức của lô")
    void testTC08_notificationSentToRecipientsWithNotificationRead() {
        when(productionLotRepository.findAll()).thenReturn(List.of(lot));

        InspectionValidityResponse validity = InspectionValidityResponse.builder()
                .requiresInspection(true)
                .status(InspectionValidityStatus.EXPIRING)
                .earliestExpiryDate(today.plusDays(3))
                .daysRemaining(3L)
                .inactiveStampCount(50L)
                .totalStamps(100L)
                .canActivate(true)
                .build();

        when(inspectionValidityService.calculateValidity(lot, today)).thenReturn(validity);
        when(alertRepository.existsAlertToday(eq(lot.getId()), eq(AlertType.INSPECTION_EXPIRING), any(), any()))
                .thenReturn(false);

        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections(today);

        assertThat(result.getNotificationsSent()).isEqualTo(1);
        verify(notificationService, times(1)).sendInspectionExpiryNotification(any(Alert.class), eq(lot), eq(validity));
    }
}
