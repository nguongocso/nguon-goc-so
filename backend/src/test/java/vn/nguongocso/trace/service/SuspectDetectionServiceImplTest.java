package vn.nguongocso.trace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.dto.request.UnlockTraceCodeRequest;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeDetailResponse;
import vn.nguongocso.trace.dto.response.UnlockTraceCodeResponse;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.SuspectDetectionServiceImpl;

/**
 * Kiểm thử chấm điểm nghi vấn NCL-08-CN-007.
 *
 * <p>
 * Xác minh các quy tắc +30/+40/+15, ngưỡng SUSPECT = 50, không tự động khóa,
 * và sự nhất quán giữa điểm lưu (evaluateSuspicion) và bảng phân tích
 * (getSuspectDetail).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SuspectDetectionServiceImplTest {

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private TraceCodeScanLogRepository scanLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private SuspectDetectionServiceImpl service;

    private UUID traceCodeId;
    private TraceCode traceCode;

    @BeforeEach
    void setUp() {
        service = new SuspectDetectionServiceImpl(
                traceCodeRepository, scanLogRepository, userRepository, notificationService, eventPublisher);

        traceCodeId = UUID.randomUUID();

        traceCode = new TraceCode();
        traceCode.setId(traceCodeId);
        traceCode.setCodeValue("NCL0001");
        traceCode.setStatus(TraceCodeStatus.ACTIVE);

        when(traceCodeRepository.findById(traceCodeId)).thenReturn(Optional.of(traceCode));
    }

    private TraceCodeScanLog scan(long minutesAgo, double lat, double lon) {
        return TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .scannedAt(LocalDateTime.now().minusMinutes(minutesAgo))
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lon))
                .build();
    }

    private void stubRecentScans(List<TraceCodeScanLog> scans) {
        when(scanLogRepository.findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(
                eq(traceCodeId), any(LocalDateTime.class)))
                .thenReturn(scans);
    }

    @Test
    void shouldApplyImpossibleTravelScore_whenTwoScansFarApartWithinShortTime() {
        // Scan 1: Hà Nội, scan 2: Đà Nẵng ~2 phút sau -> +40 (không đạt SUSPECT).
        List<TraceCodeScanLog> scans = new ArrayList<>();
        scans.add(scan(10, 21.0285, 105.8542)); // Hà Nội
        scans.add(scan(8, 16.0544, 108.2022)); // Đà Nẵng
        stubRecentScans(scans);

        service.evaluateSuspicion(traceCodeId);

        assertEquals(40, traceCode.getSuspicionScore());
        assertEquals(TraceCodeStatus.ACTIVE, traceCode.getStatus());
        verify(notificationService, never()).sendSuspectTraceCodeNotification(any(TraceCode.class));
        verify(traceCodeRepository).save(traceCode);
    }

    @Test
    void shouldTransitionToSuspect_whenScoreReachesOrExceedsThreshold() {
        // 10 lượt quét +30; đi xa +40; >= 5 địa điểm +15 => tổng 85 (SUSPECT).
        List<TraceCodeScanLog> scans = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 10 lượt quét khác nhau (>=5 vị trí khác nhau nhờ chênh lệch tọa độ > 0.5km).
        double[][] coords = {
                { 21.0285, 105.8542 }, // Hà Nội
                { 16.0544, 108.2022 }, // Đà Nẵng
                { 10.7769, 106.7009 }, // TP.HCM
                { 10.0452, 105.7469 }, // Cần Thơ
                { 12.2388, 109.1967 }, // Nha Trang
                { 20.8587, 106.6297 }, // Hải Phòng
                { 18.6796, 105.6813 }, // Vinh
                { 13.9819, 108.0000 }, // Pleiku
                { 11.9400, 108.4583 }, // Đà Lạt
                { 21.5900, 105.8500 }, // Thái Nguyên
        };
        for (int i = 0; i < 10; i++) {
            TraceCodeScanLog log = TraceCodeScanLog.builder()
                    .id(UUID.randomUUID())
                    .scannedAt(now.minusMinutes(10 - i))
                    .latitude(BigDecimal.valueOf(coords[i][0]))
                    .longitude(BigDecimal.valueOf(coords[i][1]))
                    .build();
            scans.add(log);
        }

        stubRecentScans(scans);

        service.evaluateSuspicion(traceCodeId);

        assertEquals(85, traceCode.getSuspicionScore());
        assertEquals(TraceCodeStatus.SUSPECT, traceCode.getStatus());
        // Không tự động khóa.
        assertNotEquals(TraceCodeStatus.LOCKED, traceCode.getStatus());
        verify(notificationService).sendSuspectTraceCodeNotification(traceCode);
    }

    @Test
    void shouldNotEvaluate_whenLessThanTwoScans() {
        List<TraceCodeScanLog> scans = new ArrayList<>();
        scans.add(scan(5, 21.0285, 105.8542));
        stubRecentScans(scans);

        service.evaluateSuspicion(traceCodeId);

        assertEquals(null, traceCode.getSuspicionScore());
        verify(traceCodeRepository, never()).save(any(TraceCode.class));
        verify(notificationService, never()).sendSuspectTraceCodeNotification(any(TraceCode.class));
    }

    @Test
    void impossibleTravelCategoryIsCappedAt40_whenMultiplePairsQualify() {
        // 3 lượt quét: A→B và B→C đều là di chuyển bất hợp lý (> 50 km, < 30 phút).
        // Điểm cho hạng mục impossible travel phải bị giới hạn ở +40, KHÔNG phải +80.
        List<TraceCodeScanLog> scans = new ArrayList<>();
        LocalDateTime t0 = LocalDateTime.now().minusMinutes(30);
        scans.add(TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .scannedAt(t0)
                .latitude(BigDecimal.valueOf(21.0285)) // Hà Nội
                .longitude(BigDecimal.valueOf(105.8542))
                .build());
        scans.add(TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .scannedAt(t0.plusMinutes(5)) // Đà Nẵng ~620km
                .latitude(BigDecimal.valueOf(16.0544))
                .longitude(BigDecimal.valueOf(108.2022))
                .build());
        scans.add(TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .scannedAt(t0.plusMinutes(10)) // TP.HCM ~1000km từ Đà Nẵng
                .latitude(BigDecimal.valueOf(10.7769))
                .longitude(BigDecimal.valueOf(106.7009))
                .build());
        stubRecentScans(scans);

        service.evaluateSuspicion(traceCodeId);

        assertEquals(40, traceCode.getSuspicionScore(),
                "impossible travel category phải bị giới hạn ở +40 dù có 2 cặp hợp lệ");

        // Bảng phân tích (getSuspectDetail) phải khớp: impossibleTravel = 40, tổng = 40.
        SuspectTraceCodeDetailResponse detail = service.getSuspectDetail(traceCodeId);
        assertEquals(40, detail.getAnomalyDetails().getScoreBreakdown().getImpossibleTravel());
        assertEquals(traceCode.getSuspicionScore(), sumBreakdown(detail),
                "Bảng phân tích phải khớp với điểm đã lưu");
    }

    @Test
    void shouldNotAutoLockTraceCode_duringEvaluation() {
        List<TraceCodeScanLog> scans = new ArrayList<>();
        scans.add(scan(10, 21.0285, 105.8542));
        scans.add(scan(8, 16.0544, 108.2022));
        stubRecentScans(scans);

        service.evaluateSuspicion(traceCodeId);

        assertNotEquals(TraceCodeStatus.LOCKED, traceCode.getStatus());
    }

    @Test
    void shouldProduceConsistentBreakdown_betweenEvaluateAndDetail() {
        // 2 lượt quét bất hợp lý về khoảng cách -> impossibleTravel = +40.
        List<TraceCodeScanLog> scans = new ArrayList<>();
        scans.add(scan(10, 21.0285, 105.8542));
        scans.add(scan(8, 16.0544, 108.2022));
        stubRecentScans(scans);

        service.evaluateSuspicion(traceCodeId);

        SuspectTraceCodeDetailResponse detail = service.getSuspectDetail(traceCodeId);

        assertNotNull(detail.getAnomalyDetails());
        assertEquals(traceCode.getSuspicionScore(), detail.getSuspicionScore());

        Integer breakdownTotal = sumBreakdown(detail);
        assertEquals(traceCode.getSuspicionScore(), breakdownTotal,
                "Bảng phân tích phải khớp với điểm đã lưu");
        assertEquals(40, detail.getAnomalyDetails().getScoreBreakdown().getImpossibleTravel());
    }

    private Integer sumBreakdown(SuspectTraceCodeDetailResponse detail) {
        var b = detail.getAnomalyDetails().getScoreBreakdown();
        return b.getHighFrequency() + b.getImpossibleTravel() + b.getMultipleLocations();
    }

    @Test
    void unlockTraceCodeWithVerification_happyPath_shouldUnlockSuccessfully() {
        // Arrange
        UUID adminId = UUID.randomUUID();
        User admin = User.builder().userId(adminId).userName("admin01").build();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        UUID lockerId = UUID.randomUUID();
        User locker = User.builder().userId(lockerId).userName("locker01").build();

        traceCode.setStatus(TraceCodeStatus.LOCKED);
        traceCode.setLockedBy(locker);
        traceCode.setLockedAt(LocalDateTime.now().minusDays(1));
        traceCode.setLockReason("Quét bất thường nhiều nơi");

        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Đã xác minh vận đơn và đối soát thực tế, không có dấu hiệu giả mạo.");
        request.setEvidence("Biên bản đối soát số 123");

        // Act
        var response = service.unlockTraceCodeWithVerification(traceCodeId.toString(), request, adminId, "Admin User");

        // Assert
        assertNotNull(response);
        assertEquals(TraceCodeStatus.ACTIVE.name(), response.getStatus());
        assertEquals("NCL0001", response.getCodeValue());
        assertEquals(adminId, response.getUnlockedBy());
        assertEquals("Admin User", response.getUnlockedByName());
        assertEquals(request.getConclusion(), response.getUnlockConclusion());
        assertEquals(request.getEvidence(), response.getUnlockEvidence());
        assertEquals(request.getConclusion(), response.getVerificationNote());
        assertEquals(TraceCodeStatus.ACTIVE, traceCode.getStatus());
        assertNotNull(traceCode.getUnlockedAt());

        verify(traceCodeRepository).save(traceCode);
        verify(notificationService).sendTraceCodeUnlockedNotification(traceCode);
        verify(eventPublisher).publishEvent(any(vn.nguongocso.alert.event.ActivityLogEvent.class));
    }

    @Test
    void unlockTraceCodeWithVerification_whenNotLocked_shouldThrowConflict() {
        // Arrange
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        UUID adminId = UUID.randomUUID();
        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Đã xác minh đầy đủ thông tin hợp lệ.");

        // Act & Assert
        vn.nguongocso.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                vn.nguongocso.exception.BusinessException.class,
                () -> service.unlockTraceCodeWithVerification(traceCodeId.toString(), request, adminId, "Admin User"));

        assertEquals("Mã tem không ở trạng thái bị khóa.", ex.getMessage());
    }

    @Test
    void unlockTraceCodeWithVerification_whenBlankConclusion_shouldThrowException() {
        // Arrange
        traceCode.setStatus(TraceCodeStatus.LOCKED);
        UUID adminId = UUID.randomUUID();
        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("   ");

        // Act & Assert
        vn.nguongocso.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                vn.nguongocso.exception.BusinessException.class,
                () -> service.unlockTraceCodeWithVerification(traceCodeId.toString(), request, adminId, "Admin User"));

        assertEquals("Kết luận xác minh không được để trống.", ex.getMessage());
    }

    @Test
    void unlockTraceCodeWithVerification_sameAdminLocked_shortConclusion_shouldThrowException() {
        // Arrange
        UUID adminId = UUID.randomUUID();
        User admin = User.builder().userId(adminId).userName("admin01").build();

        traceCode.setStatus(TraceCodeStatus.LOCKED);
        traceCode.setLockedBy(admin); // Same admin locked it

        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Xác minh xong"); // < 20 chars

        // Act & Assert
        vn.nguongocso.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                vn.nguongocso.exception.BusinessException.class,
                () -> service.unlockTraceCodeWithVerification(traceCodeId.toString(), request, adminId, "Admin User"));

        assertEquals("Quản trị viên đã khóa mã tem cần nhập kết luận xác minh chi tiết hơn (tối thiểu 20 ký tự).", ex.getMessage());
    }

    @Test
    void unlockTraceCodeWithVerification_sameAdminLocked_detailedConclusion_shouldSucceed() {
        // Arrange
        UUID adminId = UUID.randomUUID();
        User admin = User.builder().userId(adminId).userName("admin01").build();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        traceCode.setStatus(TraceCodeStatus.LOCKED);
        traceCode.setLockedBy(admin); // Same admin locked it

        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Đã kiểm tra lại toàn bộ nhật ký quét và hóa đơn phân phối hợp lệ."); // >= 20 chars

        // Act
        var response = service.unlockTraceCodeWithVerification(traceCodeId.toString(), request, adminId, "Admin User");

        // Assert
        assertNotNull(response);
        assertEquals(TraceCodeStatus.ACTIVE.name(), response.getStatus());
        verify(traceCodeRepository).save(traceCode);
    }
}