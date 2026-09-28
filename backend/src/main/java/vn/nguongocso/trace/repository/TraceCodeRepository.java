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

import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;

/** Repository quản lý mã truy xuất. */
@Repository
public interface TraceCodeRepository extends JpaRepository<TraceCode, UUID> {
    /** Kiểm tra mã truy xuất đã tồn tại theo giá trị hay chưa. */
    boolean existsByCodeValue(String codeValue);

    /** Lấy danh sách mã truy xuất theo lô hàng. */
    List<TraceCode> findByShipmentId(UUID shipmentId);

    /** Xóa tất cả mã truy xuất theo lô hàng. */
    void deleteByShipmentId(UUID shipmentId);

    /** Tìm mã truy xuất theo giá trị. */
    Optional<TraceCode> findByCodeValue(String codeValue);

    /** Tìm mã truy xuất theo ID và ID lô sản xuất. */
    Optional<TraceCode> findByIdAndShipment_ProductionLot_Id(UUID id, UUID productionLotId);

    /** Tìm mã truy xuất theo giá trị và ID lô sản xuất. */
    Optional<TraceCode> findByCodeValueAndShipment_ProductionLot_Id(String codeValue, UUID productionLotId);

    /** Lấy giá trị mã lớn nhất theo tổ chức và prefix. */
    @Query("SELECT MAX(t.codeValue) FROM TraceCode t WHERE t.shipment.organization.id = :orgId AND t.codeValue LIKE CONCAT(:prefix, '%')")
    String findMaxCodeValueByOrganization(@Param("orgId") UUID orgId, @Param("prefix") String prefix);

    /** Tìm mã truy xuất theo trạng thái có phân trang. */
    Page<TraceCode> findByStatus(TraceCodeStatus status, Pageable pageable);

    /** Tìm mã truy xuất theo danh sách trạng thái có phân trang. */
    Page<TraceCode> findByStatusIn(List<TraceCodeStatus> statuses, Pageable pageable);

    /** Tìm mã truy xuất theo điểm nghi vấn tối thiểu và trạng thái. */
    Page<TraceCode> findBySuspicionScoreGreaterThanEqualAndStatus(
            Integer suspicionScore, TraceCodeStatus status, Pageable pageable);

    /** Tìm mã truy xuất theo điểm nghi vấn tối thiểu và danh sách trạng thái. */
    Page<TraceCode> findBySuspicionScoreGreaterThanEqualAndStatusIn(
            Integer suspicionScore, List<TraceCodeStatus> statuses, Pageable pageable);

    /** Tìm các mã nghi vấn bao gồm phản hồi của người tiêu dùng. */
    @Query("""
            SELECT DISTINCT tc
            FROM TraceCode tc
            LEFT JOIN ProductFeedback pf
              ON pf.traceCode = tc AND pf.severity = :feedbackSeverity
            WHERE tc.status IN :statuses
              AND (COALESCE(tc.suspicionScore, 0) >= :minScore OR pf.id IS NOT NULL)
            """)
    Page<TraceCode> findSuspectsIncludingConsumerFeedback(
            @Param("minScore") Integer minScore,
            @Param("statuses") List<TraceCodeStatus> statuses,
            @Param("feedbackSeverity") ProductFeedbackSeverity feedbackSeverity,
            Pageable pageable);

    /** Kiểm tra lô sản xuất đã có mã truy xuất được kích hoạt hay chưa. */
    @Query("SELECT COUNT(tc) > 0 FROM TraceCode tc "
            + "WHERE tc.shipment.productionLot.id = :productionLotId "
            + "AND tc.status <> vn.nguongocso.trace.enums.TraceCodeStatus.INACTIVE")
    boolean existsActivatedByProductionLotId(@Param("productionLotId") UUID productionLotId);

    /** Đếm số tem chưa kích hoạt của lô sản xuất. */
    @Query("SELECT COUNT(tc) FROM TraceCode tc "
            + "WHERE tc.shipment.productionLot.id = :productionLotId "
            + "AND tc.shipment.status <> vn.nguongocso.trace.enums.ShipmentStatus.RECALLED "
            + "AND tc.status = vn.nguongocso.trace.enums.TraceCodeStatus.INACTIVE")
    long countInactiveByProductionLotId(@Param("productionLotId") UUID productionLotId);

    /** Đếm tổng số tem thuộc các lô hàng chưa thu hồi của lô sản xuất. */
    @Query("SELECT COUNT(tc) FROM TraceCode tc "
            + "WHERE tc.shipment.productionLot.id = :productionLotId "
            + "AND tc.shipment.status <> vn.nguongocso.trace.enums.ShipmentStatus.RECALLED")
    long countTotalByProductionLotId(@Param("productionLotId") UUID productionLotId);

    /** Tìm mã truy xuất theo lô hàng và giá trị. */
    Optional<TraceCode> findByShipmentIdAndCodeValue(UUID shipmentId, String codeValue);

    /** Lấy danh sách mã truy xuất theo lô hàng và dải giá trị. */
    List<TraceCode> findByShipmentIdAndCodeValueBetween(UUID shipmentId, String fromCode, String toCode);

    /** Khóa danh sách mã của lô hàng để cập nhật tách lô. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tc FROM TraceCode tc WHERE tc.shipment.id = :shipmentId ORDER BY tc.codeValue ASC, tc.id ASC")
    List<TraceCode> findAllByShipmentIdForSplitUpdate(@Param("shipmentId") UUID shipmentId);

    /** Lấy danh sách mã truy xuất theo lô hàng và danh sách giá trị. */
    List<TraceCode> findByShipmentIdAndCodeValueIn(UUID shipmentId, List<String> codeValues);

    /** Lấy danh sách mã tem theo lô hàng và tổ chức có bộ lọc và phân trang. */
    @Query("SELECT tc FROM TraceCode tc WHERE tc.shipment.id = :shipmentId "
            + "AND tc.shipment.organization.organizationId = :orgId "
            + "AND (:status IS NULL OR tc.status = :status) "
            + "AND (:search IS NULL OR LOWER(tc.codeValue) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "ORDER BY tc.codeValue ASC")
    Page<TraceCode> findByShipmentAndFilters(
            @Param("shipmentId") UUID shipmentId,
            @Param("orgId") UUID orgId,
            @Param("status") TraceCodeStatus status,
            @Param("search") String search,
            Pageable pageable);

    /** Lấy tất cả mã tem theo lô hàng và tổ chức có bộ lọc để xuất danh sách. */
    @Query("SELECT tc FROM TraceCode tc WHERE tc.shipment.id = :shipmentId "
            + "AND tc.shipment.organization.organizationId = :orgId "
            + "AND (:status IS NULL OR tc.status = :status) "
            + "AND (:search IS NULL OR LOWER(tc.codeValue) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "ORDER BY tc.codeValue ASC")
    List<TraceCode> findAllByShipmentAndFilters(
            @Param("shipmentId") UUID shipmentId,
            @Param("orgId") UUID orgId,
            @Param("status") TraceCodeStatus status,
            @Param("search") String search);

    /** Tìm danh sách mã tem theo danh sách lô sản xuất và trạng thái. */
    @Query("SELECT tc FROM TraceCode tc WHERE tc.shipment.productionLot.id IN :lotIds AND tc.status = :status")
    List<TraceCode> findByProductionLotIdsAndStatus(
            @Param("lotIds") Collection<UUID> lotIds,
            @Param("status") TraceCodeStatus status);

    /** Kiểm tra lô hàng có mã truy xuất thuộc trạng thái chỉ định hay chưa. */
    boolean existsByShipmentIdAndStatus(UUID shipmentId, TraceCodeStatus status);

    /** Đếm số tem đã kích hoạt theo từng tổ chức trong khoảng thời gian. */
    @Query("""
            SELECT tc.shipment.organization.organizationId, COUNT(tc)
            FROM TraceCode tc
            WHERE tc.activatedAt BETWEEN :from AND :to
            GROUP BY tc.shipment.organization.organizationId
            """)
    List<Object[]> countActivatedGroupedByOrg(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Lấy thời điểm kích hoạt tem mới nhất của từng tổ chức. */
    @Query("""
            SELECT tc.shipment.organization.organizationId, MAX(tc.activatedAt)
            FROM TraceCode tc
            WHERE tc.activatedAt IS NOT NULL
            GROUP BY tc.shipment.organization.organizationId
            """)
    List<Object[]> maxActivatedAtGroupedByOrg();
}
