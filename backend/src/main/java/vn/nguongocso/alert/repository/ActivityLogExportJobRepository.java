package vn.nguongocso.alert.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.alert.entity.ActivityLogExportJob;
import vn.nguongocso.alert.enums.ActivityLogExportStatus;

/** Truy cập yêu cầu xuất nhật ký hoạt động theo phạm vi tổ chức. */
public interface ActivityLogExportJobRepository extends JpaRepository<ActivityLogExportJob, UUID> {
    Optional<ActivityLogExportJob> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ActivityLogExportJob> findByIdAndProcessingTokenAndStatus(
            UUID id, String processingToken, ActivityLogExportStatus status);

    @Query("""
            SELECT job.id FROM ActivityLogExportJob job
            WHERE job.status = :status
              AND (job.processingToken IS NULL OR job.leaseExpiresAt < :now)
            ORDER BY job.createdAt ASC
            """)
    List<UUID> findRecoverableJobIds(
            @Param("status") ActivityLogExportStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            UPDATE ActivityLogExportJob job
            SET job.processingToken = :token, job.leaseExpiresAt = :leaseExpiresAt
            WHERE job.id = :jobId
              AND job.status = :status
              AND (job.processingToken IS NULL OR job.leaseExpiresAt < :now)
            """)
    int claim(
            @Param("jobId") UUID jobId,
            @Param("token") String token,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt,
            @Param("now") LocalDateTime now,
            @Param("status") ActivityLogExportStatus status);

    @Modifying
    @Transactional
    @Query("""
            UPDATE ActivityLogExportJob job
            SET job.leaseExpiresAt = :leaseExpiresAt
            WHERE job.id = :jobId
              AND job.processingToken = :token
              AND job.status = :status
            """)
    int renewLease(
            @Param("jobId") UUID jobId,
            @Param("token") String token,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt,
            @Param("status") ActivityLogExportStatus status);

    @Modifying
    @Transactional
    @Query("""
            UPDATE ActivityLogExportJob job
            SET job.processingToken = NULL, job.leaseExpiresAt = NULL
            WHERE job.id = :jobId AND job.processingToken = :token
            """)
    int releaseClaim(@Param("jobId") UUID jobId, @Param("token") String token);
}
