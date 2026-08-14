package vn.nguongocso.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.report.dto.response.AbnormalScanResponse;
import vn.nguongocso.report.dto.response.LookupStatisticsResponse;
import vn.nguongocso.report.service.LookupStatisticsService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller thống kê tra cứu mã truy xuất.
 *
 * @author Triệu Văn Đại
 */
@RestController
@RequestMapping("/api/v1/reports/lookup-statistics")
@RequiredArgsConstructor
public class LookupStatisticsController {
    private final LookupStatisticsService lookupStatisticsService;

    /**
     * Lấy thống kê tra cứu mã truy xuất.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<LookupStatisticsResponse>> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID productionLotId,
            @RequestParam(required = false) UUID shipmentId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false, defaultValue = "MONTH") String groupBy,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        LookupStatisticsResponse response = lookupStatisticsService.getStatistics(
                startDate, endDate, productionLotId, shipmentId, organizationId, groupBy, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy danh sách các lần quét bất thường.
     */
    @GetMapping("/abnormal")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<Page<AbnormalScanResponse>>> getAbnormalScans(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID productionLotId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AbnormalScanResponse> response = lookupStatisticsService.getAbnormalScans(
                startDate, endDate, productionLotId, organizationId, pageable, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
