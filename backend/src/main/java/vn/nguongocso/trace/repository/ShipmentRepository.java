package vn.nguongocso.trace.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import vn.nguongocso.report.dto.response.ProductBreakdownItem;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;

/** Repository quản lý lô hàng. */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    /** Lấy danh sách lô hàng theo ID của lô sản xuất. */
    List<Shipment> findByProductionLotId(UUID productionLotId);

    /** Lấy danh sách lô hàng theo ID lô sản xuất và trạng thái. */
    List<Shipment> findByProductionLotIdAndStatus(UUID productionLotId, ShipmentStatus status);

    /** Lấy danh sách lô hàng theo ID lô sản xuất và danh sách trạng thái. */
    List<Shipment> findByProductionLotIdAndStatusIn(UUID productionLotId, Collection<ShipmentStatus> statuses);

    /** Lấy danh sách lô hàng theo tổ chức và trạng thái. */
    List<Shipment> findByOrganization_OrganizationIdAndStatus(UUID organizationId, ShipmentStatus status);

    /** Lấy danh sách lô hàng theo tổ chức và danh sách trạng thái. */
    List<Shipment> findByOrganization_OrganizationIdAndStatusIn(UUID organizationId, Collection<ShipmentStatus> statuses);

    /** Lấy tất cả lô hàng theo tổ chức. */
    List<Shipment> findByOrganization_OrganizationId(UUID organizationId);

    /** Lấy danh sách lô hàng theo ID lô sản xuất có phân trang. */
    Page<Shipment> findByProductionLotId(UUID productionLotId, Pageable pageable);

    /** Tính tổng sản lượng của các lô hàng theo địa bàn và khoảng thời gian. */
    @Query("SELECT COALESCE(SUM(s.totalQuantity), 0) " +
            "FROM Shipment s " +
            "WHERE s.organization.organizationId IN :organizationIds " +
            "AND s.status <> vn.nguongocso.trace.enums.ShipmentStatus.SPLIT " +
            "AND s.createdAt >= :fromDate " +
            "AND s.createdAt < :toDate")
    Double getTotalQuantity(
            @Param("organizationIds") List<UUID> organizationIds,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    /** Thống kê số lô hàng và tổng sản lượng theo từng loại nông sản. */
    @Query("SELECT new vn.nguongocso.report.dto.response.ProductBreakdownItem(" +
            "pc.name, " +
            "COUNT(s), " +
            "COALESCE(SUM(s.totalQuantity), 0)" +
            ") " +
            "FROM Shipment s " +
            "JOIN s.productionLot pl " +
            "JOIN pl.productCategory pc " +
            "WHERE s.organization.organizationId IN :organizationIds " +
            "AND s.status <> vn.nguongocso.trace.enums.ShipmentStatus.SPLIT " +
            "AND s.createdAt >= :fromDate " +
            "AND s.createdAt < :toDate " +
            "GROUP BY pc.name " +
            "ORDER BY COALESCE(SUM(s.totalQuantity), 0) DESC")
    List<ProductBreakdownItem> getProductBreakdown(
            @Param("organizationIds") List<UUID> organizationIds,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    /** Đếm số lô hàng của các tổ chức trong khoảng thời gian. */
    @Query("SELECT COUNT(s) " +
            "FROM Shipment s " +
            "WHERE s.organization.organizationId IN :organizationIds " +
            "AND s.status <> vn.nguongocso.trace.enums.ShipmentStatus.SPLIT " +
            "AND s.createdAt >= :fromDate " +
            "AND s.createdAt < :toDate")
    Long countShipments(
            @Param("organizationIds") List<UUID> organizationIds,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    /** Tìm lô hàng theo ID và tổ chức sở hữu. */
    Optional<Shipment> findByIdAndOrganization_OrganizationId(
            UUID shipmentId,
            UUID organizationId);

    /** Khóa lô hàng trong transaction để tạo/cập nhật thu hồi. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shipment s " +
            "WHERE s.id = :shipmentId " +
            "AND s.organization.organizationId = :organizationId")
    Optional<Shipment> findOwnedByIdForRecallUpdate(
            @Param("shipmentId") UUID shipmentId,
            @Param("organizationId") UUID organizationId);

    /** Khóa lô nguồn thuộc tổ chức hiện tại để thực hiện tách lô. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shipment s " +
            "WHERE s.id = :shipmentId " +
            "AND s.organization.organizationId = :organizationId")
    Optional<Shipment> findOwnedByIdForSplitUpdate(
            @Param("shipmentId") UUID shipmentId,
            @Param("organizationId") UUID organizationId);

    /** Lấy các lô con trực tiếp của một lô cha theo thứ tự tạo. */
    List<Shipment> findAllByParentShipment_IdOrderByCreatedAtAsc(UUID parentShipmentId);

    /** Kiểm tra lô hàng đã có lô con trực tiếp hay chưa. */
    boolean existsByParentShipment_Id(UUID parentShipmentId);

    /** Đếm số lô con của một lô cha. */
    long countByParentShipment_Id(UUID parentShipmentId);

    /** Tìm lô con theo ID và tổ chức nhận. */
    Optional<Shipment> findByIdAndRecipientOrganization_OrganizationId(
            UUID shipmentId,
            UUID recipientOrganizationId);

    /** Lấy danh sách lô hàng đủ điều kiện thu mua theo trạng thái. */
    List<Shipment> findByStatusOrderByCreatedAtDesc(ShipmentStatus status);

    /** Lấy danh sách lô hàng theo trạng thái và tổ chức nhận. */
    List<Shipment> findByStatusAndRecipientOrganization_OrganizationIdOrderByCreatedAtDesc(
            ShipmentStatus status, UUID recipientOrganizationId);

    /** Lấy danh sách lô hàng đủ điều kiện xuất báo cáo theo tiêu chí lọc. */
    @Query("SELECT s FROM Shipment s " +
            "LEFT JOIN s.productionLot pl " +
            "LEFT JOIN pl.productCategory pc " +
            "WHERE (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
            "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
            "AND s.status <> vn.nguongocso.trace.enums.ShipmentStatus.RECALLED " +
            "AND s.status <> vn.nguongocso.trace.enums.ShipmentStatus.SPLIT " +
            "AND (:categoryIds IS NULL OR pc.id IN :categoryIds) " +
            "AND (:shipmentIds IS NULL OR s.id IN :shipmentIds)")
    List<Shipment> findEligibleShipments(
            @Param("orgId") UUID orgId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("categoryIds") List<UUID> categoryIds,
            @Param("shipmentIds") List<UUID> shipmentIds);

    /** Lấy danh sách lô hàng theo danh sách ID lô sản xuất. */
    @Query("SELECT s FROM Shipment s WHERE s.productionLot.id IN :productionLotIds")
    List<Shipment> findByProductionLotIdIn(@Param("productionLotIds") List<UUID> productionLotIds);
}
