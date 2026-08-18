package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO mô tả thông tin cấu hình ngưỡng quy định của một chỉ số giám sát.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricThresholdDto {
    /** Mã định danh chỉ số */
    private String metricCode;

    /** Tên hiển thị chỉ số */
    private String metricName;

    /** Giá trị ngưỡng cảnh báo quy định */
    private String thresholdValue;

    /** Đơn vị tính của ngưỡng */
    private String unit;

    /** Mô tả chi tiết ý nghĩa của ngưỡng */
    private String description;
}
