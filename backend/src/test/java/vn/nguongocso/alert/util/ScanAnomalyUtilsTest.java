package vn.nguongocso.alert.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import vn.nguongocso.report.entity.TraceCodeScanLog;

/**
 * Kiểm thử đơn vị cho lớp tiện ích ScanAnomalyUtils (NCL-08-CN-014).
 */
class ScanAnomalyUtilsTest {

    private TraceCodeScanLog scan(LocalDateTime time, Double lat, Double lon) {
        return TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .scannedAt(time)
                .latitude(lat != null ? BigDecimal.valueOf(lat) : null)
                .longitude(lon != null ? BigDecimal.valueOf(lon) : null)
                .build();
    }

    @Nested
    @DisplayName("Kiểm tra thời gian ân hạn (isWithinGracePeriod)")
    class GracePeriodTests {

        @Test
        @DisplayName("Trả về false khi thông tin kích hoạt hoặc tham số null")
        void shouldReturnFalse_whenParametersAreNull() {
            LocalDateTime now = LocalDateTime.now();
            assertFalse(ScanAnomalyUtils.isWithinGracePeriod(null, now, 7));
            assertFalse(ScanAnomalyUtils.isWithinGracePeriod(now, null, 7));
            assertFalse(ScanAnomalyUtils.isWithinGracePeriod(now, now, null));
            assertFalse(ScanAnomalyUtils.isWithinGracePeriod(now, now, 0));
            assertFalse(ScanAnomalyUtils.isWithinGracePeriod(now, now, -5));
        }

        @Test
        @DisplayName("Trả về true khi thời gian quét nằm trong thời gian ân hạn")
        void shouldReturnTrue_whenWithinGracePeriod() {
            LocalDateTime activatedAt = LocalDateTime.now().minusDays(3);
            LocalDateTime scanTime = LocalDateTime.now();
            // 3 ngày < 7 ngày ân hạn
            assertTrue(ScanAnomalyUtils.isWithinGracePeriod(activatedAt, scanTime, 7));
        }

        @Test
        @DisplayName("Trả về false khi thời gian quét đã vượt qua hoặc bằng thời gian ân hạn")
        void shouldReturnFalse_whenGracePeriodElapsed() {
            LocalDateTime activatedAt = LocalDateTime.now().minusDays(7);
            LocalDateTime scanTime = LocalDateTime.now();
            // 7 ngày >= 7 ngày ân hạn -> hết ân hạn
            assertFalse(ScanAnomalyUtils.isWithinGracePeriod(activatedAt, scanTime, 7));

            LocalDateTime activated10DaysAgo = LocalDateTime.now().minusDays(10);
            // 10 ngày >= 7 ngày ân hạn
            assertFalse(ScanAnomalyUtils.isWithinGracePeriod(activated10DaysAgo, scanTime, 7));
        }
    }

    @Nested
    @DisplayName("Kiểm tra tần suất quét cao theo cửa sổ trượt (isHighFrequency)")
    class HighFrequencyTests {

        @Test
        @DisplayName("Trả về false khi danh sách quét rỗng hoặc null")
        void shouldReturnFalse_whenListIsEmptyOrNull() {
            assertFalse(ScanAnomalyUtils.isHighFrequency(null, 5, 10));
            assertFalse(ScanAnomalyUtils.isHighFrequency(List.of(), 5, 10));
        }

        @Test
        @DisplayName("Trả về true khi số lượt quét trong 1 giờ >= maxPerHour")
        void shouldReturnTrue_whenExceedingMaxPerHour() {
            LocalDateTime base = LocalDateTime.now().minusHours(5);
            List<TraceCodeScanLog> scans = new ArrayList<>();
            // 5 lượt quét trong 20 phút (ngưỡng maxPerHour = 5)
            for (int i = 0; i < 5; i++) {
                scans.add(scan(base.plusMinutes(i * 4), 21.0285, 105.8542));
            }
            assertTrue(ScanAnomalyUtils.isHighFrequency(scans, 5, 10));
        }

        @Test
        @DisplayName("Trả về true khi số lượt quét trong 24 giờ >= maxPerDay")
        void shouldReturnTrue_whenExceedingMaxPerDay() {
            LocalDateTime base = LocalDateTime.now().minusHours(20);
            List<TraceCodeScanLog> scans = new ArrayList<>();
            // 10 lượt quét rải rác trong 18 giờ (mỗi 1.8 giờ 1 lượt, không vượt quá maxPerHour=5)
            for (int i = 0; i < 10; i++) {
                scans.add(scan(base.plusMinutes(i * 100), 21.0285, 105.8542));
            }
            // Không vượt 1h nhưng vượt 24h
            assertTrue(ScanAnomalyUtils.isHighFrequency(scans, 5, 10));
        }

        @Test
        @DisplayName("Trả về false khi các lượt quét rải rác nhiều ngày không vi phạm bất kỳ cửa sổ 24h hay 1h nào")
        void shouldReturnFalse_whenScansSpreadAcrossDays() {
            LocalDateTime base = LocalDateTime.now().minusDays(20);
            List<TraceCodeScanLog> scans = new ArrayList<>();
            // 14 lượt quét trải dài 28 ngày (mỗi 2 ngày 1 lượt)
            for (int i = 0; i < 14; i++) {
                scans.add(scan(base.plusDays(i * 2), 21.0285, 105.8542));
            }
            // Tổng 14 lượt nhưng trong bất kỳ cửa sổ 24h nào cũng chỉ có tối đa 1 lượt
            assertFalse(ScanAnomalyUtils.isHighFrequency(scans, 5, 10));
        }
    }

    @Nested
    @DisplayName("Kiểm tra di chuyển phi lý (isImpossibleTravel)")
    class ImpossibleTravelTests {

        @Test
        @DisplayName("Trả về false khi ít hơn 2 lượt quét hoặc thiếu tọa độ")
        void shouldReturnFalse_whenInsufficientScansOrMissingCoords() {
            LocalDateTime now = LocalDateTime.now();
            assertFalse(ScanAnomalyUtils.isImpossibleTravel(null, 50.0, 30));
            assertFalse(ScanAnomalyUtils.isImpossibleTravel(List.of(scan(now, 21.0, 105.0)), 50.0, 30));

            List<TraceCodeScanLog> noCoords = List.of(
                    scan(now.minusMinutes(10), null, null),
                    scan(now, null, null)
            );
            assertFalse(ScanAnomalyUtils.isImpossibleTravel(noCoords, 50.0, 30));
        }

        @Test
        @DisplayName("Trả về true khi 2 lượt quét cách xa hơn maxDistance trong thời gian <= minTime")
        void shouldReturnTrue_whenDistanceExceededWithinTimeWindow() {
            LocalDateTime now = LocalDateTime.now();
            // Hà Nội -> TP.HCM (~1150km) trong 20 phút (ngưỡng 50km trong 30 phút)
            List<TraceCodeScanLog> scans = List.of(
                    scan(now.minusMinutes(20), 21.0285, 105.8542), // Hà Nội
                    scan(now, 10.7769, 106.7009) // TP.HCM
            );
            assertTrue(ScanAnomalyUtils.isImpossibleTravel(scans, 50.0, 30));
        }

        @Test
        @DisplayName("Trả về false khi khoảng cách lớn nhưng thời gian di chuyển hợp lý (> minTime)")
        void shouldReturnFalse_whenDistanceLargeButTimeSufficient() {
            LocalDateTime now = LocalDateTime.now();
            // Hà Nội -> TP.HCM trong 120 phút (vượt quá minTime 30 phút nên không coi là impossible travel)
            List<TraceCodeScanLog> scans = List.of(
                    scan(now.minusMinutes(120), 21.0285, 105.8542),
                    scan(now, 10.7769, 106.7009)
            );
            assertFalse(ScanAnomalyUtils.isImpossibleTravel(scans, 50.0, 30));
        }
    }
}
