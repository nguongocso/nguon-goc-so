package vn.nguongocso.certification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.CultivationMilestoneCatalog;
import java.util.List;
import java.util.Optional;

/**
 * Repository for the cultivation milestone catalog.
 * Story: NCL-09-CN-011
 */
public interface CultivationMilestoneCatalogRepository extends JpaRepository<CultivationMilestoneCatalog, Long> {

    /**
     * Check if a milestone with the same name and activity type already exists.
     */
    @Query("SELECT COUNT(m) > 0 FROM CultivationMilestoneCatalog m " +
           "WHERE LOWER(m.name) = LOWER(:name) " +
           "AND m.activityType = :activityType")
    boolean existsByNameAndActivityType(
            @Param("name") String name,
            @Param("activityType") String activityType);

    /**
     * Check if a milestone with the same name and activity type exists, excluding a given id.
     */
    @Query("SELECT COUNT(m) > 0 FROM CultivationMilestoneCatalog m " +
           "WHERE LOWER(m.name) = LOWER(:name) " +
           "AND m.activityType = :activityType " +
           "AND m.id <> :excludeId")
    boolean existsByNameAndActivityTypeAndIdNot(
            @Param("name") String name,
            @Param("activityType") String activityType,
            @Param("excludeId") Long excludeId);

    /**
     * Find milestones with optional keyword and status filter, paginated.
     */
    @Query("SELECT m FROM CultivationMilestoneCatalog m " +
           "WHERE (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:activityType IS NULL OR m.activityType = :activityType) " +
           "ORDER BY m.name ASC")
    Page<CultivationMilestoneCatalog> search(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("activityType") String activityType,
            Pageable pageable);

    /**
     * Find all active milestones.
     */
    List<CultivationMilestoneCatalog> findByStatusOrderByNameAsc(String status);

    /**
     * Find all milestones by given ids.
     */
    List<CultivationMilestoneCatalog> findByIdIn(List<Long> ids);

    /**
     * Find milestones by activity type.
     */
    List<CultivationMilestoneCatalog> findByActivityTypeAndStatusOrderByNameAsc(
            String activityType, String status);
}
