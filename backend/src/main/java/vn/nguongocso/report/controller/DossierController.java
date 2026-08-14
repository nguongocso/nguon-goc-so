package vn.nguongocso.report.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.report.exception.DossierValidationException;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;
import vn.nguongocso.report.service.DossierService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Controller quản lý hồ sơ truy xuất.
 *
 * @author Triệu Văn Đại
 */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class DossierController {

    private final DossierService dossierService;
    private final PermissionChecker permissionChecker;

    /**
     * API Kiểm tra điều kiện xuất hồ sơ truy xuất.
     * Cho phép các bên kiểm tra trước xem hồ sơ của lô hàng đã đủ điều kiện hay
     * chưa.
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
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {

        permissionChecker.check("SHIPMENT", "READ");
        String ipAddress = extractClientIp(request);
        byte[] pdfBytes = dossierService.exportDossierPdf(shipmentId, currentUser, ipAddress);

        String rawFileName = "Ho_so_truy_xuat_" + shipmentId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(rawFileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * API Xuất hồ sơ truy xuất theo lược đồ GS1 mô phỏng.
     *
     * <p>
     * Chỉ dành cho VT-02 (Quản lý HTX) và VT-04 (Doanh nghiệp thu mua). Hồ sơ
     * được ánh xạ theo bốn chiều {@code who / when / where / why}. Hỗ trợ xuất
     * dưới dạng {@code json} hoặc {@code xml}, kèm bảng ánh xạ schema (mặc định
     * {@code includeMapping=true}).
     * </p>
     *
     * @param shipmentId     ID lô hàng cần xuất hồ sơ
     * @param format         định dạng xuất: {@code json} hoặc {@code xml}
     * @param includeMapping có kèm bảng ánh xạ schema hay không
     * @param currentUser    thông tin người dùng hiện tại
     * @param request        HTTP request dùng để lấy IP client
     * @return hồ sơ GS1 mô phỏng (JSON hoặc XML)
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

        // JSON (default) – bọc trong ApiResult theo convention project
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
