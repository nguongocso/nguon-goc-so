package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO kết quả tiến trình quét kiểm tra hạn hiệu lực kết quả kiểm nghiệm (NCL-11-CN-004).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionScanResult {

    /**
     * Tổng số lô sản xuất được quét.
     */
    private int totalLotsScanned;

    /**
     * Số lô có kết quả kiểm nghiệm còn hạn dài (> ngưỡng).
     */
    private int validCount;

    /**
     * Số lô có kết quả kiểm nghiệm sắp hết hạn.
     */
    private int expiringCount;

    /**
     * Số lô có kết quả kiểm nghiệm đã hết hạn.
     */
    private int expiredCount;

    /**
     * Số lô bị bỏ qua (đã kích hoạt hết tem, trạng thái loại trừ, trùng trong ngày, ...).
     */
    private int skippedCount;

    /**
     * Tổng số cảnh báo mới được tạo.
     */
    private int alertsCreated;

    /**
     * Số lượng cảnh báo sắp hết hạn mới được tạo.
     */
    private int expiringAlertsCreated;

    /**
     * Số lượng cảnh báo đã hết hạn mới được tạo.
     */
    private int expiredAlertsCreated;

    /**
     * Số lượng cảnh báo được bỏ qua do đã gửi cảnh báo cùng loại trong ngày.
     */
    private int skippedDuplicateToday;

    /**
     * Tổng số notification đã gửi.
     */
    private int notificationsSent;

    /**
     * Ngày quét.
     */
    private LocalDate scanDate;

    /**
     * Thông điệp kết quả thực thi.
     */
    private String message;
}
