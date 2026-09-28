package vn.nguongocso.certification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionCatalogRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionCatalogResponse;

/**
 * Service quản lý danh mục chỉ tiêu kiểm nghiệm.
 * Story: NCL-09-CN-009
 */
public interface InspectionCriterionCatalogService {
        /**
         * Tìm kiếm chỉ tiêu kiểm nghiệm theo từ khóa và trạng thái có phân trang.
         */
        Page<InspectionCriterionCatalogResponse> searchCriteria(
                        String keyword, String status, Pageable pageable, CustomUserDetails currentUser);

        /**
         * Lấy chi tiết chỉ tiêu kiểm nghiệm theo ID.
         */
        InspectionCriterionCatalogResponse getCriterion(
                        Long id, CustomUserDetails currentUser);

        /**
         * Tạo mới chỉ tiêu kiểm nghiệm vào danh mục.
         */
        InspectionCriterionCatalogResponse createCriterion(
                        InspectionCriterionCatalogRequest request, CustomUserDetails currentUser);

        /**
         * Cập nhật thông tin chỉ tiêu kiểm nghiệm trong danh mục.
         */
        InspectionCriterionCatalogResponse updateCriterion(
                        Long id, InspectionCriterionCatalogRequest request, CustomUserDetails currentUser);

        /**
         * Vô hiệu hóa chỉ tiêu kiểm nghiệm trong danh mục.
         */
        void disableCriterion(
                        Long id, CustomUserDetails currentUser);

        /**
         * Kích hoạt lại chỉ tiêu kiểm nghiệm trong danh mục.
         */
        InspectionCriterionCatalogResponse enableCriterion(
                        Long id, CustomUserDetails currentUser);

        /**
         * Xóa chỉ tiêu kiểm nghiệm khỏi danh mục.
         */
        void deleteCriterion(
                        Long id, CustomUserDetails currentUser);
}
