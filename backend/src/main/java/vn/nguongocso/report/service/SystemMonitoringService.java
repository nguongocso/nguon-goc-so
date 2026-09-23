package vn.nguongocso.report.service;

import java.util.List;

import vn.nguongocso.report.dto.response.MetricThresholdDto;
import vn.nguongocso.report.dto.response.SystemStatusResponse;

/** Service giám sát tình trạng hệ thống. */
public interface SystemMonitoringService {

    /**
     * Lấy tình trạng sức khỏe tổng thể của hệ thống thời gian thực.
     */
    SystemStatusResponse getSystemStatus();

    /**
     * Lấy danh sách các ngưỡng cài đặt giám sát hệ thống.
     */
    List<MetricThresholdDto> getMonitoringThresholds();
}
