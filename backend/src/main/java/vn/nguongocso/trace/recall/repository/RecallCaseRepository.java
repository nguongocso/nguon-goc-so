package vn.nguongocso.trace.recall.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository quản lý vụ việc thu hồi (NCL-08-CN-012).
 */
@Repository
public interface RecallCaseRepository extends JpaRepository<RecallCase, UUID> {

    List<RecallCase> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<RecallCase> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<RecallCase> findByProductionLotIdAndStatus(UUID productionLotId, RecallCaseStatus status);

    Optional<RecallCase> findByProductionLotId(UUID productionLotId);

    boolean existsByProductionLotId(UUID productionLotId);

    /**
     * Tìm các vụ việc đã đóng có chứa một lô hàng nhất định, mới đóng trước.
     * Dùng cho nội dung cảnh báo công khai (QTN-09).
     */
    @Query("""
            SELECT DISTINCT rc
            FROM RecallCase rc
            JOIN RecallLotResult lr ON lr.recallCase = rc
            WHERE lr.shipment.id = :shipmentId
              AND rc.status = :status
            ORDER BY rc.closedAt DESC
            """)
    List<RecallCase> findClosedByShipmentId(@Param("shipmentId") UUID shipmentId,
                                            @Param("status") RecallCaseStatus status);
}
