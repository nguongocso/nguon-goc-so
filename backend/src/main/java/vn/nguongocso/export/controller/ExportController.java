package vn.nguongocso.export.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.service.ExportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
/** Xuất dữ liệu công khai ra tệp tải về. */
public class ExportController {

    private final ExportService exportService;

    /** Xuất dữ liệu open data theo định dạng yêu cầu. */
    @PostMapping("/open-data")
    @PreAuthorize("hasRole('VT-05')")
    public ResponseEntity<Resource> exportOpenData(
            @Valid @RequestBody ExportOpenDataRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Resource file = exportService.exportOpenData(request, currentUser);

        String format = request.getFormat() != null ? request.getFormat().toLowerCase() : "json";
        String contentType = switch (format) {
            case "xml" -> MediaType.APPLICATION_XML_VALUE;
            case "csv" -> "text/csv";
            default -> MediaType.APPLICATION_JSON_VALUE;
        };

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "export_" + timestamp + "." + format;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(file);
    }
}