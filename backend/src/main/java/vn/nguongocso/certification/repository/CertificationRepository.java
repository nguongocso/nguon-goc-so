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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể Certification.
 */
public interface CertificationRepository extends JpaRepository<Certification, UUID> {
    /**
     * Tìm chứng nhận theo ID và ID tổ chức.
     *
     * @param id             ID của chứng nhận.
     * @param organizationId ID của tổ chức.
     * @return Optional chứa chứng nhận nếu tìm thấy, ngược lại là Optional rỗng.
     */
    @Query("SELECT c FROM Certification c WHERE c.id = :id AND c.organization.organizationId = :orgId")
    Optional<Certification> findByIdAndOrganizationId(@Param("id") UUID id, @Param("orgId") UUID organizationId);

    /**
     * Tìm tất cả chứng nhận hợp lệ để gắn cho lô thuộc tổ chức (loại bỏ EXPIRED và REJECTED; cho phép PENDING và VERIFIED).
     *
     * @param organizationId ID của tổ chức.
     * @param date           Ngày hiện tại để so sánh hạn sử dụng.
     * @return Danh sách chứng nhận có thể gắn cho lô.
     */
    @Query("SELECT c FROM Certification c WHERE c.organization.organizationId = :orgId AND c.expiryDate > :date AND c.verificationStatus != vn.nguongocso.certification.enums.CertificationVerificationStatus.REJECTED")
    List<Certification> findByOrganizationIdAndExpiryDateAfter(@Param("orgId") UUID organizationId,
            @Param("date") LocalDate date);

    /**
     * Tìm tất cả chứng nhận theo ID tổ chức.
     *
     * @param organizationId ID của tổ chức.
     * @return Danh sách chứng nhận thuộc tổ chức.
     */
    @Query("SELECT c FROM Certification c WHERE c.organization.organizationId = :orgId")
    List<Certification> findByOrganizationId(@Param("orgId") UUID organizationId);

    /**
     * Tìm chứng nhận theo số hiệu.
     *
     * @param code Số hiệu chứng nhận.
     * @return Optional chứa chứng nhận nếu tìm thấy, ngược lại là Optional rỗng.
     */
    Optional<Object> findByCode(
            @NotBlank(message = "Số hiệu chứng nhận không được để trống") @Size(max = 50, message = "Số hiệu chứng nhận tối đa 50 ký tự") String code);

    /**
     * Tìm kiếm chứng nhận của tổ chức theo từ khoá (tên, số hiệu, cơ quan cấp)
     * và trạng thái hiệu lực, có phân trang.
     *
     * <p>Trạng thái tính theo ngày hết hạn, ba nhóm rời rạc khớp với badge
     * hiển thị:
     * <ul>
     *   <li>{@code valid}: {@code expiryDate > threshold} (còn hiệu lực quá 30 ngày)</li>
     *   <li>{@code expiring}: {@code today <= expiryDate <= threshold}</li>
     *   <li>{@code expired}: {@code expiryDate < today}</li>
     * </ul></p>
     *
     * @param organizationId ID của tổ chức hiện tại.
     * @param keyword        từ khoá tìm kiếm (null/empty để bỏ qua).
     * @param status         valid | expiring | expired (null để bỏ qua).
     * @param today          ngày hiện tại.
     * @param threshold      ngưỡng cảnh báo sắp hết hạn.
     * @param pageable       thông tin phân trang và sắp xếp.
     * @return trang dữ liệu chứng nhận.
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
     *
     * @param status         Trạng thái xác thực (PENDING, VERIFIED, REJECTED) hoặc null để lấy tất cả.
     * @param organizationId ID tổ chức (nếu muốn lọc theo tổ chức) hoặc null để lấy tất cả.
     * @param keyword        Từ khóa tìm kiếm (số hiệu, tên chứng nhận, cơ quan cấp, tên tiêu chuẩn, tên tổ chức).
     * @param pageable       Thông tin phân trang và sắp xếp.
     * @return Trang dữ liệu chứng nhận kèm thông tin liên quan.
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
            """,
            countQuery = """
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
     *
     * @param id              ID chứng nhận.
     * @param expectedStatus  Trạng thái kỳ vọng hiện tại (thường là PENDING).
     * @param newStatus       Trạng thái mới (VERIFIED hoặc REJECTED).
     * @param reviewer        Quản trị viên thực hiện.
     * @param reviewedAt      Thời điểm thực hiện.
     * @param reviewNote      Ghi chú xác thực (nếu duyệt).
     * @param rejectionReason Lý do từ chối (nếu từ chối).
     * @return Số dòng cập nhật (1 nếu thành công, 0 nếu bản ghi không còn ở expectedStatus).
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
}