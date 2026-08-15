package vn.nguongocso.alert.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.alert.service.impl.ScanAnomalyDetectionServiceImpl;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Kiểm thử phát hiện quét bất thường NCL-08-CN-001.
 *
 * <p>
 * Xác minh: phát hiện bất thường, đánh dấu scan log bất thường (QTN-10), tạo
 * cảnh báo {@code SCAN_ANOMALY}, giữ nguyên hành vi severity và gửi thông báo.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ScanAnomalyDetectionServiceImplTest {

    private static final double LOC_A_LAT = 21.0285;
    private static final double LOC_A_LON = 105.8542;
    private static final double LOC_B_LAT = 10.7769;
    private static final double LOC_B_LON = 106.7009;
    private static final double LOC_C_LAT = 16.0544;
    private static final double LOC_C_LON = 108.2022;

    @Mock
    private TraceCodeScanLogRepository traceCodeScanLogRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    private ScanAnomalyDetectionServiceImpl service;

    private UUID traceCodeId;
    private TraceCode traceCode;

    @BeforeEach
    void setUp() {
        service = new ScanAnomalyDetectionServiceImpl(
                traceCodeScanLogRepository,
                notificationService,
                alertRepository,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                traceCodeRepository);

        traceCodeId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setOrganization(organization);

        traceCode = new TraceCode();
        traceCode.setId(traceCodeId);
        traceCode.setCodeValue("NCL0001");
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        traceCode.setShipment(shipment);

        lenient().when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TraceCodeScanLog scan(LocalDateTime at, double lat, double lon, boolean abnormal) {
        return TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .scannedAt(at)
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lon))
                .isAbnormal(abnormal)
                .build();
    }

    private void stubRecentScans(List<TraceCodeScanLog> scanLogs) {
        when(traceCodeScanLogRepository
                .findByTraceCodeIdAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
                        eq(traceCodeId), any(LocalDateTime.class)))
                .thenReturn(scanLogs);
    }

    @Test
    void shouldCreateScanAnomalyAlertAndMarkScansAbnormal_whenDistinctLocationsReachThreshold() {
        LocalDateTime now = LocalDateTime.now();
        List<TraceCodeScanLog> scans = Arrays.asList(
                scan(now.minusMinutes(9), LOC_A_LAT, LOC_A_LON, false),
                scan(now.minusMinutes(6), LOC_B_LAT, LOC_B_LON, false),
                scan(now.minusMinutes(3), LOC_C_LAT, LOC_C_LON, false));

        stubRecentScans(scans);
        when(alertRepository.existsByRelatedEntityIdAndTypeAndStatus(
                traceCodeId, AlertType.SCAN_ANOMALY, AlertStatus.PENDING)).thenReturn(false);
        when(traceCodeRepository.findById(traceCodeId)).thenReturn(Optional.of(traceCode));

        service.onScanRecorded(traceCodeId);

        // QTN-10: các scan log phải được đánh dấu bất thường kèm lý do.
        verify(traceCodeScanLogRepository).saveAll(scans);
        for (TraceCodeScanLog scan : scans) {
            assertTrue(scan.getIsAbnormal(), "Scan log phải được đánh dấu bất thường");
            assertFalse(scan.getAbnormalReason() == null || scan.getAbnormalReason().isBlank(),
                    "abnormalReason phải được điền");
        }

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();
        assertEquals(AlertType.SCAN_ANOMALY, alert.getType());
        assertEquals(traceCodeId, alert.getRelatedEntityId());
        assertEquals("TRACE_CODE", alert.getRelatedEntityType());
        assertEquals(AlertSeverity.HIGH, alert.getSeverity());
        assertEquals(AlertStatus.PENDING, alert.getStatus());

        verify(notificationService).sendScanAnomalyNotification(alert);
    }

    @Test
    void shouldReturnMediumSeverity_whenOnlyTwoDistinctLocations() {
        LocalDateTime now = LocalDateTime.now();
        List<TraceCodeScanLog> scans = Arrays.asList(
                scan(now.minusMinutes(9), LOC_A_LAT, LOC_A_LON, false),
                scan(now.minusMinutes(6), LOC_A_LAT, LOC_A_LON, false),
                scan(now.minusMinutes(3), LOC_B_LAT, LOC_B_LON, false));

        stubRecentScans(scans);
        when(alertRepository.existsByRelatedEntityIdAndTypeAndStatus(
                traceCodeId, AlertType.SCAN_ANOMALY, AlertStatus.PENDING)).thenReturn(false);
        when(traceCodeRepository.findById(traceCodeId)).thenReturn(Optional.of(traceCode));

        service.onScanRecorded(traceCodeId);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(AlertSeverity.MEDIUM, alertCaptor.getValue().getSeverity());
    }

    @Test
    void shouldNotCreateAlertOrMarkScans_whenAllScansAreAtSameLocation() {
        LocalDateTime now = LocalDateTime.now();
        List<TraceCodeScanLog> scans = Arrays.asList(
                scan(now.minusMinutes(9), LOC_A_LAT, LOC_A_LON, false),
                scan(now.minusMinutes(6), LOC_A_LAT, LOC_A_LON, false),
                scan(now.minusMinutes(3), LOC_A_LAT, LOC_A_LON, false),
                scan(now.minusMinutes(1), LOC_A_LAT, LOC_A_LON, false));

        stubRecentScans(scans);

        service.onScanRecorded(traceCodeId);

        verify(traceCodeScanLogRepository, never()).saveAll(anyList());
        verify(alertRepository, never()).save(any(Alert.class));
        verify(notificationService, never()).sendScanAnomalyNotification(any(Alert.class));
        for (TraceCodeScanLog scan : scans) {
            assertFalse(scan.getIsAbnormal(), "Quét cùng vị trí hợp lệ không được đánh dấu bất thường");
        }
    }

    @Test
    void shouldNotCreateAlert_whenScanCountBelowThreshold() {
        LocalDateTime now = LocalDateTime.now();
        List<TraceCodeScanLog> scans = Arrays.asList(
                scan(now.minusMinutes(2), LOC_A_LAT, LOC_A_LON, false),
                scan(now.minusMinutes(1), LOC_B_LAT, LOC_B_LON, false));

        stubRecentScans(scans);

        service.onScanRecorded(traceCodeId);

        verify(alertRepository, never()).save(any(Alert.class));
        verify(traceCodeScanLogRepository, never()).saveAll(anyList());
    }
}