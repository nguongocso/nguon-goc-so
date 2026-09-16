package vn.nguongocso.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Thống kê số lượng cảnh báo tổng hợp theo mức độ khẩn cấp và loại (NCL-08-CN-016).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateAlertCountResponse {
    private long totalOpen;
    private long highSeverityCount;
    private long mediumSeverityCount;
    private Map<String, Long> byTypeCounts;
}
