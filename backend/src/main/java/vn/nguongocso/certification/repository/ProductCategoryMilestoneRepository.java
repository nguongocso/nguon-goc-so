package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.ProductCategoryMilestone;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing the product-category-milestone relationship.
 * Story: NCL-09-CN-011
 */
public interface ProductCategoryMilestoneRepository extends JpaRepository<ProductCategoryMilestone, UUID> {

    /**
     * Find all milestones assigned to a product category.
     */
    @Query("SELECT pcm FROM ProductCategoryMilestone pcm " +
           "JOIN FETCH pcm.milestone " +
           "LEFT JOIN FETCH pcm.standard " +
           "WHERE pcm.category.id = :categoryId " +
           "ORDER BY pcm.milestone.name ASC")
    List<ProductCategoryMilestone> findByCategoryIdWithMilestones(@Param("categoryId") UUID categoryId);

    /**
     * Find all milestones assigned to a product category, filtered by mandatory flag.
     */
    @Query("SELECT pcm FROM ProductCategoryMilestone pcm " +
           "JOIN FETCH pcm.milestone " +
           "LEFT JOIN FETCH pcm.standard " +
           "WHERE pcm.category.id = :categoryId " +
           "AND pcm.isMandatory = :mandatory " +
           "ORDER BY pcm.milestone.name ASC")
    List<ProductCategoryMilestone> findByCategoryIdAndMandatory(
            @Param("categoryId") UUID categoryId,
            @Param("mandatory") Boolean mandatory);

    /**
     * Count milestones assigned to a category with a specific mandatory flag.
     */
    @Query("SELECT COUNT(pcm) FROM ProductCategoryMilestone pcm " +
           "WHERE pcm.category.id = :categoryId " +
           "AND pcm.isMandatory = :mandatory")
    long countByCategoryIdAndMandatory(
            @Param("categoryId") UUID categoryId,
            @Param("mandatory") Boolean mandatory);

    /**
     * Delete milestones by category + specific milestone ids.
     */
    @Modifying
    @Query("DELETE FROM ProductCategoryMilestone pcm WHERE pcm.category.id = :categoryId AND pcm.milestone.id IN :milestoneIds")
    int deleteByCategory_IdAndMilestone_IdIn(
            @Param("categoryId") UUID categoryId,
            @Param("milestoneIds") Collection<Long> milestoneIds);

    /**
     * Check if a specific milestone is assigned to any product category.
     */
    boolean existsByMilestone_Id(Long milestoneId);

    /**
     * Return all milestone ids that are referenced by at least one
     * product-category-milestone mapping. Used to compute the "referenced" flag.
     */
    @Query("SELECT DISTINCT pcm.milestone.id FROM ProductCategoryMilestone pcm")
    List<Long> findReferencedMilestoneIds();

    /**
     * Find all mandatory milestones for a product category, optionally filtered by standard ids.
     * Used for validation at packaging time.
     */
    @Query("SELECT pcm FROM ProductCategoryMilestone pcm " +
           "JOIN FETCH pcm.milestone " +
           "WHERE pcm.category.id = :categoryId " +
           "AND pcm.isMandatory = true " +
           "AND pcm.milestone.status = 'ACTIVE' " +
           "AND (pcm.standard IS NULL OR pcm.standard.id IN :standardIds)")
    List<ProductCategoryMilestone> findMandatoryMilestonesForValidation(
            @Param("categoryId") UUID categoryId,
            @Param("standardIds") List<UUID> standardIds);
}
