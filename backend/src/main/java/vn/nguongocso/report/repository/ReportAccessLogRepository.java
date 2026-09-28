package vn.nguongocso.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.nguongocso.report.entity.ReportAccessLog;

import java.util.UUID;

/** Repository quản lý nhật ký truy cập báo cáo. */
@Repository
public interface ReportAccessLogRepository extends JpaRepository<ReportAccessLog, UUID> {

}
