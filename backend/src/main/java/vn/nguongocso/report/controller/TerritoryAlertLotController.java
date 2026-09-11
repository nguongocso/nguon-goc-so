package vn.nguongocso.report.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.organization.service.AreaScopeService;
import vn.nguongocso.report.dto.response.AlertLotDetailResponse;
import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;
import vn.nguongocso.report.enums.LotAlertType;
import vn.nguongocso.report.service.TerritoryLotAlertService;

/**
 * Controller quản lý danh sách và chi tiết các lô có cảnh báo theo địa bàn cho Cán bộ quản lý ngành (NCL-07-CN-006).
 */
@RestController
@RequestMapping("/api/v1/reports/alert-lots")
@RequiredArgsConstructor
public class TerritoryAlertLotController {

    private final TerritoryLotAlertService territoryLotAlertService;

    /**
     * Lấy danh sách lô có cảnh báo theo địa bàn phụ trách.
     */
    @GetMapping
    @PreAuthorize("hasRole('VT-05') or hasRole('VT-01')")
    public ResponseEntity<ApiResult<PageResponse<AlertLotSummaryResponse>>> getAlertLots(
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) List<UUID> unitIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latestAlertTriggeredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        LotAlertType parsedAlertType = LotAlertType.fromString(alertType);

        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<AlertLotSummaryResponse> result = territoryLotAlertService.getAlertLots(
                currentUser, parsedAlertType, organizationId, fromDate, toDate, unitIds, pageable);

        String message = (result.getTotalElements() == 0 && result.getItems().isEmpty())
                ? AreaScopeService.UNASSIGNED_MESSAGE
                : "Truy vấn danh sách lô có cảnh báo thành công.";

        ApiResult<PageResponse<AlertLotSummaryResponse>> apiResult = ApiResult.<PageResponse<AlertLotSummaryResponse>>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(result)
                .build();

        return ResponseEntity.ok(apiResult);
    }

    /**
     * Lấy chi tiết một lô có cảnh báo (Read-only).
     */
    @GetMapping("/{lotId}")
    @PreAuthorize("hasRole('VT-05') or hasRole('VT-01')")
    public ResponseEntity<ApiResult<AlertLotDetailResponse>> getAlertLotDetail(
            @PathVariable UUID lotId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        AlertLotDetailResponse detail = territoryLotAlertService.getAlertLotDetail(currentUser, lotId);

        ApiResult<AlertLotDetailResponse> apiResult = ApiResult.<AlertLotDetailResponse>builder()
                .success(true)
                .status(200)
                .message("Lấy chi tiết lô có cảnh báo thành công.")
                .data(detail)
                .build();

        return ResponseEntity.ok(apiResult);
    }

    /**
     * Xuất file Excel danh sách lô có cảnh báo theo địa bàn.
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('VT-05') or hasRole('VT-01')")
    public ResponseEntity<byte[]> exportAlertLots(
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) List<UUID> unitIds,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        LotAlertType parsedAlertType = LotAlertType.fromString(alertType);

        byte[] excelBytes = territoryLotAlertService.exportAlertLots(
                currentUser, parsedAlertType, organizationId, fromDate, toDate, unitIds);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "Danh_sach_lo_canh_bao_" + timestamp + ".xlsx";

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
