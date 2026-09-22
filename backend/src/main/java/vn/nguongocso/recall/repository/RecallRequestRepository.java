package vn.nguongocso.recall.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.nguongocso.recall.entity.RecallRequest;
import vn.nguongocso.recall.enums.RecallRequestStatus;

/** Repository quản lý các yêu cầu thu hồi lô sản xuất. */
@Repository
public interface RecallRequestRepository extends JpaRepository<RecallRequest, UUID> {
    /** Lấy danh sách yêu cầu thu hồi theo tổ chức có phân trang. */
    Page<RecallRequest> findByProductionLot_Organization_OrganizationId(
            UUID organizationId, Pageable pageable);

    /** Lấy danh sách yêu cầu thu hồi theo tổ chức và trạng thái có phân trang. */
    Page<RecallRequest> findByProductionLot_Organization_OrganizationIdAndStatus(
            UUID organizationId, RecallRequestStatus status, Pageable pageable);

    /** Tìm yêu cầu thu hồi theo ID và ID tổ chức. */
    Optional<RecallRequest> findByIdAndProductionLot_Organization_OrganizationId(
            UUID id, UUID organizationId);

    /** Kiểm tra một lô sản xuất đã có yêu cầu đang chờ duyệt hay chưa. */
    boolean existsByProductionLot_IdAndStatus(UUID productionLotId, RecallRequestStatus status);

    /** Kiểm tra lô hàng đã có yêu cầu thu hồi theo trạng thái hay chưa. */
    boolean existsByShipment_IdAndStatus(UUID shipmentId, RecallRequestStatus status);

    /** Kiểm tra phản hồi nguồn đã có yêu cầu thu hồi theo trạng thái hay chưa. */
    boolean existsBySourceFeedback_IdAndStatus(UUID sourceFeedbackId, RecallRequestStatus status);

    /** Tìm yêu cầu thu hồi gần nhất theo ID phản hồi nguồn. */
    Optional<RecallRequest> findTopBySourceFeedback_IdOrderByRequestedAtDesc(UUID sourceFeedbackId);

    /** Lấy yêu cầu thu hồi đã được duyệt gần nhất của một lô sản xuất. */
    Optional<RecallRequest> findTopByProductionLot_IdAndStatusOrderByApprovedAtDesc(
            UUID productionLotId,
            RecallRequestStatus status);

    /** Lấy yêu cầu thu hồi đã được duyệt gần nhất của một lô hàng. */
    Optional<RecallRequest> findTopByShipment_IdAndStatusOrderByApprovedAtDesc(
            UUID shipmentId,
            RecallRequestStatus status);

    /** Tìm danh sách yêu cầu thu hồi theo danh sách lô sản xuất và trạng thái. */
    @Query("SELECT rr FROM RecallRequest rr WHERE rr.productionLot.id IN :lotIds AND rr.status IN :statuses")
    List<RecallRequest> findByProductionLotIdInAndStatusIn(
            @Param("lotIds") Collection<UUID> lotIds,
            @Param("statuses") Collection<RecallRequestStatus> statuses);
}
