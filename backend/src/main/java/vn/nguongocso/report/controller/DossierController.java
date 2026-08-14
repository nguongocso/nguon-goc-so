package vn.nguongocso.report.controller;

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