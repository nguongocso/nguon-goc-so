package vn.nguongocso.certification.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CategoryMilestoneRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneCatalogResponse;
import vn.nguongocso.certification.dto.response.ProductCategoryMilestoneResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing milestone assignments to product categories.
 * Story: NCL-09-CN-011
 */
public interface CategoryMilestoneAssignmentService {

    /**
     * Get milestones assigned to a product category.
     */
    List<ProductCategoryMilestoneResponse> getCategoryMilestones(
            UUID categoryId, CustomUserDetails currentUser);

    /**
     * Assign (replace) milestones to a product category. Only PLATFORM_ADMIN.
     */
    List<ProductCategoryMilestoneResponse> assignMilestones(
            UUID categoryId, CategoryMilestoneRequest request, CustomUserDetails currentUser);
}
