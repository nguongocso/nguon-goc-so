package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.CategoryCriterion;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing the category-criteria relationship.
 * Story: NCL-09-CN-009
 */
public interface CategoryCriterionRepository extends JpaRepository<CategoryCriterion, Long> {

    /**
     * Find all criteria assigned to a product category.
     */
    @Query("SELECT cc FROM CategoryCriterion cc " +
           "JOIN FETCH cc.criterion " +
           "WHERE cc.category.id = :categoryId " +
           "ORDER BY cc.criterion.name ASC")
    List<CategoryCriterion> findByCategoryIdWithCriteria(@Param("categoryId") UUID categoryId);

    /**
     * Find all criteria assigned to a product category, filtered by status.
     */
    @Query("SELECT cc FROM CategoryCriterion cc " +
           "JOIN FETCH cc.criterion " +
           "WHERE cc.category.id = :categoryId " +
           "AND cc.criterion.status = :status " +
           "ORDER BY cc.criterion.name ASC")
    List<CategoryCriterion> findByCategoryIdAndCriteriaStatus(
            @Param("categoryId") UUID categoryId,
            @Param("status") String status);

    /**
     * Count criteria assigned to a category with a specific status.
     */
    @Query("SELECT COUNT(cc) FROM CategoryCriterion cc " +
           "WHERE cc.category.id = :categoryId " +
           "AND cc.criterion.status = :status")
    long countByCategoryIdAndCriteriaStatus(
            @Param("categoryId") UUID categoryId,
            @Param("status") String status);

    /**
     * Delete all criteria assignments for a category.
     */
    void deleteByCategory_Id(UUID categoryId);

    /**
     * Check if a specific criterion is assigned to any product category.
     */
    boolean existsByCriterion_Id(Long criterionId);

    /**
     * Check if a specific criterion is assigned to a product category
     * that requires mandatory inspection (BR-3 invariant: mandatory ⇒ ≥1 criterion).
     */
    boolean existsByCriterion_IdAndCategory_RequiresInspectionTrue(Long criterionId);
}
