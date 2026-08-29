package vn.nguongocso.trace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import vn.nguongocso.trace.entity.CodeRange;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể CodeRange.
 */
public interface CodeRangeRepository extends JpaRepository<CodeRange, UUID> {
    /**
     * Tìm một dải mã truy xuất theo prefix.
     *
     * @param prefix prefix của dải mã
     * @return Optional chứa CodeRange nếu tìm thấy, ngược lại là Optional.empty()
     */
    Optional<CodeRange> findByPrefix(String prefix);

    /**
     * Tìm dải mã truy xuất mới nhất (theo createdAt) của một tổ chức.
     *
     * <p>
     * Bảng {@code code_ranges} hiện tại KHÔNG có ràng buộc UNIQUE trên
     * {@code organization_id} nên một tổ chức có thể có nhiều dòng. Dùng
     * {@code findFirst...OrderByCreatedAtDesc} để chọn một kết quả xác định
     * (dải mã mới nhất), tránh {@code NonUniqueResultException}. Nên thêm ràng
     * buộc UNIQUE ở database sau khi dữ liệu đã được làm sạch.
     * </p>
     *
     * @param organizationId ID của tổ chức
     * @return Optional chứa CodeRange mới nhất nếu tìm thấy, ngược lại là Optional.empty()
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CodeRange> findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    /**
     * Tìm dải mã truy xuất mới nhất (theo createdAt) của một tổ chức,
     * KHÔNG khoá dòng (không sinh {@code FOR UPDATE}).
     *
     * <p>
     * Dành cho các luồng chỉ đọc (ví dụ: hiển thị số lượng mã truy xuất còn
     * lại). MySQL không cho phép thực thi {@code SELECT ... FOR UPDATE} bên
     * trong transaction {@code READ ONLY} (lỗi 1792 / SQLState 25006), nên các
     * luồng chạy trong {@code @Transactional(readOnly = true)} phải dùng phương
     * thức này thay cho
     * {@link #findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(UUID)}
     * (có khoá {@code PESSIMISTIC_WRITE} dành cho luồng ghi).
     * </p>
     *
     * @param organizationId ID của tổ chức
     * @return Optional chứa CodeRange mới nhất nếu tìm thấy, ngược lại là Optional.empty()
     */
    Optional<CodeRange> findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CodeRange cr SET cr.usedCount = cr.usedCount - :count, cr.updatedAt = CURRENT_TIMESTAMP WHERE cr.id = :id AND cr.usedCount >= :count")
    int refundQuota(@Param("id") UUID id, @Param("count") Long count);
}
