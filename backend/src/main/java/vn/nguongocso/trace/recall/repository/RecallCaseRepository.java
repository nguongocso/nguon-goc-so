package vn.nguongocso.trace.recall.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository quản lý vụ việc thu hồi. */
@Repository
public interface RecallCaseRepository extends JpaRepository<RecallCase, UUID> {
    /** Lấy danh sách vụ việc thu hồi của một tổ chức theo thời gian tạo giảm dần. */
    List<RecallCase> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    /** Lấy danh sách vụ việc thu hồi của một tổ chức theo trạng thái. */
    List<RecallCase> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, RecallCaseStatus status);

    /** Lấy danh sách vụ việc thu hồi theo trạng thái. */
    List<RecallCase> findByStatusOrderByCreatedAtDesc(RecallCaseStatus status);

    /** Tìm vụ việc thu hồi theo ID và ID tổ chức. */
    Optional<RecallCase> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /** Tìm vụ việc thu hồi theo ID lô sản xuất và trạng thái. */
    Optional<RecallCase> findByProductionLotIdAndStatus(UUID productionLotId, RecallCaseStatus status);

    /** Tìm vụ việc thu hồi theo ID lô sản xuất. */
    Optional<RecallCase> findByProductionLotId(UUID productionLotId);

    /** Kiểm tra lô sản xuất đã có vụ việc thu hồi hay chưa. */
    boolean existsByProductionLotId(UUID productionLotId);

    /** Tìm các vụ việc thu hồi đã đóng có chứa một lô hàng. */
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

    /** Đếm số lượng vụ việc thu hồi theo tổ chức và trạng thái (TASK-AI-05). */
    long countByOrganizationIdAndStatus(UUID organizationId, RecallCaseStatus status);

    /** Đếm số lượng vụ việc thu hồi theo danh sách tổ chức và trạng thái (TASK-AI-05 & TASK-AI-07). */
    long countByOrganizationIdInAndStatus(Collection<UUID> organizationIds, RecallCaseStatus status);
}

