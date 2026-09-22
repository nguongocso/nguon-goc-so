package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.InspectionCriterion;

import java.util.List;
import java.util.UUID;

/**
 * Repository cho thực thể chỉ tiêu kiểm nghiệm (InspectionCriterion).
 */
public interface InspectionCriterionRepository
                extends JpaRepository<InspectionCriterion, UUID> {
        /**
         * Kiểm tra xem chỉ tiêu danh mục có đang được tham chiếu bởi chỉ tiêu kiểm nghiệm nào không (BR-5).
         */
        @Query("""
                        SELECT COUNT(ic) > 0 FROM InspectionCriterion ic
                        WHERE ic.criterionId = :criterionId
                        """)
        boolean existsByCriterionId(@Param("criterionId") Long criterionId);

        /**
         * Lấy danh sách ID chỉ tiêu danh mục đang được tham chiếu bởi ít nhất một chỉ tiêu kiểm nghiệm.
         * Dùng để tính cờ tham chiếu tránh lỗi N+1 truy vấn khi ánh xạ danh sách.
         */
        @Query("""
                        SELECT DISTINCT ic.criterionId FROM InspectionCriterion ic
                        WHERE ic.criterionId IS NOT NULL
                        """)
        List<Long> findReferencedCriterionIds();
}
