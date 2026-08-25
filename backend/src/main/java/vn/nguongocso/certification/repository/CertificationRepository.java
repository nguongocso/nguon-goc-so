package vn.nguongocso.certification.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nguongocso.certification.entity.Certification;

import java.time.LocalDate;
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
     * Tìm tất cả chứng nhận theo ID tổ chức.
     *
     * @param organizationId ID của tổ chức.
     * @return Danh sách chứng nhận thuộc tổ chức.
     */
    @Query("SELECT c FROM Certification c WHERE c.organization.organizationId = :orgId AND c.expiryDate > :date")
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
}