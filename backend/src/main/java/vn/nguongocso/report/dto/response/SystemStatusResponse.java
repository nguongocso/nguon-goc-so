package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.report.enums.OverallSystemStatus;

import java.time.Instant;
import java.util.Map;

/**
 * DTO chứa kết quả tổng hợp tình trạng sức khỏe hệ thống.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {
    private OverallSystemStatus overallStatus;
    private boolean hasSufficientData;
    private long uptimeSeconds;
    private Instant lastUpdated;
    private int breachedMetricsCount;
    private String summaryMessage;
    private Map<String, MetricItemDto> metrics;
}
