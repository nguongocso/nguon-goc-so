package vn.nguongocso.export.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.dto.response.OpenDataExportJobResponse;
import vn.nguongocso.export.service.ExportService;
import vn.nguongocso.export.service.OpenDataAsyncExportService;
import vn.nguongocso.export.service.ProfileTemplateService;

/** Controller phụ trách xuất dữ liệu công khai và hồ sơ truy xuất theo mẫu đối tác. */
@Slf4j
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {
    private final ExportService exportService;
    private final ProfileTemplateService profileTemplateService;
    private final OpenDataAsyncExportService openDataAsyncExportService;

    /** Xuất dữ liệu open data theo định dạng yêu cầu (hỗ trợ cả đồng bộ và bất đồng bộ qua tham số async). */
    @PostMapping("/open-data")
    @PreAuthorize("hasRole('VT-05')")
    public ResponseEntity<?> exportOpenData(
            @Valid @RequestBody ExportOpenDataRequest request,
            @RequestParam(defaultValue = "false") boolean async,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        log.info("Nhận yêu cầu xuất open data: user={}, async={}",
                currentUser != null ? currentUser.getUsername() : "anonymous", async);

        if (async) {
            OpenDataExportJobResponse jobResponse = openDataAsyncExportService.submitJob(request, currentUser);
            return ResponseEntity.accepted().body(ApiResult.success(jobResponse));
        }

        Resource file = exportService.exportOpenData(request, currentUser);

        String format = request.getFormat() != null ? request.getFormat().toLowerCase() : "json";
        String contentType = switch (format) {
            case "csv" -> "text/csv";
            case "excel", "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> MediaType.APPLICATION_JSON_VALUE;
        };

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "export_" + timestamp + "." + format;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(file);
    }

    /** Lấy trạng thái của tác vụ xuất dữ liệu bất đồng bộ. */
    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-05')")
    public ResponseEntity<ApiResult<OpenDataExportJobResponse>> getExportJob(@PathVariable UUID jobId) {
        OpenDataExportJobResponse response = openDataAsyncExportService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /** Tải về tệp kết xuất của tác vụ đã hoàn thành. */
    @GetMapping("/jobs/{jobId}/download")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-05')")
    public ResponseEntity<Resource> downloadExportJob(@PathVariable UUID jobId) {
        OpenDataExportJobResponse job = openDataAsyncExportService.getJobStatus(jobId);
        Resource file = openDataAsyncExportService.getJobDownload(jobId);

        String format = job.getFormat() != null ? job.getFormat().toLowerCase() : "json";
        String contentType = switch (format) {
            case "csv" -> "text/csv";
            case "xml" -> MediaType.APPLICATION_XML_VALUE;
            case "excel", "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> MediaType.APPLICATION_JSON_VALUE;
        };

        String fileName = job.getFileName() != null ? job.getFileName() : ("export_" + jobId + "." + format);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(file);
    }

    /** Xem trước nội dung hồ sơ truy xuất áp dụng mẫu cấu hình trước khi xuất. */
    @GetMapping({"/shipments/{shipmentId}/preview", "/open-data/shipments/{shipmentId}/preview"})
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<Map<String, Object>>> previewProfileTemplate(
            @PathVariable UUID shipmentId,
            @RequestParam(name = "templateId", required = false) UUID templateId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        log.info("Nhận yêu cầu xem trước hồ sơ theo mẫu: shipmentId={}, templateId={}, user={}",
                shipmentId, templateId, currentUser != null ? currentUser.getUsername() : "anonymous");
        Map<String, Object> previewData = profileTemplateService.buildPreview(shipmentId, templateId, currentUser);
        log.info(
                "Xây dựng dữ liệu xem trước thành công: shipmentId={}, số nhóm thuộc tính={}",
                shipmentId,
                previewData.size());
        return ResponseEntity.ok(ApiResult.success(previewData));
    }

    /** Xuất hồ sơ truy xuất lô hàng theo mẫu đối tác đã cấu hình (NCL-07-CN-007). */
    @GetMapping("/shipments/{shipmentId}")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<Resource> exportWithTemplate(
            @PathVariable UUID shipmentId,
            @RequestParam(name = "templateId", required = false) UUID templateId,
            @RequestParam(name = "format", defaultValue = "json") String format,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Resource file = exportService.exportWithTemplate(shipmentId, templateId, format, currentUser);

        String normalizedFormat = format != null ? format.toLowerCase() : "json";
        String contentType = "csv".equals(normalizedFormat) ? "text/csv" : MediaType.APPLICATION_JSON_VALUE;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "dossier_profile_" + shipmentId + "_" + timestamp + "." + normalizedFormat;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(file);
    }
}
