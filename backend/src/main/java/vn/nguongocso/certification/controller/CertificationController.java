package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateCertificationRequest;
import vn.nguongocso.certification.dto.response.CertificationResponse;
import vn.nguongocso.certification.service.CertificationService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

/*
* Controller quản lý chứng nhận cho tổ chức.
*/
@RestController
@RequestMapping("/api/v1/certifications")
@RequiredArgsConstructor
public class CertificationController {
    private final CertificationService certificationService;

    /**
     * Tạo mới chứng nhận cho tổ chức (VT-02).
     * POST /api/v1/certifications
     */
    @PostMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<CertificationResponse>> createCertification(
            @Valid @RequestBody CreateCertificationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CertificationResponse response = certificationService.createCertification(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Tìm kiếm chứng nhận của tổ chức, có phân trang (VT-02).
     *
     * <p>Từ khoá khớp tên / số hiệu / cơ quan cấp. Trạng thái:
     * valid | expiring | expired. Sắp xếp theo sortBy (name | issueDate |
     * expiryDate) và sortDir (asc | desc).</p>
     *
     * GET /api/v1/certifications?keyword=&status=&sortBy=&sortDir=&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<PageResponse<CertificationResponse>>> searchCertifications(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        PageResponse<CertificationResponse> response = certificationService.searchCertifications(
                keyword, status, sortBy, sortDir, page, size, currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
    }
}