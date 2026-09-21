package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO kết quả tiến trình quét kiểm tra hạn hiệu lực kết quả kiểm nghiệm
 * (NCL-11-CN-004).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionScanResult {
    private int totalLotsScanned;

    private int validCount;

    private int expiringCount;

    private int expiredCount;

    private int skippedCount;

    private int alertsCreated;

    private int expiringAlertsCreated;

    private int expiredAlertsCreated;

    private int skippedDuplicateToday;

    private int notificationsSent;

    private LocalDate scanDate;

    private String message;
}
