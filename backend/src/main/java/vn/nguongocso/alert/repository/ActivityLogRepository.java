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

/**
 * Repository cho thực thể ActivityLog.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID>, JpaSpecificationExecutor<ActivityLog> {
    // Kế thừa JpaSpecificationExecutor nhằm hỗ trợ tìm kiếm động linh hoạt

    /**
     * Đếm số người dùng hoạt động (userId phân biệt) của từng tổ chức trong
     * khoảng thời gian (NCL-07-CN-008). "Hoạt động" nghĩa là có bản ghi trong
     * activity_logs, không phải tổng số tài khoản thành viên.
     *
     * @param from mốc bắt đầu khoảng thời gian
     * @param to   mốc kết thúc khoảng thời gian
     * @return danh sách [organizationId, số user phân biệt]
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
     * Lấy thời điểm hoạt động mới nhất của từng tổ chức (NCL-07-CN-008,
     * phục vụ tính lastActivityAt).
     *
     * @return danh sách [organizationId, createdAt lớn nhất]
     */
    @Query("""
            SELECT a.organizationId, MAX(a.createdAt)
            FROM ActivityLog a
            GROUP BY a.organizationId
            """)
    List<Object[]> maxActivityAtGroupedByOrg();
}
