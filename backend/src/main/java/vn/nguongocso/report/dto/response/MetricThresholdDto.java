package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO cấu hình ngưỡng giám sát. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricThresholdDto {
    private String metricCode;

    private String metricName;

    private String thresholdValue;

    private String unit;

    private String description;
}
