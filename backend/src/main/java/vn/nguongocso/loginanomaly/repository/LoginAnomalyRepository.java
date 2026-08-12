package vn.nguongocso.loginanomaly.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.loginanomaly.entity.LoginAnomaly;
import vn.nguongocso.loginanomaly.enums.LoginAnomalySeverity;

/** Repository cho thực thể LoginAnomaly. */
public interface LoginAnomalyRepository extends JpaRepository<LoginAnomaly, UUID> {

    /**
     * Lọc danh sách bất thường kèm trạng thái tài khoản hiện tại.
     *
     * <p>
     * Trạng thái tài khoản được đọc trực tiếp từ bảng {@code users} tại thời
     * điểm truy vấn. Bản ghi không còn liên kết tới người dùng (user bị xóa)
     * sẽ không khớp khi lọc theo {@code accountStatus}.
     * </p>
     *
     * @param organizationId giới hạn theo tổ chức (null = toàn nền tảng)
     * @param severity       mức độ bất thường (null = tất cả)
     * @param accountStatus  trạng thái tài khoản (null = tất cả)
     * @param keyword        tìm theo username, họ tên hoặc IP (null = bỏ qua)
     * @param from           mốc thời gian bắt đầu (null = bỏ qua)
     * @param to             mốc thời gian kết thúc, không bao gồm (null = bỏ qua)
     * @param pageable       phân trang
     * @return trang kết quả
     */
    @Query(value = """
                SELECT a
                FROM LoginAnomaly a
                LEFT JOIN User u ON u.userId = a.userId
                WHERE (:organizationId IS NULL OR a.organizationId = :organizationId)
                  AND (:severity IS NULL OR a.severity = :severity)
                  AND (:accountStatus IS NULL OR u.status = :accountStatus)
                  AND (:keyword IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                       OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                       OR a.ipAddress LIKE CONCAT('%', :keyword, '%'))
                  AND (:from IS NULL OR a.loginAt >= :from)
                  AND (:to IS NULL OR a.loginAt < :to)
                ORDER BY a.loginAt DESC
            """,
            countQuery = """
                SELECT COUNT(a)
                FROM LoginAnomaly a
                LEFT JOIN User u ON u.userId = a.userId
                WHERE (:organizationId IS NULL OR a.organizationId = :organizationId)
                  AND (:severity IS NULL OR a.severity = :severity)
                  AND (:accountStatus IS NULL OR u.status = :accountStatus)
                  AND (:keyword IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                       OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                       OR a.ipAddress LIKE CONCAT('%', :keyword, '%'))
                  AND (:from IS NULL OR a.loginAt >= :from)
                  AND (:to IS NULL OR a.loginAt < :to)
            """)
    Page<LoginAnomaly> findWithFilters(
            @Param("organizationId") UUID organizationId,
            @Param("severity") LoginAnomalySeverity severity,
            @Param("accountStatus") UserStatus accountStatus,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
