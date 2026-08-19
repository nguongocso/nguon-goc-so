package vn.nguongocso.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.nguongocso.report.dto.response.MetricItemDto;
import vn.nguongocso.report.dto.response.MetricThresholdDto;
import vn.nguongocso.report.dto.response.SystemStatusResponse;
import vn.nguongocso.report.enums.MetricStatus;
import vn.nguongocso.report.enums.OverallSystemStatus;
import vn.nguongocso.report.service.MetricsBufferService;
import vn.nguongocso.report.service.SystemMonitoringService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMonitoringServiceImpl implements SystemMonitoringService {

    public static final double SERVER_ERROR_THRESHOLD = 20.0;
    public static final double PUBLIC_TRACE_LATENCY_THRESHOLD_MS = 2000.0;
    public static final double DATA_GATEWAY_CALLS_THRESHOLD = 1000.0;

    private final DataSource dataSource;
    private final MetricsBufferService metricsBufferService;

    @Override
    public SystemStatusResponse getSystemStatus() {
        boolean hasEnoughData = metricsBufferService.hasSufficientData();
        long uptime = metricsBufferService.getUptimeSeconds();

        Map<String, MetricItemDto> metricsMap = new LinkedHashMap<>();
        int breachedCount = 0;
        boolean hasCritical = false;

        // 1. Chỉ số Kết nối CSDL
        boolean isDbHealthy = checkDbConnection();
        MetricStatus dbStatus = isDbHealthy ? MetricStatus.NORMAL : MetricStatus.CRITICAL;
        if (!isDbHealthy) {
            hasCritical = true;
            breachedCount++;
        }

        metricsMap.put("dbConnection", MetricItemDto.builder()
                .metricCode("DB_CONNECTION")
                .metricName("Trạng thái kết nối CSDL")
                .value(isDbHealthy ? "UP" : "DOWN")
                .numericValue(isDbHealthy ? 1.0 : 0.0)
                .threshold("UP")
                .status(dbStatus)
                .unit("STATUS")
                .message(isDbHealthy ? "Kết nối CSDL MySQL ổn định." : "MẤT KẾT NỐI CƠ SỞ DỮ LIỆU MYSQL!")
                .build());

        // 2. Chỉ số Lỗi máy chủ (5xx)
        long serverErrors = metricsBufferService.getServerErrorCountLastHour();
        MetricStatus serverErrorStatus;
        String serverErrorMessage;
        if (!hasEnoughData) {
            serverErrorStatus = MetricStatus.INSUFFICIENT_DATA;
            serverErrorMessage = "Chưa đủ số liệu quan sát trong 1 giờ gần nhất.";
        } else if (serverErrors > SERVER_ERROR_THRESHOLD) {
            serverErrorStatus = MetricStatus.WARNING;
            serverErrorMessage = String.format("Số lỗi máy chủ trong 1 giờ qua (%d lỗi) đã vượt ngưỡng cảnh báo quy định (%.0f lỗi).",
                    serverErrors, SERVER_ERROR_THRESHOLD);
            breachedCount++;
        } else {
            serverErrorStatus = MetricStatus.NORMAL;
            serverErrorMessage = String.format("Số lỗi máy chủ nằm trong giới hạn an toàn (%d/%.0f).", serverErrors, SERVER_ERROR_THRESHOLD);
        }

        metricsMap.put("serverErrorCount", MetricItemDto.builder()
                .metricCode("SERVER_ERRORS")
                .metricName("Số lỗi máy chủ (1 giờ gần nhất)")
                .value(hasEnoughData ? String.valueOf(serverErrors) : "N/A")
                .numericValue(hasEnoughData ? (double) serverErrors : null)
                .threshold(String.valueOf(SERVER_ERROR_THRESHOLD))
                .status(serverErrorStatus)
                .unit("lỗi/giờ")
                .message(serverErrorMessage)
                .build());

        // 3. Chỉ số Latency tra cứu công khai
        double avgLatency = metricsBufferService.getPublicTraceAvgLatencyLastHour();
        MetricStatus latencyStatus;
        String latencyMessage;
        if (!hasEnoughData || avgLatency < 0) {
            latencyStatus = MetricStatus.INSUFFICIENT_DATA;
            latencyMessage = "Chưa đủ lượt truy cập để tính thời gian phản hồi trung bình.";
        } else if (avgLatency > PUBLIC_TRACE_LATENCY_THRESHOLD_MS) {
            latencyStatus = MetricStatus.WARNING;
            latencyMessage = String.format("Thời gian phản hồi TB (%.0f ms) đã vượt ngưỡng cho phép (%.0f ms).",
                    avgLatency, PUBLIC_TRACE_LATENCY_THRESHOLD_MS);
            breachedCount++;
        } else {
            latencyStatus = MetricStatus.NORMAL;
            latencyMessage = String.format("Thời gian phản hồi nằm trong giới hạn an toàn (%.0f ms).", avgLatency);
        }

        metricsMap.put("publicTraceAvgResponseTime", MetricItemDto.builder()
                .metricCode("PUBLIC_TRACE_LATENCY")
                .metricName("Thời gian phản hồi TB tra cứu công khai")
                .value(hasEnoughData && avgLatency >= 0 ? String.format("%.0f ms", avgLatency) : "N/A")
                .numericValue(hasEnoughData && avgLatency >= 0 ? avgLatency : null)
                .threshold(String.valueOf(PUBLIC_TRACE_LATENCY_THRESHOLD_MS))
                .status(latencyStatus)
                .unit("ms")
                .message(latencyMessage)
                .build());

        // 4. Chỉ số Lượt gọi Cổng dữ liệu
        long dataGatewayCalls = metricsBufferService.getDataGatewayCallCountLastHour();
        MetricStatus gatewayStatus;
        String gatewayMessage;
        if (!hasEnoughData) {
            gatewayStatus = MetricStatus.INSUFFICIENT_DATA;
            gatewayMessage = "Chưa đủ số liệu lượt gọi Cổng dữ liệu.";
        } else if (dataGatewayCalls > DATA_GATEWAY_CALLS_THRESHOLD) {
            gatewayStatus = MetricStatus.WARNING;
            gatewayMessage = String.format("Số lượt gọi Cổng dữ liệu (%d lượt) đã vượt ngưỡng cho phép (%.0f lượt).",
                    dataGatewayCalls, DATA_GATEWAY_CALLS_THRESHOLD);
            breachedCount++;
        } else {
            gatewayStatus = MetricStatus.NORMAL;
            gatewayMessage = String.format("Lượt gọi Cổng dữ liệu bình thường (%d/%.0f).", dataGatewayCalls, DATA_GATEWAY_CALLS_THRESHOLD);
        }

        metricsMap.put("dataGatewayCallCount", MetricItemDto.builder()
                .metricCode("DATA_GATEWAY_CALLS")
                .metricName("Số lượt gọi Cổng dữ liệu (1 giờ)")
                .value(hasEnoughData ? String.valueOf(dataGatewayCalls) : "N/A")
                .numericValue(hasEnoughData ? (double) dataGatewayCalls : null)
                .threshold(String.valueOf(DATA_GATEWAY_CALLS_THRESHOLD))
                .status(gatewayStatus)
                .unit("lượt/giờ")
                .message(gatewayMessage)
                .build());

        // Xác định trạng thái tổng thể
        OverallSystemStatus overallStatus;
        String summary;

        if (!hasEnoughData) {
            overallStatus = OverallSystemStatus.INSUFFICIENT_DATA;
            summary = "Hệ thống vừa khởi động lại, đang thu thập số liệu giám sát...";
        } else if (hasCritical) {
            overallStatus = OverallSystemStatus.CRITICAL;
            summary = "NGUY HẠI: Hệ thống gặp sự cố nghiêm trọng (Mất kết nối CSDL)!";
        } else if (breachedCount > 0) {
            overallStatus = OverallSystemStatus.WARNING;
            summary = String.format("CẢNH BÁO: Phát hiện %d chỉ số vượt ngưỡng cho phép trước buổi trình diễn!", breachedCount);
        } else {
            overallStatus = OverallSystemStatus.HEALTHY;
            summary = "Hệ thống hoạt động bình thường và sẵn sàng cho buổi trình diễn.";
        }

        return SystemStatusResponse.builder()
                .overallStatus(overallStatus)
                .hasSufficientData(hasEnoughData)
                .uptimeSeconds(uptime)
                .lastUpdated(Instant.now())
                .breachedMetricsCount(breachedCount)
                .summaryMessage(summary)
                .metrics(metricsMap)
                .build();
    }

    @Override
    public List<MetricThresholdDto> getMonitoringThresholds() {
        return List.of(
                MetricThresholdDto.builder()
                        .metricCode("DB_CONNECTION")
                        .metricName("Trạng thái kết nối CSDL")
                        .thresholdValue("UP")
                        .unit("STATUS")
                        .description("Yêu cầu kết nối CSDL phải sẵn sàng (UP)")
                        .build(),
                MetricThresholdDto.builder()
                        .metricCode("SERVER_ERRORS")
                        .metricName("Số lỗi máy chủ (1 giờ gần nhất)")
                        .thresholdValue(String.valueOf(SERVER_ERROR_THRESHOLD))
                        .unit("lỗi/giờ")
                        .description("Tối đa 20 lỗi 5xx trong 60 phút")
                        .build(),
                MetricThresholdDto.builder()
                        .metricCode("PUBLIC_TRACE_LATENCY")
                        .metricName("Thời gian phản hồi TB tra cứu công khai")
                        .thresholdValue(String.valueOf(PUBLIC_TRACE_LATENCY_THRESHOLD_MS))
                        .unit("ms")
                        .description("Tối đa 2000ms latency trung bình")
                        .build(),
                MetricThresholdDto.builder()
                        .metricCode("DATA_GATEWAY_CALLS")
                        .metricName("Số lượt gọi Cổng dữ liệu (1 giờ)")
                        .thresholdValue(String.valueOf(DATA_GATEWAY_CALLS_THRESHOLD))
                        .unit("lượt/giờ")
                        .description("Tối đa 1000 lượt gọi vào Cổng dữ liệu trong 60 phút")
                        .build()
        );
    }

    private boolean checkDbConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            return false;
        }
    }
}
