package vn.nguongocso.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.service.OpenDataExportService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Controller cung cấp API kết xuất dữ liệu mở theo lược đồ chuẩn.
 */
@RestController
@RequestMapping("/api/v1/reports/open-data")
@RequiredArgsConstructor
public class OpenDataExportController {
    private final OpenDataExportService openDataExportService;

    /**
     * API xuất dữ liệu mở theo lược đồ chuẩn.
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('VT-05')")
    public ResponseEntity<byte[]> exportOpenData(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<UUID> unitIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam String format,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {

        String ipAddress = extractClientIp(request);
        byte[] fileBytes = openDataExportService.exportOpenData(region, unitIds, fromDate, toDate, format,
                currentUser, ipAddress);

        String fileExtension = format.toLowerCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeRegion = (region == null || region.isBlank()) ? "phamvi" : region.replace(" ", "_");
        String fileName = "open_data_" + safeRegion + "_" + timestamp + "." + fileExtension;

        MediaType mediaType;
        if ("XML".equalsIgnoreCase(format)) {
            mediaType = MediaType.APPLICATION_XML;
        } else if ("CSV".equalsIgnoreCase(format)) {
            mediaType = new MediaType("text", "csv", StandardCharsets.UTF_8);
        } else {
            mediaType = MediaType.APPLICATION_JSON;
        }

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(mediaType)
                .body(fileBytes);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
