package vn.nguongocso.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.report.dto.response.MetricThresholdDto;
import vn.nguongocso.report.dto.response.SystemStatusResponse;
import vn.nguongocso.report.service.SystemMonitoringService;

import java.util.List;

/**
 * Controller cung cấp API Giám sát tình trạng hệ thống thời gian thực trước buổi trình diễn.
 *
 * <p><strong>Story ID:</strong> NCL-10-CN-010</p>
 * <p><strong>Role yêu cầu:</strong> VT-01 (Quản trị viên nền tảng)</p>
 */
@RestController
@RequestMapping("/api/v1/admin/monitoring")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VT-01')")
public class SystemMonitoringController {

    private final SystemMonitoringService systemMonitoringService;

    /**
     * API Lấy tình trạng sức khỏe tổng thể và 4 chỉ số giám sát hệ thống trong 1 giờ gần nhất.
     *
     * @return ApiResult chứa SystemStatusResponse
     */
    @GetMapping("/system-status")
    public ResponseEntity<ApiResult<SystemStatusResponse>> getSystemStatus() {
        SystemStatusResponse status = systemMonitoringService.getSystemStatus();
        return ResponseEntity.ok(ApiResult.success(status));
    }

    /**
     * API Lấy danh sách cấu hình các ngưỡng giám sát hệ thống.
     *
     * @return ApiResult chứa danh sách MetricThresholdDto
     */
    @GetMapping("/thresholds")
    public ResponseEntity<ApiResult<List<MetricThresholdDto>>> getThresholds() {
        List<MetricThresholdDto> thresholds = systemMonitoringService.getMonitoringThresholds();
        return ResponseEntity.ok(ApiResult.success(thresholds));
    }
}
