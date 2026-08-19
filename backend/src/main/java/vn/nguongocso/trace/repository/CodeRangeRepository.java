package vn.nguongocso.trace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

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
}
