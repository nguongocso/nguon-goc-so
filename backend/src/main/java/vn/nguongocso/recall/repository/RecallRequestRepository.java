package vn.nguongocso.recall.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.recall.entity.RecallRequest;
import vn.nguongocso.recall.enums.RecallRequestStatus;

/**
 * Repository quản lý các yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 */
@Repository
public interface RecallRequestRepository extends JpaRepository<RecallRequest, UUID> {

    Page<RecallRequest> findByProductionLot_Organization_OrganizationId(
            UUID organizationId, Pageable pageable);

    Page<RecallRequest> findByProductionLot_Organization_OrganizationIdAndStatus(
            UUID organizationId, RecallRequestStatus status, Pageable pageable);

    Optional<RecallRequest> findByIdAndProductionLot_Organization_OrganizationId(
            UUID id, UUID organizationId);

    /** Kiểm tra một lô sản xuất đã có yêu cầu đang chờ duyệt hay chưa. */
    boolean existsByProductionLot_IdAndStatus(UUID productionLotId, RecallRequestStatus status);

    boolean existsByShipment_IdAndStatus(UUID shipmentId, RecallRequestStatus status);

    boolean existsBySourceFeedback_IdAndStatus(UUID sourceFeedbackId, RecallRequestStatus status);

    Optional<RecallRequest> findTopBySourceFeedback_IdOrderByRequestedAtDesc(UUID sourceFeedbackId);

    /** Lấy yêu cầu thu hồi đã được duyệt gần nhất của một lô sản xuất. */
    Optional<RecallRequest> findTopByProductionLot_IdAndStatusOrderByApprovedAtDesc(
            UUID productionLotId,
            RecallRequestStatus status);

    Optional<RecallRequest> findTopByShipment_IdAndStatusOrderByApprovedAtDesc(
            UUID shipmentId,
            RecallRequestStatus status);

    /**
     * Tìm danh sách yêu cầu thu hồi theo danh sách lô sản xuất và trạng thái (NCL-07-CN-006).
     */
    @org.springframework.data.jpa.repository.Query("SELECT rr FROM RecallRequest rr WHERE rr.productionLot.id IN :lotIds AND rr.status IN :statuses")
    java.util.List<RecallRequest> findByProductionLotIdInAndStatusIn(
            @org.springframework.data.repository.query.Param("lotIds") java.util.Collection<UUID> lotIds,
            @org.springframework.data.repository.query.Param("statuses") java.util.Collection<RecallRequestStatus> statuses);
}

