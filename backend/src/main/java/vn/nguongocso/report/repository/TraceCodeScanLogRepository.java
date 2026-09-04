package vn.nguongocso.report.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.report.entity.TraceCodeScanLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Repository thao tác TraceCodeScanLog. */
public interface TraceCodeScanLogRepository extends JpaRepository<TraceCodeScanLog, UUID> {
        /** Đếm số lượt quét của một mã truy xuất sau một thời điểm nhất định. */
        long countByTraceCodeIdAndScannedAtAfter(UUID traceCodeId, LocalDateTime time);

        /**
         * Lấy danh sách các lượt quét của một mã truy xuất sau một thời điểm nhất định,
         * sắp xếp theo thời gian quét giảm dần.
         */
        List<TraceCodeScanLog> findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(UUID traceCodeId,
                        LocalDateTime time);

        /**
         * Lấy danh sách các lượt quét của một mã truy xuất trong một khoảng thời gian,
         * sắp xếp theo thời gian quét giảm dần.
         */
        @Query("SELECT COUNT(l) FROM TraceCodeScanLog l " +
                        "JOIN l.traceCode tc JOIN tc.shipment s " +
                        "WHERE (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
                        "AND (:lotId IS NULL OR s.productionLot.id = :lotId) " +
                        "AND (:shipmentId IS NULL OR s.id = :shipmentId) " +
                        "AND (CAST(:startDate AS date) IS NULL OR l.scannedAt >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR l.scannedAt <= :endDate)")
        long countScans(@Param("orgId") UUID orgId,
                        @Param("lotId") UUID lotId,
                        @Param("shipmentId") UUID shipmentId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Đếm số lượng mã truy xuất duy nhất đã được quét trong một khoảng thời gian.
         */
        @Query("SELECT COUNT(DISTINCT tc.id) FROM TraceCodeScanLog l " +
                        "JOIN l.traceCode tc JOIN tc.shipment s " +
                        "WHERE (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
                        "AND (:lotId IS NULL OR s.productionLot.id = :lotId) " +
                        "AND (:shipmentId IS NULL OR s.id = :shipmentId) " +
                        "AND (CAST(:startDate AS date) IS NULL OR l.scannedAt >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR l.scannedAt <= :endDate)")
        long countUniqueCodes(@Param("orgId") UUID orgId,
                        @Param("lotId") UUID lotId,
                        @Param("shipmentId") UUID shipmentId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Lấy danh sách các lượt quét bất thường trong một khoảng thời gian, sắp xếp
         * theo thời gian quét giảm dần.
         */
        @Query("SELECT COUNT(l) FROM TraceCodeScanLog l " +
                        "JOIN l.traceCode tc JOIN tc.shipment s " +
                        "WHERE l.isAbnormal = true " +
                        "AND (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
                        "AND (:lotId IS NULL OR s.productionLot.id = :lotId) " +
                        "AND (:shipmentId IS NULL OR s.id = :shipmentId) " +
                        "AND (CAST(:startDate AS date) IS NULL OR l.scannedAt >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR l.scannedAt <= :endDate)")
        long countAbnormalScans(@Param("orgId") UUID orgId,
                        @Param("lotId") UUID lotId,
                        @Param("shipmentId") UUID shipmentId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /** Lấy thống kê số lượt quét theo vị trí trong một khoảng thời gian. */
        @Query("SELECT l.location AS location, COUNT(l.id) AS scanCount FROM TraceCodeScanLog l " +
                        "JOIN l.traceCode tc JOIN tc.shipment s " +
                        "WHERE (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
                        "AND (:lotId IS NULL OR s.productionLot.id = :lotId) " +
                        "AND (:shipmentId IS NULL OR s.id = :shipmentId) " +
                        "AND (CAST(:startDate AS date) IS NULL OR l.scannedAt >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR l.scannedAt <= :endDate) " +
                        "GROUP BY l.location " +
                        "ORDER BY COUNT(l.id) DESC")
        List<Object[]> getStatsByLocation(@Param("orgId") UUID orgId,
                        @Param("lotId") UUID lotId,
                        @Param("shipmentId") UUID shipmentId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /** Lấy thống kê số lượt quét theo lô sản xuất trong một khoảng thời gian. */
        @Query("SELECT pl.id AS lotId, pl.name AS lotName, COUNT(l.id) AS scanCount, SUM(CASE WHEN l.isAbnormal = true THEN 1 ELSE 0 END) AS abnormalCount FROM TraceCodeScanLog l "
                        +
                        "JOIN l.traceCode tc JOIN tc.shipment s JOIN s.productionLot pl " +
                        "WHERE (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
                        "AND (:lotId IS NULL OR pl.id = :lotId) " +
                        "AND (:shipmentId IS NULL OR s.id = :shipmentId) " +
                        "AND (CAST(:startDate AS date) IS NULL OR l.scannedAt >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR l.scannedAt <= :endDate) " +
                        "GROUP BY pl.id, pl.name " +
                        "ORDER BY COUNT(l.id) DESC")
        List<Object[]> getStatsByProductionLot(@Param("orgId") UUID orgId,
                        @Param("lotId") UUID lotId,
                        @Param("shipmentId") UUID shipmentId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Lấy danh sách thời điểm quét của các lượt quét trong một khoảng thời gian,
         * sắp xếp theo thời gian quét tăng dần.
         */
        @Query("SELECT l.scannedAt FROM TraceCodeScanLog l " +
                        "JOIN l.traceCode tc JOIN tc.shipment s " +
                        "WHERE (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
                        "AND (:lotId IS NULL OR s.productionLot.id = :lotId) " +
                        "AND (:shipmentId IS NULL OR s.id = :shipmentId) " +
                        "AND (CAST(:startDate AS date) IS NULL OR l.scannedAt >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR l.scannedAt <= :endDate) " +
                        "ORDER BY l.scannedAt ASC")
        List<LocalDateTime> getScannedAtList(@Param("orgId") UUID orgId,
                        @Param("lotId") UUID lotId,
                        @Param("shipmentId") UUID shipmentId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Lấy danh sách các lượt quét bất thường trong một khoảng thời gian, sắp xếp
         * theo thời gian quét giảm dần.
         */
        @Query("SELECT l FROM TraceCodeScanLog l " +
                        "JOIN l.traceCode tc JOIN tc.shipment s JOIN s.productionLot pl " +
                        "WHERE l.isAbnormal = true " +
                        "AND (:orgId IS NULL OR s.organization.organizationId = :orgId) " +
                        "AND (:lotId IS NULL OR pl.id = :lotId) " +
                        "AND (CAST(:startDate AS date) IS NULL OR l.scannedAt >= :startDate) " +
                        "AND (CAST(:endDate AS date) IS NULL OR l.scannedAt <= :endDate) " +
                        "ORDER BY l.scannedAt DESC")
        Page<TraceCodeScanLog> findAbnormalScans(@Param("orgId") UUID orgId,
                        @Param("lotId") UUID lotId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /** Lấy các lượt quét gần nhất của mã truy xuất. */
        List<TraceCodeScanLog> findByTraceCodeIdAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
                        UUID traceCodeId,
                        LocalDateTime scannedAt);

        /**
         * Lấy tất cả các lượt quét từ một thời điểm trở lại đây, sắp xếp theo thời gian quét tăng dần.
         */
        List<TraceCodeScanLog> findByScannedAtGreaterThanEqualOrderByScannedAtAsc(LocalDateTime scannedAt);

}
