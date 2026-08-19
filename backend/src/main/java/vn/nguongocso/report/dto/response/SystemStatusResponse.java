package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.report.enums.OverallSystemStatus;

import java.time.Instant;
import java.util.Map;

/**
 * DTO tổng hợp tình trạng sức khỏe hệ thống phục vụ trang giám sát trước buổi trình diễn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {
    /** Trạng thái tổng thể hệ thống: HEALTHY, WARNING, CRITICAL, INSUFFICIENT_DATA */
    private OverallSystemStatus overallStatus;

    /** Cờ đánh dấu hệ thống đã có đủ số liệu quan sát hay chưa (TC-04) */
    private boolean hasSufficientData;

    /** Thời gian hệ thống đã chạy tính theo giây (Uptime) */
    private long uptimeSeconds;

    /** Mốc thời gian cập nhật số liệu mới nhất */
    private Instant lastUpdated;

    /** Số lượng chỉ số đang vượt ngưỡng cảnh báo */
    private int breachedMetricsCount;

    /** Thông điệp tóm tắt tình trạng hệ thống */
    private String summaryMessage;

    /** Map chứa chi tiết 4 chỉ số giám sát (dbConnection, serverErrorCount, publicTraceAvgResponseTime, dataGatewayCallCount) */
    private Map<String, MetricItemDto> metrics;
}
