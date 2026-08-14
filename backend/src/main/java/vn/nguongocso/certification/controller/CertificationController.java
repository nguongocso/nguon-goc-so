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

import java.util.List;

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
     * Lấy danh sách tất cả chứng nhận của tổ chức (VT-02)
     * GET /api/v1/certifications
     */
    @GetMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<List<CertificationResponse>>> getAllCertifications(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<CertificationResponse> list = certificationService.getAllCertifications(currentUser);
        return ResponseEntity.ok(ApiResult.success(list));
    }
}