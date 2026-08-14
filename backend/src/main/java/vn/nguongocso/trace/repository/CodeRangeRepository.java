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
     * Tìm một dải mã truy xuất theo tổ chức.
     *
     * @param organizationId ID của tổ chức
     * @return Optional chứa CodeRange nếu tìm thấy, ngược lại là Optional.empty()
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CodeRange> findByOrganizationOrganizationId(UUID organizationId);
}
