package vn.nguongocso.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.event.entity.FailedEventLog;

import java.util.UUID;

/**
 * Repository cho entity FailedEventLog.
 *
 * @author Triệu Văn Đại
 */
@Repository
public interface FailedEventLogRepository extends JpaRepository<FailedEventLog, UUID> {
    /**
     * Lấy danh sách nhật ký sự kiện bị chặn, sắp xếp theo thời gian thử lại giảm
     * dần.
     *
     * @param pageable thông tin phân trang
     * @return danh sách nhật ký sự kiện bị chặn
     */
    Page<FailedEventLog> findAllByOrderByAttemptedAtDesc(Pageable pageable);
}
