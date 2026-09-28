package vn.nguongocso.certification.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể chứng nhận (Certification).
 */
public interface CertificationRepository
                extends JpaRepository<Certification, UUID> {
        /**
         * Tìm chứng nhận theo ID và ID tổ chức sở hữu.
         */
        @Query("""
                        SELECT c FROM Certification c
                        WHERE c.id = :id AND c.organization.organizationId = :orgId
                        """)
        Optional<Certification> findByIdAndOrganizationId(
                        @Param("id") UUID id,
                        @Param("orgId") UUID organizationId);

        /**
         * Tìm tất cả chứng nhận hợp lệ để gắn cho lô thuộc tổ chức (loại bỏ EXPIRED và REJECTED; cho phép PENDING và
         * VERIFIED).
         */
        @Query("""
                        SELECT c FROM Certification c
                        WHERE c.organization.organizationId = :orgId
                        AND c.expiryDate >= :date
                        AND c.verificationStatus != vn.nguongocso.certification.enums.CertificationVerificationStatus.REJECTED
                        """)
        List<Certification> findByOrganizationIdAndExpiryDateAfter(
                        @Param("orgId") UUID organizationId,
                        @Param("date") LocalDate date);

        /**
         * Tìm tất cả chứng nhận theo ID tổ chức sở hữu.
         */
        @Query("""
                        SELECT c FROM Certification c
                        WHERE c.organization.organizationId = :orgId
                        """)
        List<Certification> findByOrganizationId(
                        @Param("orgId") UUID organizationId);

        /**
         * Tìm chứng nhận theo số hiệu.
         */
        Optional<Object> findByCode(
                        @NotBlank(message = "Số hiệu chứng nhận không được để trống") @Size(max = 50, message = "Số hiệu chứng nhận tối đa 50 ký tự") String code);

        /**
         * Tìm kiếm chứng nhận của tổ chức theo từ khoá (tên, số hiệu, cơ quan cấp) và trạng thái hiệu lực, có phân
         * trang.
         * Trạng thái tính theo ngày hết hạn (valid, expiring, expired).
         */
        @Query("""
                        SELECT c FROM Certification c
                        WHERE c.organization.organizationId = :orgId
                          AND (:keyword IS NULL
                               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(COALESCE(c.issuedBy, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
                          AND (:status IS NULL
                               OR (:status = 'expired' AND c.expiryDate < :today)
                               OR (:status = 'valid' AND c.expiryDate > :threshold)
                               OR (:status = 'expiring' AND c.expiryDate >= :today AND c.expiryDate <= :threshold))
                        """)
        Page<Certification> search(
                        @Param("orgId") UUID organizationId,
                        @Param("keyword") String keyword,
                        @Param("status") String status,
                        @Param("today") LocalDate today,
                        @Param("threshold") LocalDate threshold,
                        Pageable pageable);

        /**
         * Tìm kiếm và phân trang chứng nhận dành cho Quản trị viên nền tảng (VT-01).
         */
        @Query(value = """
                        SELECT c FROM Certification c
                        JOIN FETCH c.organization org
                        JOIN FETCH c.standard std
                        LEFT JOIN FETCH c.reviewedBy rb
                        WHERE (:status IS NULL OR c.verificationStatus = :status)
                          AND (:organizationId IS NULL OR org.organizationId = :organizationId)
                          AND (:keyword IS NULL
                               OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(COALESCE(c.issuedBy, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(std.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(org.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        """, countQuery = """
                        SELECT COUNT(c) FROM Certification c
                        JOIN c.organization org
                        JOIN c.standard std
                        WHERE (:status IS NULL OR c.verificationStatus = :status)
                          AND (:organizationId IS NULL OR org.organizationId = :organizationId)
                          AND (:keyword IS NULL
                               OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(COALESCE(c.issuedBy, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(std.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(org.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        """)
        Page<Certification> searchForAdmin(
                        @Param("status") CertificationVerificationStatus status,
                        @Param("organizationId") UUID organizationId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        /**
         * Cập nhật trạng thái xác thực một cách atomic (ngăn chặn race condition).
         */
        @Modifying
        @Query("""
                        UPDATE Certification c
                        SET c.verificationStatus = :newStatus,
                            c.reviewedBy = :reviewer,
                            c.reviewedAt = :reviewedAt,
                            c.reviewNote = :reviewNote,
                            c.rejectionReason = :rejectionReason,
                            c.updatedAt = :reviewedAt
                        WHERE c.id = :id
                          AND c.verificationStatus = :expectedStatus
                        """)
        int updateVerificationStatus(
                        @Param("id") UUID id,
                        @Param("expectedStatus") CertificationVerificationStatus expectedStatus,
                        @Param("newStatus") CertificationVerificationStatus newStatus,
                        @Param("reviewer") User reviewer,
                        @Param("reviewedAt") LocalDateTime reviewedAt,
                        @Param("reviewNote") String reviewNote,
                        @Param("rejectionReason") String rejectionReason);

        /** Tìm các chứng nhận sắp hết hiệu lực trong khoảng thời gian của một tổ chức (TASK-AI-05). */
        @Query("""
                        SELECT c FROM Certification c
                        JOIN FETCH c.standard std
                        WHERE c.organization.organizationId = :organizationId
                          AND c.expiryDate >= :today
                          AND c.expiryDate <= :threshold
                          AND c.verificationStatus != vn.nguongocso.certification.enums.CertificationVerificationStatus.REJECTED
                        ORDER BY c.expiryDate ASC
                        """)
        List<Certification> findExpiringCertifications(
                        @Param("organizationId") UUID organizationId,
                        @Param("today") LocalDate today,
                        @Param("threshold") LocalDate threshold);

        /** Tìm các chứng nhận sắp hết hiệu lực trong khoảng thời gian theo danh sách tổ chức (TASK-AI-05 & TASK-AI-07). */
        @Query("""
                        SELECT c FROM Certification c
                        JOIN FETCH c.standard std
                        WHERE c.organization.organizationId IN :organizationIds
                          AND c.expiryDate >= :today
                          AND c.expiryDate <= :threshold
                          AND c.verificationStatus != vn.nguongocso.certification.enums.CertificationVerificationStatus.REJECTED
                        ORDER BY c.expiryDate ASC
                        """)
        List<Certification> findExpiringCertificationsByOrgIds(
                        @Param("organizationIds") Collection<UUID> organizationIds,
                        @Param("today") LocalDate today,
                        @Param("threshold") LocalDate threshold);
}

