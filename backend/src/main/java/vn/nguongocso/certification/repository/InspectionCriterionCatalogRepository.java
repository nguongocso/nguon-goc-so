package vn.nguongocso.certification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho danh mục chỉ tiêu kiểm nghiệm (NCL-09-CN-009).
 */
public interface InspectionCriterionCatalogRepository
                extends JpaRepository<InspectionCriterionCatalog, Long> {
        /**
         * Kiểm tra trùng tên và tiêu chuẩn tham chiếu (quy tắc BR-1).
         */
        @Query("""
                        SELECT COUNT(c) > 0 FROM InspectionCriterionCatalog c
                        WHERE LOWER(c.name) = LOWER(:name)
                        AND COALESCE(c.referenceStandard, '') = COALESCE(:referenceStandard, '')
                        """)
        boolean existsByNameAndReferenceStandard(
                        @Param("name") String name,
                        @Param("referenceStandard") String referenceStandard);

        /**
         * Kiểm tra trùng tên và tiêu chuẩn tham chiếu khi cập nhật, loại trừ ID hiện
         * tại.
         */
        @Query("""
                        SELECT COUNT(c) > 0 FROM InspectionCriterionCatalog c
                        WHERE LOWER(c.name) = LOWER(:name)
                        AND COALESCE(c.referenceStandard, '') = COALESCE(:referenceStandard, '')
                        AND c.id <> :excludeId
                        """)
        boolean existsByNameAndReferenceStandardAndIdNot(
                        @Param("name") String name,
                        @Param("referenceStandard") String referenceStandard,
                        @Param("excludeId") Long excludeId);

        /**
         * Tìm kiếm chỉ tiêu kiểm nghiệm theo từ khoá và trạng thái, có phân trang.
         */
        @Query("""
                        SELECT c FROM InspectionCriterionCatalog c
                        WHERE (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(c.referenceStandard, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        AND (:status IS NULL OR c.status = :status)
                        ORDER BY c.name ASC
                        """)
        Page<InspectionCriterionCatalog> search(
                        @Param("keyword") String keyword,
                        @Param("status") String status,
                        Pageable pageable);

        /**
         * Lấy tất cả chỉ tiêu kiểm nghiệm theo trạng thái, sắp xếp theo tên.
         */
        List<InspectionCriterionCatalog> findByStatusOrderByNameAsc(String status);

        /**
         * Lấy danh sách chỉ tiêu kiểm nghiệm theo tập ID cho trước.
         */
        List<InspectionCriterionCatalog> findByIdIn(List<Long> ids);

        /**
         * Tìm chỉ tiêu kiểm nghiệm đầu tiên theo tên (không phân biệt hoa thường).
         */
        Optional<InspectionCriterionCatalog> findFirstByNameIgnoreCase(String name);
}
