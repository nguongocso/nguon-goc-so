package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.AccreditationScope;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository cho phạm vi công nhận của đơn vị kiểm nghiệm
 * (NCL-11-CN-006 Phase 2).
 */
public interface AccreditationScopeRepository
        extends JpaRepository<AccreditationScope, UUID> {

    /**
     * Lấy toàn bộ phạm vi công nhận của một đơn vị kiểm nghiệm
     * (kèm chỉ tiêu danh mục, tránh N+1), sắp theo tên chỉ tiêu.
     */
    @Query("""
            SELECT s FROM AccreditationScope s
            JOIN FETCH s.criterion
            WHERE s.testingUnit.id = :testingUnitId
            ORDER BY s.criterion.name ASC
            """)
    List<AccreditationScope> findByTestingUnitIdWithCriterion(
            @Param("testingUnitId") UUID testingUnitId);

    /**
     * Lấy các phạm vi công nhận khớp một tập chỉ tiêu cho trước.
     * Dùng để kiểm tra chỉ tiêu nào thuộc phạm vi khi tạo yêu cầu.
     */
    @Query("""
            SELECT s FROM AccreditationScope s
            JOIN FETCH s.criterion
            WHERE s.testingUnit.id = :testingUnitId
            AND s.criterion.id IN :criterionIds
            """)
    List<AccreditationScope> findByTestingUnitIdAndCriterionIdIn(
            @Param("testingUnitId") UUID testingUnitId,
            @Param("criterionIds") Collection<Long> criterionIds);

    /**
     * Xoá toàn bộ phạm vi công nhận của một đơn vị kiểm nghiệm.
     * Dùng khi cập nhật theo ngữ nghĩa REPLACE-ALL.
     */
    @Modifying
    @Query("DELETE FROM AccreditationScope s WHERE s.testingUnit.id = :testingUnitId")
    void deleteByTestingUnitId(@Param("testingUnitId") UUID testingUnitId);
}
