package vn.nguongocso.farm.repository;

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
	/**
	 * Lấy danh sách nhật ký canh tác của lô sản xuất theo phân trang.
	 *
	 * @param productionLot lô sản xuất
	 * @param pageable      thông tin phân trang
	 * @return danh sách nhật ký canh tác
	 */
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

	/**
	 * Lấy danh sách nhật ký canh tác của lô sản xuất theo ID của lô sản xuất, sắp
	 * xếp theo ngày thực hiện tăng dần.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @return danh sách nhật ký canh tác
	 */
	Page<FarmLog> findByProductionLotId(ProductionLot productionLot, Pageable pageable);

	/**
	 * Lấy danh sách nhật ký canh tác của lô sản xuất theo ID của lô sản xuất, sắp
	 * xếp theo ngày thực hiện tăng dần.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @return danh sách nhật ký canh tác
	 */
	List<FarmLog> findByProductionLotId_IdOrderByExecutedDateAsc(UUID productionLotId);

	/**
	 * Kiểm tra xem có tồn tại nhật ký canh tác nào liên quan đến lô sản xuất hay
	 * không.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @return true nếu tồn tại, false nếu không tồn tại
	 */
	@Query("SELECT COUNT(fl) > 0 FROM FarmLog fl " +
			"WHERE fl.productionLotId.id = :productionLotId")
	boolean existsByProductionLotId(@Param("productionLotId") UUID productionLotId);

	/**
	 * Kiểm tra xem vật tư có tên cho trước đã từng được dùng trong nhật ký canh tác hay chưa.
	 */
	@Query("SELECT COUNT(fl) > 0 FROM FarmLog fl WHERE LOWER(TRIM(fl.material)) = LOWER(TRIM(:materialName))")
	boolean existsByMaterialIgnoreCase(@Param("materialName") String materialName);

	/**
	 * NCL-03-CN-006: lấy các bản đính chính liên kết tới một bản gốc
	 * nhật ký canh tác, sắp xếp theo thời gian tạo giảm dần (mới nhất trước).
	 *
	 * @param originalFarmLogId ID của bản gốc
	 * @return danh sách bản đính chính
	 */
	List<FarmLog> findByOriginalFarmLogId_IdOrderByCreatedAtDesc(UUID originalFarmLogId);

	/**
	 * Lấy danh sách nhật ký canh tác của các lô sản xuất theo danh sách ID của lô
	 * sản xuất, sắp xếp theo ngày thực hiện tăng dần.
	 *
	 * @param productionLotIds danh sách ID của các lô sản xuất
	 * @return danh sách nhật ký canh tác
	 */
	@Query("SELECT fl FROM FarmLog fl WHERE fl.productionLotId.id IN :productionLotIds ORDER BY fl.executedDate ASC")
	List<FarmLog> findByProductionLotId_IdInOrderByExecutedDateAsc(
			@Param("productionLotIds") List<UUID> productionLotIds);

	/**
	 * Lấy toàn bộ nhật ký canh tác của một lô sản xuất theo loại hoạt động.
	 *
	 * @param productionLotId ID của lô sản xuất
	 * @param activityType    loại hoạt động (ví dụ: PESTICIDE)
	 * @return danh sách nhật ký canh tác
	 */
	@Query("SELECT fl FROM FarmLog fl WHERE fl.productionLotId.id = :productionLotId AND fl.activityType = :activityType ORDER BY fl.executedDate ASC")
	List<FarmLog> findByProductionLotIdAndActivityType(
			@Param("productionLotId") UUID productionLotId,
			@Param("activityType") FarmActivityType activityType);
}
