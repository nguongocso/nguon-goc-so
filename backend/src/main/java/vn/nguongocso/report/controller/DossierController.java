package vn.nguongocso.report.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.export.service.ProfileTemplateService;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.report.dto.request.BatchDossierCheckRequest;
import vn.nguongocso.report.dto.request.BatchDossierExportRequest;
import vn.nguongocso.report.dto.response.BatchDossierCheckResponse;
import vn.nguongocso.report.dto.response.BatchDossierHistoryDto;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;
import vn.nguongocso.report.exception.DossierValidationException;
import vn.nguongocso.report.service.DossierService;

/**
 * Controller quản lý hồ sơ truy xuất.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class DossierController {

    private final DossierService dossierService;
    private final PermissionChecker permissionChecker;
    private final ProfileTemplateService profileTemplateService;

    /**
     * API Kiểm tra điều kiện xuất hồ sơ truy xuất.
     */
    @GetMapping("/{shipmentId}/dossier/check")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<DossierCheckResponse>> checkEligibility(
            @PathVariable UUID shipmentId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        permissionChecker.check("SHIPMENT", "READ");
        DossierCheckResponse response = dossierService.checkEligibility(shipmentId, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * API Xuất và tải hồ sơ truy xuất nguồn gốc dưới dạng file PDF.
     */
    @GetMapping("/{shipmentId}/dossier/export")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<byte[]> exportDossierPdf(
            @PathVariable UUID shipmentId,
            @RequestParam(name = "templateId", required = false) UUID templateId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {
        permissionChecker.check("SHIPMENT", "READ");
        String ipAddress = extractClientIp(request);
        byte[] pdfBytes = templateId != null
                ? dossierService.exportDossierPdf(shipmentId, templateId, currentUser, ipAddress)
                : dossierService.exportDossierPdf(shipmentId, currentUser, ipAddress);

        String rawFileName = "Ho_so_truy_xuat_" + shipmentId + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(rawFileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * API Xem trước hồ sơ truy xuất áp dụng mẫu cấu hình trường dữ liệu đối tác.
     */
    @GetMapping("/{shipmentId}/dossier/preview")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<Map<String, Object>>> previewDossierWithTemplate(
            @PathVariable UUID shipmentId,
            @RequestParam(name = "templateId", required = false) UUID templateId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        permissionChecker.check("SHIPMENT", "READ");
        Map<String, Object> previewData = profileTemplateService.buildPreview(shipmentId, templateId, currentUser);
        return ResponseEntity.ok(ApiResult.success(previewData));
    }

    /**
     * API Xuất hồ sơ truy xuất theo lược đồ GS1 mô phỏng.
     */
    @GetMapping("/{shipmentId}/dossier/gs1")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-04')")
    public ResponseEntity<?> exportGs1Dossier(
            @PathVariable UUID shipmentId,
            @RequestParam(name = "format", defaultValue = "json") String format,
            @RequestParam(name = "includeMapping", defaultValue = "true") boolean includeMapping,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {
        permissionChecker.check("SHIPMENT", "READ");
        String ipAddress = extractClientIp(request);
        Gs1DossierExportResponse response = dossierService.exportGs1Dossier(
                shipmentId, format, includeMapping, currentUser, ipAddress);

        String normalizedFormat = format == null ? "json" : format.toLowerCase();

        if ("xml".equals(normalizedFormat)) {
            try {
                XmlMapper xmlMapper = new XmlMapper();
                xmlMapper.registerModule(new JavaTimeModule());
                xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                String xml = xmlMapper.writeValueAsString(response);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(xml);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Lỗi khi sinh XML hồ sơ GS1.", ex);
            }
        }

        return ResponseEntity.ok(ApiResult.success(response));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * API Kiểm tra điều kiện xuất hồ sơ hàng loạt.
     */
    @PostMapping("/dossiers/batch-check")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<BatchDossierCheckResponse>> checkBatchEligibility(
            @Valid @RequestBody BatchDossierCheckRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        permissionChecker.check("SHIPMENT", "READ");
        BatchDossierCheckResponse response = dossierService.checkBatchEligibility(request, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * API Xuất và tải về duy nhất một tệp PDF bộ hồ sơ truy xuất hợp nhất.
     */
    @PostMapping("/dossiers/batch-export")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<byte[]> exportBatchDossierPdf(
            @Valid @RequestBody BatchDossierExportRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest servletRequest) {
        permissionChecker.check("SHIPMENT", "READ");
        String ipAddress = extractClientIp(servletRequest);
        byte[] pdfBytes = dossierService.exportBatchDossierPdf(request, currentUser, ipAddress);

        String rawFileName = "Bo_ho_so_truy_xuat_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(rawFileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * API Lấy lịch sử xuất bộ hồ sơ hàng loạt.
     */
    @GetMapping("/dossiers/batch-history")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-04')")
    public ResponseEntity<ApiResult<List<BatchDossierHistoryDto>>> getBatchExportHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        permissionChecker.check("SHIPMENT", "READ");
        List<BatchDossierHistoryDto> response = dossierService.getBatchExportHistory(currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Xử lý ngoại lệ DossierValidationException và trả về phản hồi lỗi.
     */
    @ExceptionHandler(DossierValidationException.class)
    public ResponseEntity<ApiResult<Void>> handleDossierValidation(
            DossierValidationException e,
            HttpServletRequest request) {
        ApiResult<Void> body = ApiResult.error(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                e.getErrors(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
