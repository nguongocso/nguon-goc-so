package vn.nguongocso.trace.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.report.dto.response.ProductBreakdownItem;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;

/**
 * Repository quản lý lô hàng.
 */
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

        /**
         * Lấy danh sách lô hàng theo ID của lô sản xuất.
         */
        List<Shipment> findByProductionLotId(UUID productionLotId);

        /**
         * Lấy danh sách lô hàng theo ID của lô sản xuất với phân trang.
         */
        Page<Shipment> findByProductionLotId(UUID productionLotId, Pageable pageable);

        /**
         * Tính tổng sản lượng của các lô hàng theo địa bàn và khoảng thời gian.
         */
        @Query("SELECT COALESCE(SUM(s.totalQuantity), 0) " +
                        "FROM Shipment s " +
                        "WHERE s.organization.organizationId IN :organizationIds " +
                        "AND s.createdAt >= :fromDate " +
                        "AND s.createdAt < :toDate")
        Double getTotalQuantity(
                        @Param("organizationIds") List<UUID> organizationIds,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate);

        /**
         * Thống kê số lô hàng và tổng sản lượng theo từng loại nông sản.
         */
        @Query("SELECT new vn.nguongocso.report.dto.response.ProductBreakdownItem(" +
                        "pc.name, " +
                        "COUNT(s), " +
                        "COALESCE(SUM(s.totalQuantity), 0)" +
                        ") " +
                        "FROM Shipment s " +
                        "JOIN s.productionLot pl " +
                        "JOIN pl.productCategory pc " +
                        "WHERE s.organization.organizationId IN :organizationIds " +
                        "AND s.createdAt >= :fromDate " +
                        "AND s.createdAt < :toDate " +
                        "GROUP BY pc.name " +
                        "ORDER BY COALESCE(SUM(s.totalQuantity), 0) DESC")
        List<ProductBreakdownItem> getProductBreakdown(
                        @Param("organizationIds") List<UUID> organizationIds,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate);

        /**
         * Đếm số lô hàng của các tổ chức trong khoảng thời gian.
         */
        @Query("SELECT COUNT(s) " +
                        "FROM Shipment s " +
                        "WHERE s.organization.organizationId IN :organizationIds " +
                        "AND s.createdAt >= :fromDate " +
                        "AND s.createdAt < :toDate")
        Long countShipments(
                        @Param("organizationIds") List<UUID> organizationIds,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate);

        /**
         * Tìm lô hàng theo ID và tổ chức.
         */
        Optional<Shipment> findByIdAndOrganization_OrganizationId(
                        UUID shipmentId,
                        UUID organizationId);

        /**
         * Lấy danh sách lô hàng đủ điều kiện thu mua (status = ACTIVATED).
         * Dùng cho Doanh nghiệp thu mua (VT‑04) xem các lô hàng sẵn sàng.
         */
        List<Shipment> findByStatusOrderByCreatedAtDesc(ShipmentStatus status);

        /**
         * Lấy danh sách lô hàng đủ điều kiện xuất báo cáo / lọc theo nhiều tiêu chí.
         * Bao gồm: tổ chức, khoảng thời gian, danh mục sản phẩm, danh sách shipment.
         * Mặc định loại trừ các lô đã bị thu hồi (RECALLED).
         */
        @Query("SELECT s FROM Shipment s " +
                        "LEFT JOIN s.productionLot pl " +
                        "LEFT JOIN pl.productCategory pc " +
                        "WHERE (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
                        "AND (:fromDate IS NULL OR s.createdAt >= :fromDate) " +
                        "AND (:toDate IS NULL OR s.createdAt <= :toDate) " +
                        "AND s.status <> vn.nguongocso.trace.enums.ShipmentStatus.RECALLED " +
                        "AND (:categoryIds IS NULL OR pc.id IN :categoryIds) " +
                        "AND (:shipmentIds IS NULL OR s.id IN :shipmentIds)")
        List<Shipment> findEligibleShipments(
                        @Param("orgId") UUID orgId,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate,
                        @Param("categoryIds") List<UUID> categoryIds,
                        @Param("shipmentIds") List<UUID> shipmentIds);

        /**
         * Lấy danh sách lô hàng theo danh sách ID của lô sản xuất.
         */
        @Query("SELECT s FROM Shipment s WHERE s.productionLot.id IN :productionLotIds")
        List<Shipment> findByProductionLotIdIn(@Param("productionLotIds") List<UUID> productionLotIds);
}
