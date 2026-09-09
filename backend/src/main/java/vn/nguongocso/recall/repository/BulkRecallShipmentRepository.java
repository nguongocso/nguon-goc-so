package vn.nguongocso.recall.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.recall.entity.BulkRecallShipment;

/**
 * Repository quản lý chi tiết lô hàng trong yêu cầu thu hồi hàng loạt (NCL-08-CN-011).
 */
@Repository
public interface BulkRecallShipmentRepository extends JpaRepository<BulkRecallShipment, UUID> {

    /**
     * Lấy danh sách chi tiết theo yêu cầu thu hồi.
     */
    List<BulkRecallShipment> findByBulkRecallRequestId(UUID bulkRecallRequestId);

    /**
     * Lấy danh sách chi tiết theo yêu cầu thu hồi và trạng thái included.
     */
    List<BulkRecallShipment> findByBulkRecallRequestIdAndIncluded(UUID bulkRecallRequestId, boolean included);

    /**
     * Xóa tất cả chi tiết theo yêu cầu thu hồi.
     */
    void deleteByBulkRecallRequestId(UUID bulkRecallRequestId);
}
