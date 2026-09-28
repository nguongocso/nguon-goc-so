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
        /** Tìm yêu cầu xuất nhật ký theo ID và ID tổ chức. */
        Optional<ActivityLogExportJob> findByIdAndOrganizationId(UUID id, UUID organizationId);

        /** Tìm yêu cầu xuất nhật ký theo ID, token xử lý và trạng thái. */
        Optional<ActivityLogExportJob> findByIdAndProcessingTokenAndStatus(
                        UUID id, String processingToken, ActivityLogExportStatus status);

        /** Tìm danh sách ID các yêu cầu xuất nhật ký có thể phục hồi để xử lý lại. */
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

        /** Nhận quyền xử lý (claim lease) đối với yêu cầu xuất nhật ký. */
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

        /** Gia hạn thời gian giữ quyền xử lý (renew lease) cho yêu cầu xuất nhật ký. */
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

        /** Hủy bỏ quyền xử lý (release claim) đối với yêu cầu xuất nhật ký. */
        @Modifying
        @Transactional
        @Query("""
                        UPDATE ActivityLogExportJob job
                        SET job.processingToken = NULL, job.leaseExpiresAt = NULL
                        WHERE job.id = :jobId AND job.processingToken = :token
                        """)
        int releaseClaim(@Param("jobId") UUID jobId, @Param("token") String token);
}
