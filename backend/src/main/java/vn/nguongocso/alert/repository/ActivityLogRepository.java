package vn.nguongocso.alert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.alert.entity.ActivityLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Repository truy cập nhật ký hoạt động. */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID>, JpaSpecificationExecutor<ActivityLog> {
        /**
         * Đếm số người dùng hoạt động (userId phân biệt) của từng tổ chức trong khoảng thời gian (NCL-07-CN-008).
         */
        @Query("""
                        SELECT a.organizationId, COUNT(DISTINCT a.userId)
                        FROM ActivityLog a
                        WHERE a.createdAt BETWEEN :from AND :to
                        GROUP BY a.organizationId
                        """)
        List<Object[]> countDistinctUsersGroupedByOrg(
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        /**
         * Lấy thời điểm hoạt động mới nhất của từng tổ chức (NCL-07-CN-008, phục vụ tính lastActivityAt).
         */
        @Query("""
                        SELECT a.organizationId, MAX(a.createdAt)
                        FROM ActivityLog a
                        GROUP BY a.organizationId
                        """)
        List<Object[]> maxActivityAtGroupedByOrg();
}
