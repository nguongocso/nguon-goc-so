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
import org.springframework.stereotype.Repository;

import vn.nguongocso.trace.entity.ShipmentHandover;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;

/** Repository quản lý biên bản bàn giao lô hàng. */
@Repository
public interface ShipmentHandoverRepository extends JpaRepository<ShipmentHandover, UUID> {
    /** Lấy danh sách bàn giao theo ID lô hàng. */
    List<ShipmentHandover> findByShipmentId(UUID shipmentId);

    /** Lấy danh sách bàn giao theo ID tổ chức nhận. */
    List<ShipmentHandover> findByToOrganizationOrganizationId(UUID orgId);

    /** Lấy danh sách bàn giao theo ID tổ chức giao. */
    List<ShipmentHandover> findByFromOrganizationOrganizationId(UUID orgId);

    /** Kiểm tra lô hàng có bàn giao theo trạng thái hay chưa. */
    boolean existsByShipmentIdAndStatus(UUID shipmentId, ShipmentHandoverStatus status);

    /** Kiểm tra lô hàng có bàn giao đến tổ chức nhận hay chưa. */
    boolean existsByShipmentIdAndToOrganizationOrganizationId(UUID shipmentId, UUID orgId);

    /** Kiểm tra lô hàng có bàn giao đến tổ chức nhận theo trạng thái hay chưa. */
    boolean existsByShipmentIdAndToOrganizationOrganizationIdAndStatus(UUID shipmentId, UUID orgId, ShipmentHandoverStatus status);

    /** Tính tổng số lượng bàn giao theo lô hàng và danh sách trạng thái. */
    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM ShipmentHandover h " +
           "WHERE h.shipment.id = :shipmentId " +
           "AND h.status IN (:statuses)")
    Long sumQuantityByShipmentIdAndStatusIn(@Param("shipmentId") UUID shipmentId,
                                            @Param("statuses") Collection<ShipmentHandoverStatus> statuses);

    /** Lấy danh sách bàn giao chờ duyệt đã hết hạn. */
    @Query("SELECT h FROM ShipmentHandover h " +
           "WHERE h.status = :status AND h.expiresAt < :now")
    List<ShipmentHandover> findExpiredPending(@Param("status") ShipmentHandoverStatus status,
                                              @Param("now") LocalDateTime now);

    /** Lấy danh sách bàn giao bên nhận có bộ lọc và phân trang. */
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

    /** Lấy danh sách bàn giao bên giao có bộ lọc và phân trang. */
    @Query("SELECT h FROM ShipmentHandover h " +
           "WHERE h.fromOrganization.organizationId = :orgId " +
           "AND (:status IS NULL OR h.status = :status) " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "     OR LOWER(h.shipment.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(h.toOrganization.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY h.createdAt DESC")
    Page<ShipmentHandover> findSentHandoversWithFilters(
            @Param("orgId") UUID orgId,
            @Param("status") ShipmentHandoverStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    /** Đếm số lượng biên bản bàn giao bên giao theo trạng thái (TASK-AI-05). */
    long countByFromOrganizationOrganizationIdAndStatus(UUID orgId, ShipmentHandoverStatus status);

    /** Đếm số lượng biên bản bàn giao bên giao theo danh sách tổ chức và trạng thái (TASK-AI-05 & TASK-AI-07). */
    long countByFromOrganizationOrganizationIdInAndStatus(Collection<UUID> orgIds, ShipmentHandoverStatus status);
}

