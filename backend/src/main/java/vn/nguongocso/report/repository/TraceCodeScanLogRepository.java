package vn.nguongocso.report.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.report.entity.TraceCodeScanLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Repository quản lý nhật ký quét mã truy xuất. */
@Repository
public interface TraceCodeScanLogRepository extends JpaRepository<TraceCodeScanLog, UUID> {

    /** Đếm tổng số lượt quét của một mã truy xuất. */
    long countByTraceCode_Id(UUID traceCodeId);

    /** Lấy 5 lượt quét gần nhất của một mã truy xuất. */
    List<TraceCodeScanLog> findTop5ByTraceCode_IdOrderByScannedAtDesc(UUID traceCodeId);

    /** Đếm số lượt quét gom nhóm theo danh sách ID mã tem. */
    @Query("SELECT l.traceCode.id, COUNT(l) FROM TraceCodeScanLog l WHERE l.traceCode.id IN :traceCodeIds GROUP BY l.traceCode.id")
    List<Object[]> countScansByTraceCodeIds(@Param("traceCodeIds") List<UUID> traceCodeIds);

    /** Đếm số lượt quét của một mã truy xuất sau một thời điểm. */
    long countByTraceCodeIdAndScannedAtAfter(UUID traceCodeId, LocalDateTime time);

    /** Lấy danh sách lượt quét của mã truy xuất sau một thời điểm. */
    List<TraceCodeScanLog> findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(
            UUID traceCodeId,
            LocalDateTime time);

    /** Đếm tổng số lượt quét theo các điều kiện lọc. */
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

    /** Đếm số lượng mã truy xuất duy nhất đã được quét trong khoảng thời gian. */
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

    /** Đếm số lượt quét bất thường trong khoảng thời gian. */
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

    /** Lấy thống kê số lượt quét theo vị trí trong khoảng thời gian. */
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

    /** Lấy thống kê số lượt quét theo lô sản xuất trong khoảng thời gian. */
    @Query("SELECT pl.id AS lotId, pl.name AS lotName, COUNT(l.id) AS scanCount, SUM(CASE WHEN l.isAbnormal = true THEN 1 ELSE 0 END) AS abnormalCount FROM TraceCodeScanLog l " +
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

    /** Lấy danh sách thời điểm quét trong khoảng thời gian. */
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

    /** Tổng hợp số lượt quét theo ngày trực tiếp tại CSDL. */
    @Query(value = """
            SELECT DATE_FORMAT(l.scanned_at, '%Y-%m-%d') AS period, COUNT(l.id) AS scan_count
            FROM trace_code_scan_logs l
            JOIN trace_codes tc ON l.trace_code_id = tc.id
            JOIN shipments s ON tc.shipment_id = s.id
            WHERE (:orgId IS NULL OR s.organization_id = :orgId)
              AND (:lotId IS NULL OR s.production_lot_id = :lotId)
              AND (:shipmentId IS NULL OR s.id = :shipmentId)
              AND (:startDate IS NULL OR l.scanned_at >= :startDate)
              AND (:endDate IS NULL OR l.scanned_at <= :endDate)
            GROUP BY period
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> getTimeSeriesGroupedByDay(
            @Param("orgId") UUID orgId,
            @Param("lotId") UUID lotId,
            @Param("shipmentId") UUID shipmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /** Tổng hợp số lượt quét theo tuần trực tiếp tại CSDL. */
    @Query(value = """
            SELECT DATE_FORMAT(l.scanned_at, '%x-W%v') AS period, COUNT(l.id) AS scan_count
            FROM trace_code_scan_logs l
            JOIN trace_codes tc ON l.trace_code_id = tc.id
            JOIN shipments s ON tc.shipment_id = s.id
            WHERE (:orgId IS NULL OR s.organization_id = :orgId)
              AND (:lotId IS NULL OR s.production_lot_id = :lotId)
              AND (:shipmentId IS NULL OR s.id = :shipmentId)
              AND (:startDate IS NULL OR l.scanned_at >= :startDate)
              AND (:endDate IS NULL OR l.scanned_at <= :endDate)
            GROUP BY period
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> getTimeSeriesGroupedByWeek(
            @Param("orgId") UUID orgId,
            @Param("lotId") UUID lotId,
            @Param("shipmentId") UUID shipmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /** Tổng hợp số lượt quét theo tháng trực tiếp tại CSDL. */
    @Query(value = """
            SELECT DATE_FORMAT(l.scanned_at, '%Y-%m') AS period, COUNT(l.id) AS scan_count
            FROM trace_code_scan_logs l
            JOIN trace_codes tc ON l.trace_code_id = tc.id
            JOIN shipments s ON tc.shipment_id = s.id
            WHERE (:orgId IS NULL OR s.organization_id = :orgId)
              AND (:lotId IS NULL OR s.production_lot_id = :lotId)
              AND (:shipmentId IS NULL OR s.id = :shipmentId)
              AND (:startDate IS NULL OR l.scanned_at >= :startDate)
              AND (:endDate IS NULL OR l.scanned_at <= :endDate)
            GROUP BY period
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> getTimeSeriesGroupedByMonth(
            @Param("orgId") UUID orgId,
            @Param("lotId") UUID lotId,
            @Param("shipmentId") UUID shipmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /** Tổng hợp số lượt quét theo năm trực tiếp tại CSDL. */
    @Query(value = """
            SELECT DATE_FORMAT(l.scanned_at, '%Y') AS period, COUNT(l.id) AS scan_count
            FROM trace_code_scan_logs l
            JOIN trace_codes tc ON l.trace_code_id = tc.id
            JOIN shipments s ON tc.shipment_id = s.id
            WHERE (:orgId IS NULL OR s.organization_id = :orgId)
              AND (:lotId IS NULL OR s.production_lot_id = :lotId)
              AND (:shipmentId IS NULL OR s.id = :shipmentId)
              AND (:startDate IS NULL OR l.scanned_at >= :startDate)
              AND (:endDate IS NULL OR l.scanned_at <= :endDate)
            GROUP BY period
            ORDER BY period ASC
            """, nativeQuery = true)
    List<Object[]> getTimeSeriesGroupedByYear(
            @Param("orgId") UUID orgId,
            @Param("lotId") UUID lotId,
            @Param("shipmentId") UUID shipmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /** Lấy danh sách lượt quét bất thường có phân trang. */
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

    /** Lấy danh sách lượt quét gần nhất của mã truy xuất từ một thời điểm. */
    List<TraceCodeScanLog> findByTraceCodeIdAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
            UUID traceCodeId,
            LocalDateTime scannedAt);

    /** Lấy danh sách tất cả lượt quét từ một thời điểm trở lại đây. */
    List<TraceCodeScanLog> findByScannedAtGreaterThanEqualOrderByScannedAtAsc(LocalDateTime scannedAt);

    /** Đếm số lượt tra cứu công khai theo từng tổ chức sở hữu tem. */
    @Query("SELECT s.organization.organizationId, COUNT(l) FROM TraceCodeScanLog l " +
            "JOIN l.traceCode tc JOIN tc.shipment s " +
            "WHERE l.scannedAt BETWEEN :from AND :to " +
            "GROUP BY s.organization.organizationId")
    List<Object[]> countScansGroupedByOrg(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Lấy thời điểm tra cứu công khai mới nhất của từng tổ chức. */
    @Query("SELECT s.organization.organizationId, MAX(l.scannedAt) FROM TraceCodeScanLog l " +
            "JOIN l.traceCode tc JOIN tc.shipment s " +
            "GROUP BY s.organization.organizationId")
    List<Object[]> maxScannedAtGroupedByOrg();
}
