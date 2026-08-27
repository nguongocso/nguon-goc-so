package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.InspectionCriterion;

import java.util.List;
import java.util.UUID;

/**
 * Repository cho thực thể InspectionCriterion.
 */
public interface InspectionCriterionRepository
        extends JpaRepository<InspectionCriterion, UUID> {

    /**
     * Check if any inspection criterion references a given catalog criterion.
     * Used for the "referenced" flag in BR-5.
     */
    @Query("SELECT COUNT(ic) > 0 FROM InspectionCriterion ic " +
           "WHERE ic.criterionId = :criterionId")
    boolean existsByCriterionId(@Param("criterionId") Long criterionId);

    /**
     * Return all catalog criterion ids that are referenced by at least one
     * inspection criterion. Used to compute the "referenced" flag without
     * N+1 queries when mapping lists.
     */
    @Query("SELECT DISTINCT ic.criterionId FROM InspectionCriterion ic " +
           "WHERE ic.criterionId IS NOT NULL")
    List<Long> findReferencedCriterionIds();
}
