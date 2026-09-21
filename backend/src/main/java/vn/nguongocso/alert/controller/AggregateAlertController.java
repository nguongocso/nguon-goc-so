package vn.nguongocso.alert.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.alert.dto.response.AggregateAlertCountResponse;
import vn.nguongocso.alert.dto.response.AggregateAlertPageResponse;
import vn.nguongocso.alert.dto.response.UnviewedAlertCountResponse;
import vn.nguongocso.alert.service.AggregateAlertService;
import vn.nguongocso.common.ApiResult;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller cung cấp các endpoint cảnh báo tổng hợp (NCL-08-CN-016).
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AggregateAlertController {
    private final AggregateAlertService aggregateAlertService;

    /**
     * Lấy danh sách cảnh báo tổng hợp gom từ cả 7 nguồn dữ liệu.
     */
    @GetMapping("/aggregate")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<AggregateAlertPageResponse>> getAggregateAlerts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Pageable pageable = PageRequest.of(page, size);
        AggregateAlertPageResponse response = aggregateAlertService.getAggregateAlerts(
                type,
                severity,
                status,
                organizationId,
                keyword,
                fromDate,
                toDate,
                pageable);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy số liệu thống kê tổng hợp số lượng cảnh báo đang mở theo mức khẩn cấp và loại.
     */
    @GetMapping("/aggregate/counts")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<AggregateAlertCountResponse>> getAggregateAlertCounts(
            @RequestParam(required = false) UUID organizationId) {

        AggregateAlertCountResponse response = aggregateAlertService.getAggregateAlertCounts(organizationId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy số lượng cảnh báo chưa xử lý phục vụ huy hiệu đếm trên thanh điều hướng.
     */
    @GetMapping("/unviewed-count")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<UnviewedAlertCountResponse>> getUnviewedAlertCount() {
        UnviewedAlertCountResponse response = aggregateAlertService.getUnviewedAlertCount();
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
