package vn.nguongocso.certification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.certification.entity.InspectionResultEntryLink;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;

/**
 * Truy cập dữ liệu vòng đời liên kết nhập kết quả kiểm nghiệm.
 */
public interface InspectionResultEntryLinkRepository
        extends JpaRepository<InspectionResultEntryLink, UUID> {

    Optional<InspectionResultEntryLink> findByTokenHash(String tokenHash);

    Optional<InspectionResultEntryLink> findFirstByInspectionRequest_IdOrderByCreatedAtDesc(
            UUID inspectionRequestId);

    List<InspectionResultEntryLink> findByInspectionRequest_IdAndStatus(
            UUID inspectionRequestId,
            InspectionResultEntryLinkStatus status);

    long countByInspectionRequest_IdAndStatus(
            UUID inspectionRequestId,
            InspectionResultEntryLinkStatus status);

    List<InspectionResultEntryLink> findByInspectionRequest_Id(UUID inspectionRequestId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InspectionResultEntryLink link
               SET link.status = :usedStatus,
                   link.usedAt = :usedAt,
                   link.usedIp = :usedIp,
                   link.usedUserAgent = :usedUserAgent
             WHERE link.id = :linkId
               AND link.status = :activeStatus
               AND link.expiresAt > :usedAt
            """)
    int consumeActiveLink(
            @Param("linkId") UUID linkId,
            @Param("activeStatus") InspectionResultEntryLinkStatus activeStatus,
            @Param("usedStatus") InspectionResultEntryLinkStatus usedStatus,
            @Param("usedAt") LocalDateTime usedAt,
            @Param("usedIp") String usedIp,
            @Param("usedUserAgent") String usedUserAgent);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InspectionResultEntryLink link
               SET link.status = :revokedStatus,
                   link.revokedAt = :revokedAt,
                   link.revokedBy = :revokedBy
             WHERE link.inspectionRequest.id = :requestId
               AND link.status = :activeStatus
            """)
    int revokeActiveLinksByRequestId(
            @Param("requestId") UUID requestId,
            @Param("activeStatus") InspectionResultEntryLinkStatus activeStatus,
            @Param("revokedStatus") InspectionResultEntryLinkStatus revokedStatus,
            @Param("revokedAt") LocalDateTime revokedAt,
            @Param("revokedBy") vn.nguongocso.auth.entity.User revokedBy);
}
