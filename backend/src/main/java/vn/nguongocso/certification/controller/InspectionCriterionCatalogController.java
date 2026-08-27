package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionCatalogRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionCatalogResponse;
import vn.nguongocso.certification.service.InspectionCriterionCatalogService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

/**
 * Controller quản lý danh mục chỉ tiêu kiểm nghiệm.
 * Story: NCL-09-CN-009
 */
@RestController
@RequestMapping("/api/v1/inspection-criteria")
@RequiredArgsConstructor
public class InspectionCriterionCatalogController {

    private final InspectionCriterionCatalogService inspectionCriterionCatalogService;

    /**
     * Danh sách chỉ tiêu kiểm nghiệm (phân trang, lọc theo keyword/status).
     * §4.1
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResult<PageResponse<InspectionCriterionCatalogResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        Page<InspectionCriterionCatalogResponse> result =
                inspectionCriterionCatalogService.searchCriteria(keyword, status, pageable, currentUser);
        return ApiResult.success(PageResponse.from(result, result.getContent()));
    }

    /**
     * Chi tiết chỉ tiêu kiểm nghiệm. §4.3
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<InspectionCriterionCatalogResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(inspectionCriterionCatalogService.getCriterion(id, currentUser));
    }

    /**
     * Tạo chỉ tiêu kiểm nghiệm. §4.2 — chỉ PLATFORM_ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<InspectionCriterionCatalogResponse>> create(
            @Valid @RequestBody InspectionCriterionCatalogRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        InspectionCriterionCatalogResponse response =
                inspectionCriterionCatalogService.createCriterion(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Cập nhật chỉ tiêu kiểm nghiệm. §4.4 — chỉ PLATFORM_ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<InspectionCriterionCatalogResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InspectionCriterionCatalogRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(inspectionCriterionCatalogService.updateCriterion(id, request, currentUser));
    }

    /**
     * Ngừng sử dụng chỉ tiêu kiểm nghiệm. §4.5 — chỉ PLATFORM_ADMIN.
     */
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<Void> disable(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        inspectionCriterionCatalogService.disableCriterion(id, currentUser);
        return ApiResult.success(null);
    }

    /**
     * Kích hoạt lại chỉ tiêu kiểm nghiệm. §4.5 — chỉ PLATFORM_ADMIN.
     */
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<InspectionCriterionCatalogResponse> enable(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(inspectionCriterionCatalogService.enableCriterion(id, currentUser));
    }

    /**
     * Xóa chỉ tiêu kiểm nghiệm (chưa tham chiếu). §4.6 — chỉ PLATFORM_ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        inspectionCriterionCatalogService.deleteCriterion(id, currentUser);
        return ApiResult.success(null);
    }
}
