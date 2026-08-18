package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.report.enums.MetricStatus;

/**
 * DTO chứa thông tin chi tiết và trạng thái đánh giá của từng chỉ số giám sát.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricItemDto {
    /** Mã định danh chỉ số (ví dụ: DB_CONNECTION, SERVER_ERRORS) */
    private String metricCode;

    /** Tên hiển thị của chỉ số */
    private String metricName;

    /** Giá trị hiển thị chuỗi (ví dụ: "25", "180 ms", "UP", "N/A") */
    private String value;

    /** Giá trị số tính toán (null nếu chưa đủ dữ liệu) */
    private Double numericValue;

    /** Ngưỡng cảnh báo quy định (ví dụ: "20.0") */
    private String threshold;

    /** Trạng thái đánh giá chỉ số: NORMAL, WARNING, CRITICAL, INSUFFICIENT_DATA */
    private MetricStatus status;

    /** Đơn vị đo đạc (ví dụ: "lỗi/giờ", "ms", "lượt/giờ") */
    private String unit;

    /** Thông điệp giải thích chi tiết về chỉ số */
    private String message;
}
