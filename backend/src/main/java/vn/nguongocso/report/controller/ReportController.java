package vn.nguongocso.report.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.report.dto.response.IndustryReportResponse;
import vn.nguongocso.report.service.ReportService;

/**
 * API cung cấp báo cáo tổng hợp ngành (dạng JSON, PDF và Excel).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    /**
     * Lấy báo cáo tổng hợp ngành dưới dạng JSON.
     *
     * <p>
     * {@code region} tuỳ chọn với cán bộ quản lý ngành (VT-05) đã được gán địa
     * bàn; {@code unitIds} lặp được ({@code ?unitIds=<uuid>&unitIds=<uuid>}).
     * </p>
     */
    @GetMapping("/industry-summary")
    public ResponseEntity<IndustryReportResponse> getIndustrySummary(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<UUID> unitIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        return ResponseEntity.ok(
                reportService.getIndustrySummary(region, unitIds, fromDate, toDate));
    }

    /**
     * Xuất báo cáo tổng hợp ngành sang file PDF hoặc Excel.
     * Mặc định format = PDF khi không truyền tham số.
     */
    @GetMapping("/industry-summary/export")
    public ResponseEntity<byte[]> exportIndustrySummary(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<UUID> unitIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "PDF") String format) {

        String normalizedFormat = format == null ? "PDF" : format.trim().toUpperCase();

        byte[] file = reportService.exportIndustrySummary(region, unitIds, fromDate, toDate, normalizedFormat);

        MediaType contentType;
        String fileName;
        if ("EXCEL".equals(normalizedFormat) || "XLSX".equals(normalizedFormat)) {
            contentType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            fileName = "industry-summary.xlsx";
        } else if ("PDF".equals(normalizedFormat)) {
            contentType = MediaType.APPLICATION_PDF;
            fileName = "industry-summary.pdf";
        } else {
            log.warn("Unsupported export format requested: {}, fallback to PDF.", format);
            contentType = MediaType.APPLICATION_PDF;
            fileName = "industry-summary.pdf";
        }

        ContentDisposition disposition = ContentDisposition
                .attachment()
                .filename(fileName)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(contentType)
                .contentLength(file.length)
                .body(file);
    }
}
