package vn.nguongocso.certification.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CategoryCriteriaRequest;
import vn.nguongocso.certification.dto.request.MandatoryInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionCatalogResponse;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý gán bộ chỉ tiêu cho loại nông sản và cờ bắt buộc kiểm nghiệm.
 * Story: NCL-09-CN-009
 */
public interface CategoryCriterionAssignmentService {

    /**
     * Lấy bộ chỉ tiêu đã gán cho loại nông sản. §4.7
     */
    List<InspectionCriterionCatalogResponse> getCategoryCriteria(
            UUID categoryId, boolean activeOnly, CustomUserDetails currentUser);

    /**
     * Gán (replace) bộ chỉ tiêu cho loại nông sản. §4.8 — chỉ PLATFORM_ADMIN.
     */
    List<InspectionCriterionCatalogResponse> assignCriteria(
            UUID categoryId, CategoryCriteriaRequest request, CustomUserDetails currentUser);

    /**
     * Bật/tắt cờ bắt buộc kiểm nghiệm cho loại nông sản. §4.9 — chỉ PLATFORM_ADMIN.
     */
    ProductCategoryResponse setMandatoryInspection(
            UUID categoryId, MandatoryInspectionRequest request, CustomUserDetails currentUser);
}
