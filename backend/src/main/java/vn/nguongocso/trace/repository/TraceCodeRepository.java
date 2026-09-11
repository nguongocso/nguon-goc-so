package vn.nguongocso.trace.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;

/**
 * Repository quản lý mã truy xuất.
 */
public interface TraceCodeRepository extends JpaRepository<TraceCode, UUID> {
	/**
	 * Kiểm tra mã đã tồn tại.
	 */
	boolean existsByCodeValue(String codeValue);

	/**
	 * Lấy mã theo lô hàng.
	 */
	List<TraceCode> findByShipmentId(UUID shipmentId);

	/**
	 * Xóa mã theo lô hàng.
	 */
	void deleteByShipmentId(UUID shipmentId);

	/**
	 * Lấy mã code.
	 */
	Optional<TraceCode> findByCodeValue(String codeValue);

	Optional<TraceCode> findByIdAndShipment_ProductionLot_Id(UUID id, UUID productionLotId);

	Optional<TraceCode> findByCodeValueAndShipment_ProductionLot_Id(String codeValue, UUID productionLotId);

	/**
	 * Lấy giá trị code lớn nhất theo tổ chức và prefix.
	 */
	@Query("SELECT MAX(t.codeValue) FROM TraceCode t WHERE t.shipment.organization.id = :orgId AND t.codeValue LIKE CONCAT(:prefix, '%')")
	String findMaxCodeValueByOrganization(@Param("orgId") UUID orgId, @Param("prefix") String prefix);

	/**
	 * Tìm TraceCode theo status cụ thể (phân trang).
	 */
	Page<TraceCode> findByStatus(TraceCodeStatus status, Pageable pageable);

	/**
	 * Tìm TraceCode theo status nằm trong danh sách (phân trang).
	 */
	Page<TraceCode> findByStatusIn(List<TraceCodeStatus> statuses, Pageable pageable);

	/**
	 * Tìm TraceCode theo suspicionScore >= minScore và status cụ thể (phân trang).
	 */
	Page<TraceCode> findBySuspicionScoreGreaterThanEqualAndStatus(
			Integer suspicionScore, TraceCodeStatus status, Pageable pageable);

	/**
	 * Tìm TraceCode theo suspicionScore >= minScore và status nằm trong danh sách (phân trang).
	 */
	Page<TraceCode> findBySuspicionScoreGreaterThanEqualAndStatusIn(
			Integer suspicionScore, List<TraceCodeStatus> statuses, Pageable pageable);

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

	/**
	 * NCL-03-CN-006: kiểm tra lô sản xuất đã có mã truy xuất được kích hoạt
	 * (trạng thái khác INACTIVE — đã rời trạng thái dự thảo) trên bất kỳ lô
	 * hàng nào của lô sản xuất hay chưa.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @return true nếu đã có mã kích hoạt, ngược lại false
	 */
	@Query("SELECT COUNT(tc) > 0 FROM TraceCode tc "
			+ "WHERE tc.shipment.productionLot.id = :productionLotId "
			+ "AND tc.status <> vn.nguongocso.trace.enums.TraceCodeStatus.INACTIVE")
	boolean existsActivatedByProductionLotId(@Param("productionLotId") UUID productionLotId);

	/**
	 * NCL-11-CN-004: Đếm số tem chưa kích hoạt (INACTIVE) thuộc các lô hàng chưa thu hồi của lô sản xuất.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @return số tem INACTIVE
	 */
	@Query("SELECT COUNT(tc) FROM TraceCode tc "
			+ "WHERE tc.shipment.productionLot.id = :productionLotId "
			+ "AND tc.shipment.status <> vn.nguongocso.trace.enums.ShipmentStatus.RECALLED "
			+ "AND tc.status = vn.nguongocso.trace.enums.TraceCodeStatus.INACTIVE")
	long countInactiveByProductionLotId(@Param("productionLotId") UUID productionLotId);

	/**
	 * NCL-11-CN-004: Đếm tổng số tem đã in thuộc các lô hàng chưa thu hồi của lô sản xuất.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @return tổng số tem
	 */
	@Query("SELECT COUNT(tc) FROM TraceCode tc "
			+ "WHERE tc.shipment.productionLot.id = :productionLotId "
			+ "AND tc.shipment.status <> vn.nguongocso.trace.enums.ShipmentStatus.RECALLED")
	long countTotalByProductionLotId(@Param("productionLotId") UUID productionLotId);

	/**
	 * Tìm mã theo lô hàng và giá trị codeValue.
	 */
	Optional<TraceCode> findByShipmentIdAndCodeValue(UUID shipmentId, String codeValue);

	/**
	 * Lấy danh sách mã theo lô hàng và khoảng codeValue.
	 */
	List<TraceCode> findByShipmentIdAndCodeValueBetween(UUID shipmentId, String fromCode, String toCode);

	/** Khóa tập mã của lô theo thứ tự ổn định trước khi phân bổ sang các lô con. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT tc FROM TraceCode tc WHERE tc.shipment.id = :shipmentId ORDER BY tc.codeValue ASC, tc.id ASC")
	List<TraceCode> findAllByShipmentIdForSplitUpdate(@Param("shipmentId") UUID shipmentId);

	/**
	 * Lấy danh sách mã theo lô hàng và danh sách codeValue.
	 */
	List<TraceCode> findByShipmentIdAndCodeValueIn(UUID shipmentId, List<String> codeValues);

	/**
	 * Lấy danh sách mã tem theo lô hàng và tổ chức, hỗ trợ lọc theo trạng thái và tìm kiếm (phân trang) (NCL-04-CN-008).
	 */
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

	/**
	 * Lấy tất cả mã tem theo lô hàng và tổ chức, hỗ trợ lọc theo trạng thái và tìm kiếm để xuất file (NCL-04-CN-008).
	 */
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
}
