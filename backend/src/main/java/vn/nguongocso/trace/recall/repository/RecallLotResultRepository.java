package vn.nguongocso.trace.recall.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.trace.recall.entity.RecallLotResult;

import java.util.List;
import java.util.UUID;

/** Repository quản lý kết quả xử lý lô trong vụ việc thu hồi (NCL-08-CN-012). */
@Repository
public interface RecallLotResultRepository extends JpaRepository<RecallLotResult, UUID> {

    List<RecallLotResult> findByRecallCaseId(UUID recallCaseId);

    boolean existsByRecallCaseIdAndShipmentId(UUID recallCaseId, UUID shipmentId);
}
