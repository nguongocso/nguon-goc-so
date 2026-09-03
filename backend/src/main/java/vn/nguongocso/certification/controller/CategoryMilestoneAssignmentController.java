package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CategoryMilestoneRequest;
import vn.nguongocso.certification.dto.response.ProductCategoryMilestoneResponse;
import vn.nguongocso.certification.service.CategoryMilestoneAssignmentService;
import vn.nguongocso.common.ApiResult;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing milestone assignments to product categories.
 * Story: NCL-09-CN-011
 */
@RestController
@RequestMapping("/api/v1/product-categories/{categoryId}/milestones")
@RequiredArgsConstructor
public class CategoryMilestoneAssignmentController {

    private final CategoryMilestoneAssignmentService assignmentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResult<List<ProductCategoryMilestoneResponse>> getMilestones(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(assignmentService.getCategoryMilestones(categoryId, currentUser));
    }

    @PutMapping
    @PreAuthorize("hasRole('VT-01')")
    public ApiResult<List<ProductCategoryMilestoneResponse>> assignMilestones(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryMilestoneRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResult.success(assignmentService.assignMilestones(categoryId, request, currentUser));
    }
}
