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
 * Repository for the inspection criterion catalog.
 * Story: NCL-09-CN-009
 */
public interface InspectionCriterionCatalogRepository extends JpaRepository<InspectionCriterionCatalog, Long> {

    /**
     * Check if a criterion with the same name and reference standard already exists.
     * For BR-1: duplicate check by (name + referenceStandard).
     */
    @Query("SELECT COUNT(c) > 0 FROM InspectionCriterionCatalog c " +
           "WHERE LOWER(c.name) = LOWER(:name) " +
           "AND COALESCE(c.referenceStandard, '') = COALESCE(:referenceStandard, '')")
    boolean existsByNameAndReferenceStandard(
            @Param("name") String name,
            @Param("referenceStandard") String referenceStandard);

    /**
     * Check if a criterion with the same name and reference standard exists, excluding a given id.
     */
    @Query("SELECT COUNT(c) > 0 FROM InspectionCriterionCatalog c " +
           "WHERE LOWER(c.name) = LOWER(:name) " +
           "AND COALESCE(c.referenceStandard, '') = COALESCE(:referenceStandard, '') " +
           "AND c.id <> :excludeId")
    boolean existsByNameAndReferenceStandardAndIdNot(
            @Param("name") String name,
            @Param("referenceStandard") String referenceStandard,
            @Param("excludeId") Long excludeId);

    /**
     * Find criteria with optional keyword and status filter, paginated.
     */
    @Query("SELECT c FROM InspectionCriterionCatalog c " +
           "WHERE (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(c.referenceStandard, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.name ASC")
    Page<InspectionCriterionCatalog> search(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);

    /**
     * Find all active criteria.
     */
    List<InspectionCriterionCatalog> findByStatusOrderByNameAsc(String status);

    /**
     * Find all criteria by given ids.
     */
    List<InspectionCriterionCatalog> findByIdIn(List<Long> ids);

    /**
     * Find by name (for standard inner uniqueness).
     */
    Optional<InspectionCriterionCatalog> findFirstByNameIgnoreCase(String name);
}
