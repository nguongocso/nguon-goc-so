package vn.nguongocso.trace.recall.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.trace.recall.entity.RecallLotResult;

import java.util.List;
import java.util.UUID;

/** Repository quản lý kết quả xử lý lô trong vụ việc thu hồi. */
@Repository
public interface RecallLotResultRepository extends JpaRepository<RecallLotResult, UUID> {
    /** Lấy danh sách kết quả xử lý lô theo ID vụ việc thu hồi. */
    List<RecallLotResult> findByRecallCaseId(UUID recallCaseId);

    /** Kiểm tra xem lô hàng đã có kết quả xử lý trong vụ việc thu hồi hay chưa. */
    boolean existsByRecallCaseIdAndShipmentId(UUID recallCaseId, UUID shipmentId);
}
