package vn.nguongocso.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.report.entity.ReportAccessLog;

import java.util.UUID;

/**
 * Repository cho entity ReportAccessLog.
 *
 * @author Triệu Văn Đại
 */
@Repository
public interface ReportAccessLogRepository extends JpaRepository<ReportAccessLog, UUID> {

}
