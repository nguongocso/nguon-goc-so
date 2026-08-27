package vn.nguongocso.trace.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;

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
	 * Lấy mã code
	 */
	Optional<TraceCode> findByCodeValue(String codeValue);

	/*
	 * Lấy giá trị code lớn nhất theo tổ chức và prefix
	 */
	@Query("SELECT MAX(t.codeValue) FROM TraceCode t WHERE t.shipment.organization.id = :orgId AND t.codeValue LIKE CONCAT(:prefix, '%')")
	String findMaxCodeValueByOrganization(@Param("orgId") UUID orgId, @Param("prefix") String prefix);

	/**
	 * Tìm TraceCode theo suspicionScore >= minScore và status cụ thể (phân trang).
	 */
	Page<TraceCode> findBySuspicionScoreGreaterThanEqualAndStatus(
			Integer suspicionScore, TraceCodeStatus status, Pageable pageable);

	/**
	 * Tìm TraceCode theo suspicionScore >= minScore và status nằm trong danh sách (phân
	 * trang).
	 */
	Page<TraceCode> findBySuspicionScoreGreaterThanEqualAndStatusIn(
			Integer suspicionScore, List<TraceCodeStatus> statuses, Pageable pageable);

	/**
	 * Tìm mã theo lô hàng và giá trị codeValue.
	 */
	Optional<TraceCode> findByShipmentIdAndCodeValue(UUID shipmentId, String codeValue);

	/**
	 * Lấy danh sách mã theo lô hàng và khoảng codeValue.
	 */
	List<TraceCode> findByShipmentIdAndCodeValueBetween(UUID shipmentId, String fromCode, String toCode);

	/**
	 * Lấy danh sách mã theo lô hàng và danh sách codeValue.
	 */
	List<TraceCode> findByShipmentIdAndCodeValueIn(UUID shipmentId, List<String> codeValues);
}
