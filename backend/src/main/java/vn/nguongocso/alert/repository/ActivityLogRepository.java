package vn.nguongocso.alert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.nguongocso.alert.entity.ActivityLog;

import java.util.UUID;

/**
 * Repository cho thực thể ActivityLog.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID>, JpaSpecificationExecutor<ActivityLog> {
    // Kế thừa JpaSpecificationExecutor nhằm hỗ trợ tìm kiếm động linh hoạt
}
