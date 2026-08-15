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

    /** Lấy danh sách yêu cầu theo trạng thái, phân trang. */
    Page<RecallRequest> findByStatus(RecallRequestStatus status, Pageable pageable);

    /** Kiểm tra một lô sản xuất đã có yêu cầu đang chờ duyệt hay chưa. */
    boolean existsByProductionLot_IdAndStatus(UUID productionLotId, RecallRequestStatus status);

    /** Lấy yêu cầu thu hồi đã được duyệt gần nhất của một lô sản xuất. */
    Optional<RecallRequest> findTopByProductionLot_IdAndStatusOrderByApprovedAtDesc(
            UUID productionLotId,
            RecallRequestStatus status);
}