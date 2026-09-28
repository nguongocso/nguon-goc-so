package vn.nguongocso.alert.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.alert.entity.ActivityLogExportItem;

/** Truy cập các dòng snapshot của một export job. */
public interface ActivityLogExportItemRepository extends JpaRepository<ActivityLogExportItem, Long> {
    /*
     * Tìm kiếm và trả về các dòng snapshot của một export job, bắt đầu từ sau
     */
    List<ActivityLogExportItem> findByJobIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
            UUID jobId, Long sequenceNo, Pageable pageable);

    /*
     * Đóng snapshot bằng một câu lệnh tại database để request không phải tải toàn bộ dữ liệu lớn qua JVM trước khi trả
     * HTTP 202.
     */
    @Modifying
    @Query(value = """
            INSERT INTO activity_log_export_items (
                job_id, sequence_no, occurred_at, actor_name, actor_username, actor_role,
                action_type, object_type, object_identifier, before_value, after_value
            )
            SELECT
                :jobId,
                ROW_NUMBER() OVER (ORDER BY al.created_at ASC, al.id ASC) - 1,
                al.created_at,
                CASE WHEN al.full_name IS NOT NULL AND TRIM(al.full_name) <> ''
                    THEN al.full_name ELSE al.username END,
                al.username,
                al.actor_role,
                al.action,
                al.entity_type,
                al.entity_id,
                al.before_value,
                al.after_value
            FROM activity_logs al
            WHERE al.organization_id = :organizationId
                AND (:startAt IS NULL OR al.created_at >= :startAt)
                AND (:endAt IS NULL OR al.created_at <= :endAt)
                AND (:action IS NULL OR al.action = :action)
                AND (:actorName IS NULL OR LOWER(al.username) LIKE LOWER(CONCAT('%', :actorName, '%'))
                    OR LOWER(al.full_name) LIKE LOWER(CONCAT('%', :actorName, '%')))
                AND (:objectType IS NULL OR al.entity_type = :objectType)
            """, nativeQuery = true)
    int snapshotFromActivityLogs(
            @Param("jobId") String jobId,
            @Param("organizationId") String organizationId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("action") String action,
            @Param("actorName") String actorName,
            @Param("objectType") String objectType);
}
