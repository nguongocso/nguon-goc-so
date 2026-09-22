package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.CategoryCriterion;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository quản lý quan hệ giữa loại sản phẩm và chỉ tiêu kiểm nghiệm (NCL-09-CN-009).
 */
public interface CategoryCriterionRepository
                extends JpaRepository<CategoryCriterion, UUID> {
        /**
         * Lấy tất cả chỉ tiêu được gán cho một loại sản phẩm (kèm chỉ tiêu danh mục), sắp xếp theo tên chỉ tiêu.
         */
        @Query("""
                        SELECT cc FROM CategoryCriterion cc
                        JOIN FETCH cc.criterion
                        WHERE cc.category.id = :categoryId
                        ORDER BY cc.criterion.name ASC
                        """)
        List<CategoryCriterion> findByCategoryIdWithCriteria(
                        @Param("categoryId") UUID categoryId);

        /**
         * Lấy tất cả chỉ tiêu được gán cho một loại sản phẩm theo trạng thái chỉ tiêu, sắp xếp theo tên chỉ tiêu.
         */
        @Query("""
                        SELECT cc FROM CategoryCriterion cc
                        JOIN FETCH cc.criterion
                        WHERE cc.category.id = :categoryId
                        AND cc.criterion.status = :status
                        ORDER BY cc.criterion.name ASC
                        """)
        List<CategoryCriterion> findByCategoryIdAndCriteriaStatus(
                        @Param("categoryId") UUID categoryId,
                        @Param("status") String status);

        /**
         * Đếm số lượng chỉ tiêu được gán cho một loại sản phẩm theo trạng thái.
         */
        @Query("""
                        SELECT COUNT(cc) FROM CategoryCriterion cc
                        WHERE cc.category.id = :categoryId
                        AND cc.criterion.status = :status
                        """)
        long countByCategoryIdAndCriteriaStatus(
                        @Param("categoryId") UUID categoryId,
                        @Param("status") String status);

        /**
         * Xóa các gán chỉ tiêu theo loại sản phẩm và danh sách chỉ tiêu cụ thể.
         * Dùng khi người dùng bỏ chọn một số chỉ tiêu để tránh xóa toàn bộ gây trùng lặp.
         */
        @Modifying
        @Query("""
                        DELETE FROM CategoryCriterion cc
                        WHERE cc.category.id = :categoryId
                        AND cc.criterion.id IN :criterionIds
                        """)
        int deleteByCategory_IdAndCriterion_IdIn(
                        @Param("categoryId") UUID categoryId,
                        @Param("criterionIds") Collection<Long> criterionIds);

        /**
         * Kiểm tra xem chỉ tiêu cụ thể đã được gán cho bất kỳ loại sản phẩm nào chưa.
         */
        boolean existsByCriterion_Id(Long criterionId);

        /**
         * Kiểm tra xem chỉ tiêu cụ thể đã được gán cho loại sản phẩm cho trước hay chưa.
         * Dùng để xác thực chỉ tiêu khi tạo yêu cầu kiểm nghiệm (NCL-11-CN-002).
         */
        boolean existsByCategory_IdAndCriterion_Id(UUID categoryId, Long criterionId);

        /**
         * Kiểm tra xem chỉ tiêu cụ thể có thuộc loại sản phẩm bắt buộc kiểm nghiệm hay không (quy tắc BR-3: bắt buộc =>
         * có ít nhất 1 chỉ tiêu).
         */
        boolean existsByCriterion_IdAndCategory_RequiresInspectionTrue(Long criterionId);
}
