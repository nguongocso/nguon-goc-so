package vn.nguongocso.trace.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.trace.entity.ShipmentHandover;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;

/**
 * Repository cho thực thể ShipmentHandover.
 */
public interface ShipmentHandoverRepository extends JpaRepository<ShipmentHandover, UUID> {

    List<ShipmentHandover> findByShipmentId(UUID shipmentId);

    List<ShipmentHandover> findByToOrganizationOrganizationId(UUID orgId);

    List<ShipmentHandover> findByFromOrganizationOrganizationId(UUID orgId);

    boolean existsByShipmentIdAndStatus(UUID shipmentId, ShipmentHandoverStatus status);

    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM ShipmentHandover h " +
           "WHERE h.shipment.id = :shipmentId " +
           "AND h.status IN (:statuses)")
    Long sumQuantityByShipmentIdAndStatusIn(@Param("shipmentId") UUID shipmentId,
                                            @Param("statuses") Collection<ShipmentHandoverStatus> statuses);

    @Query("SELECT h FROM ShipmentHandover h " +
           "WHERE h.status = :status AND h.expiresAt < :now")
    List<ShipmentHandover> findExpiredPending(@Param("status") ShipmentHandoverStatus status,
                                              @Param("now") LocalDateTime now);

    @Query("SELECT h FROM ShipmentHandover h " +
           "WHERE h.toOrganization.organizationId = :orgId " +
           "AND (:status IS NULL OR h.status = :status) " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "     OR LOWER(h.shipment.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(h.fromOrganization.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY h.createdAt DESC")
    Page<ShipmentHandover> findReceivedHandoversWithFilters(
            @Param("orgId") UUID orgId,
            @Param("status") ShipmentHandoverStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
