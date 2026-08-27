package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CategoryCriteriaRequest;
import vn.nguongocso.certification.dto.request.MandatoryInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionCatalogResponse;
import vn.nguongocso.certification.service.CategoryCriterionAssignmentService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý bộ chỉ tiêu của loại nông sản và cờ bắt buộc kiểm nghiệm.
 * Story: NCL-09-CN-009
 */
@RestController
@RequestMapping("/api/v1/product-categories/{id}")
@RequiredArgsConstructor
public class ProductCategoryCriterionController {

    private final CategoryCriterionAssignmentService categoryCriterionAssignmentService;

    /**
     * Lấy bộ chỉ tiêu của loại nông sản. §4.7
     */
    @GetMapping("/criteria")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<List<InspectionCriterionCatalogResponse>> getCriteria(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<InspectionCriterionCatalogResponse> response =
                categoryCriterionAssignmentService.getCategoryCriteria(id, activeOnly, currentUser);
        return ApiResult.success(response);
    }

    /**
     * Gán (replace) bộ chỉ tiêu cho loại nông sản. §4.8 — chỉ PLATFORM_ADMIN.
     */
    @PutMapping("/criteria")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<List<InspectionCriterionCatalogResponse>>> assignCriteria(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryCriteriaRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<InspectionCriterionCatalogResponse> response =
                categoryCriterionAssignmentService.assignCriteria(id, request, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Bật/tắt cờ bắt buộc kiểm nghiệm. §4.9 — chỉ PLATFORM_ADMIN.
     */
    @PutMapping("/mandatory-inspection")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<ProductCategoryResponse>> setMandatoryInspection(
            @PathVariable UUID id,
            @Valid @RequestBody MandatoryInspectionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ProductCategoryResponse response =
                categoryCriterionAssignmentService.setMandatoryInspection(id, request, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
