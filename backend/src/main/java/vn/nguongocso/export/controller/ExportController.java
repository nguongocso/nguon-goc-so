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
import vn.nguongocso.export.service.ExportService;
import vn.nguongocso.export.service.ProfileTemplateService;

/**
 * Controller phụ trách xuất dữ liệu công khai và hồ sơ truy xuất theo mẫu đối tác.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final ProfileTemplateService profileTemplateService;

    /**
     * Xuất dữ liệu open data theo định dạng yêu cầu.
     */
    @PostMapping("/open-data")
    @PreAuthorize("hasRole('VT-05')")
    public ResponseEntity<Resource> exportOpenData(
            @Valid @RequestBody ExportOpenDataRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        log.info("Nhận yêu cầu xuất open data: user={}", currentUser != null ? currentUser.getUsername() : "anonymous");
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

    /**
     * Xem trước nội dung hồ sơ truy xuất áp dụng mẫu cấu hình trước khi xuất.
     * Hỗ trợ cả /shipments/{shipmentId}/preview và /open-data/shipments/{shipmentId}/preview.
     *
     * @param shipmentId  ID lô hàng
     * @param templateId  ID mẫu hồ sơ (tùy chọn)
     * @param currentUser Người dùng hiện tại
     * @return Dữ liệu hồ sơ xem trước đã lọc theo trường của mẫu
     */
    @GetMapping({"/shipments/{shipmentId}/preview", "/open-data/shipments/{shipmentId}/preview"})
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<Map<String, Object>>> previewProfileTemplate(
            @PathVariable UUID shipmentId,
            @RequestParam(name = "templateId", required = false) UUID templateId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        log.info("Nhận yêu cầu xem trước hồ sơ theo mẫu: shipmentId={}, templateId={}, user={}",
                shipmentId, templateId, currentUser != null ? currentUser.getUsername() : "anonymous");
        Map<String, Object> previewData = profileTemplateService.buildPreview(shipmentId, templateId, currentUser);
        log.info("Xây dựng dữ liệu xem trước thành công: shipmentId={}, số nhóm thuộc tính={}", shipmentId, previewData.size());
        return ResponseEntity.ok(ApiResult.success(previewData));
    }

    /**
     * Xuất hồ sơ truy xuất lô hàng theo mẫu đối tác đã cấu hình (NCL-07-CN-007).
     *
     * @param shipmentId  ID lô hàng
     * @param templateId  ID mẫu hồ sơ (nếu null, dùng mẫu mặc định của tổ chức - TC-03)
     * @param format      Định dạng tệp: json hoặc csv
     * @param currentUser Người dùng hiện tại
     * @return Tệp dữ liệu hồ sơ
     */
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