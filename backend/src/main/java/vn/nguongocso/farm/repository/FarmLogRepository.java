package vn.nguongocso.farm.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.projection.FarmLogProjection;

/**
 * Repository thao tác dữ liệu nhật ký canh tác.
*/
public interface FarmLogRepository extends JpaRepository<FarmLog, UUID> {
	/** Lấy danh sách nhật ký canh tác của lô sản xuất theo phân trang. */
	@Query("""
			SELECT
			    fl.id AS id,
			    pl.id AS productionLotId,
			    pl.name AS productionLotName,
			    fl.activityType AS activityType,
			    fl.material AS material,
			    fl.quantity AS quantity,
			    fl.unit AS unit,
			    fl.executedDate AS executedDate,
			    fl.notes AS notes,
			    u.fullName AS createdByName,
			    fl.createdAt AS createdAt
			FROM FarmLog fl
			JOIN fl.productionLotId pl
			JOIN fl.createdBy u
			WHERE pl = :productionLot
			""")
	Page<FarmLogProjection> findByProductionLot(
			ProductionLot productionLot,
			Pageable pageable);

	/** Lấy nhật ký canh tác của lô sản xuất theo phân trang. */
	Page<FarmLog> findByProductionLotId(ProductionLot productionLot, Pageable pageable);

	/** Lấy nhật ký canh tác theo ID lô sản xuất theo ngày thực hiện. */
	List<FarmLog> findByProductionLotId_IdOrderByExecutedDateAsc(UUID productionLotId);

	/** Kiểm tra lô sản xuất đã có nhật ký canh tác hay chưa. */
	@Query("SELECT COUNT(fl) > 0 FROM FarmLog fl " +
			"WHERE fl.productionLotId.id = :productionLotId")
	boolean existsByProductionLotId(@Param("productionLotId") UUID productionLotId);

	/** Đếm số nhật ký canh tác của lô sản xuất. */
	@Query("SELECT COUNT(fl) FROM FarmLog fl WHERE fl.productionLotId.id = :productionLotId")
	long countByProductionLotId(@Param("productionLotId") UUID productionLotId);

	/** Kiểm tra vật tư đã từng được dùng trong nhật ký canh tác hay chưa. */
	@Query("SELECT COUNT(fl) > 0 FROM FarmLog fl WHERE LOWER(TRIM(fl.material)) = LOWER(TRIM(:materialName))")
	boolean existsByMaterialIgnoreCase(@Param("materialName") String materialName);

	/** Lấy các bản đính chính của bản gốc theo thời gian tạo giảm dần. */
	List<FarmLog> findByOriginalFarmLogId_IdOrderByCreatedAtDesc(UUID originalFarmLogId);

	/** Lấy nhật ký canh tác của danh sách lô sản xuất theo ngày thực hiện. */
	@Query("SELECT fl FROM FarmLog fl WHERE fl.productionLotId.id IN :productionLotIds ORDER BY fl.executedDate ASC")
	List<FarmLog> findByProductionLotId_IdInOrderByExecutedDateAsc(
			@Param("productionLotIds") List<UUID> productionLotIds);

	/** Lấy nhật ký canh tác của lô sản xuất theo loại hoạt động. */
	@Query("SELECT fl FROM FarmLog fl WHERE fl.productionLotId.id = :productionLotId AND fl.activityType = :activityType ORDER BY fl.executedDate ASC")
	List<FarmLog> findByProductionLotIdAndActivityType(
			@Param("productionLotId") UUID productionLotId,
			@Param("activityType") FarmActivityType activityType);

	/** Đếm số nhật ký canh tác theo từng tổ chức trong khoảng thời gian. */
	@Query("""
			SELECT pl.organization.organizationId, COUNT(fl)
			FROM FarmLog fl
			JOIN fl.productionLotId pl
			WHERE fl.createdAt BETWEEN :from AND :to
			GROUP BY pl.organization.organizationId
			""")
	List<Object[]> countFarmLogsGroupedByOrg(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	/** Lấy thời điểm ghi nhật ký mới nhất của từng tổ chức. */
	@Query("""
			SELECT pl.organization.organizationId, MAX(fl.createdAt)
			FROM FarmLog fl
			JOIN fl.productionLotId pl
			GROUP BY pl.organization.organizationId
			""")
	List<Object[]> maxFarmLogCreatedAtGroupedByOrg();
}
