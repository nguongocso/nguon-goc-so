package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.report.enums.MetricStatus;

/** DTO chi tiết chỉ số giám sát. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricItemDto {
    private String metricCode;

    private String metricName;

    private String value;

    private Double numericValue;

    private String threshold;

    private MetricStatus status;

    private String unit;

    private String message;
}
