package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.entity.InspectionResultEntryLink;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository quản lý vòng đời liên kết nhập kết quả kiểm nghiệm.
 */
public interface InspectionResultEntryLinkRepository
      extends JpaRepository<InspectionResultEntryLink, UUID> {
   /**
    * Tìm liên kết nhập kết quả theo mã băm token.
    */
   Optional<InspectionResultEntryLink> findByTokenHash(String tokenHash);

   /**
    * Tìm liên kết nhập kết quả mới nhất theo ID yêu cầu kiểm nghiệm.
    */
   Optional<InspectionResultEntryLink> findFirstByInspectionRequest_IdOrderByCreatedAtDesc(
         UUID inspectionRequestId);

   /**
    * Lấy danh sách liên kết nhập kết quả theo ID yêu cầu kiểm nghiệm và trạng
    * thái.
    */
   List<InspectionResultEntryLink> findByInspectionRequest_IdAndStatus(
         UUID inspectionRequestId,
         InspectionResultEntryLinkStatus status);

   /**
    * Đếm số lượng liên kết nhập kết quả theo ID yêu cầu kiểm nghiệm và trạng thái.
    */
   long countByInspectionRequest_IdAndStatus(
         UUID inspectionRequestId,
         InspectionResultEntryLinkStatus status);

   /**
    * Lấy toàn bộ danh sách liên kết nhập kết quả theo ID yêu cầu kiểm nghiệm.
    */
   List<InspectionResultEntryLink> findByInspectionRequest_Id(
         UUID inspectionRequestId);

   /**
    * Cập nhật tiêu thụ liên kết nhập kết quả đang hoạt động.
    */
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

   /**
    * Thu hồi tất cả liên kết đang hoạt động của một yêu cầu kiểm nghiệm.
    */
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
         @Param("revokedBy") User revokedBy);
}
