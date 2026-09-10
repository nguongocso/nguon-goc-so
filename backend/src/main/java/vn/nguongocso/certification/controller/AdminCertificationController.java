package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.RejectCertificateRequest;
import vn.nguongocso.certification.dto.request.VerifyCertificateRequest;
import vn.nguongocso.certification.dto.response.CertificationVerificationResponse;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;
import vn.nguongocso.certification.service.CertificationService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Controller quản trị xác thực và từ chối chứng nhận của tổ chức (NCL-696 / NCL-09-CN-012).
 * Chỉ dành riêng cho vai trò Quản trị viên nền tảng (VT-01).
 */
@RestController
@RequestMapping("/api/v1/admin/certifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VT-01')")
public class AdminCertificationController {

    private final CertificationService certificationService;

    /**
     * Lấy danh sách chứng nhận để Quản trị viên kiểm tra, đối chiếu.
     */
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<CertificationVerificationResponse>>> getCertifications(
            @RequestParam(required = false) CertificationVerificationStatus verificationStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        PageResponse<CertificationVerificationResponse> response = certificationService.getAdminCertifications(
                verificationStatus,
                keyword,
                organizationId,
                sortBy,
                sortDir,
                page,
                size,
                currentUser);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy chi tiết thông tin đối chiếu của một chứng nhận.
     */
    @GetMapping("/{certificationId}")
    public ResponseEntity<ApiResult<CertificationVerificationResponse>> getCertificationDetail(
            @PathVariable UUID certificationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CertificationVerificationResponse response = certificationService.getAdminCertificationDetail(
                certificationId,
                currentUser);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Xem tệp tài liệu chứng nhận an toàn (không mở trực tiếp storage path).
     */
    @GetMapping("/{certificationId}/document")
    public ResponseEntity<Resource> getCertificateDocument(
            @PathVariable UUID certificationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CertificationService.DocumentResource documentResource = certificationService.getCertificateDocumentResource(
                certificationId,
                currentUser);

        String encodedFileName = URLEncoder.encode(documentResource.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(documentResource.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .header("X-Content-Type-Options", "nosniff")
                .body(documentResource.resource());
    }

    /**
     * Xác thực chứng nhận của tổ chức (chuyển trạng thái PENDING -> VERIFIED).
     */
    @PutMapping("/{certificationId}/verify")
    public ResponseEntity<ApiResult<CertificationVerificationResponse>> verifyCertificate(
            @PathVariable UUID certificationId,
            @Valid @RequestBody(required = false) VerifyCertificateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CertificationVerificationResponse response = certificationService.verifyCertificate(
                certificationId,
                request,
                currentUser);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Từ chối xác thực chứng nhận của tổ chức (chuyển trạng thái PENDING -> REJECTED, gửi thông báo kèm lý do).
     */
    @PutMapping("/{certificationId}/reject")
    public ResponseEntity<ApiResult<CertificationVerificationResponse>> rejectCertificate(
            @PathVariable UUID certificationId,
            @Valid @RequestBody RejectCertificateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CertificationVerificationResponse response = certificationService.rejectCertificate(
                certificationId,
                request,
                currentUser);

        return ResponseEntity.ok(ApiResult.success(response));
    }
}
