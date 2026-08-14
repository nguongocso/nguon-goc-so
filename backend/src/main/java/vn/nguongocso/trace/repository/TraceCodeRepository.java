package vn.nguongocso.trace.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.trace.entity.TraceCode;

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
}
