package vn.nguongocso.farm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.projection.FarmLogProjection;

/**
 * Repository thao tác dữ liệu nhật ký canh tác.
 */
public interface FarmLogRepository extends JpaRepository<FarmLog, UUID> {
	/**
	 * Lấy danh sách nhật ký canh tác của lô sản xuất theo phân trang.
	 *
	 * @param productionLot lô sản xuất
	 * @param pageable thông tin phân trang
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
	 * Lấy danh sách nhật ký canh tác thuộc về một lô sản xuất.
	 *
	 * @param productionLot lô sản xuất
	 * @return danh sách nhật ký canh tác
	 */
	List<FarmLog> findByProductionLotId(ProductionLot productionLot);
}
