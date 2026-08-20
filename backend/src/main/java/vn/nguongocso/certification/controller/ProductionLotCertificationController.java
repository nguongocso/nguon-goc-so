package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.AttachCertificationRequest;
import vn.nguongocso.certification.dto.response.ProductionLotCertificationResponse;
import vn.nguongocso.certification.service.CertificationService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.List;
import java.util.UUID;

/*
 * Controller quản lý chứng nhận cho lô sản xuất.
 */
@RestController
@RequestMapping("/api/v1/production-lots/{lotId}/certifications")
@RequiredArgsConstructor
public class ProductionLotCertificationController {
    private final CertificationService certificationService;
    private final PermissionChecker permissionChecker;

    /**
     * Lấy danh sách chứng nhận của một lô sản xuất.
     * Cho phép VT-01, VT-02 và VT-03 xem (VT-03 cần để ghi sự kiện).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')") // ✅ Thêm VT-03
    public ResponseEntity<ApiResult<List<ProductionLotCertificationResponse>>> getCertificationsOfLot(
            @PathVariable UUID lotId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<ProductionLotCertificationResponse> list = certificationService.getCertificationsOfLot(lotId, currentUser);
        return ResponseEntity.ok(ApiResult.success(list));
    }

    /**
     * Gắn chứng nhận cho lô sản xuất.
     * Chỉ VT-02 mới có quyền gắn chứng nhận.
     */
    @PostMapping
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ProductionLotCertificationResponse>> attachCertification(
            @PathVariable UUID lotId,
            @Valid @RequestBody AttachCertificationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ProductionLotCertificationResponse response = certificationService.attachCertification(lotId, request,
                currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Gỡ bỏ chứng nhận khỏi lô sản xuất.
     * Chỉ VT-02 mới có quyền gỡ chứng nhận.
     */
    @DeleteMapping("/{certificationId}")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<Void>> detachCertification(
            @PathVariable UUID lotId,
            @PathVariable UUID certificationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        certificationService.detachCertification(lotId, certificationId, currentUser);
        return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), null));
    }
}