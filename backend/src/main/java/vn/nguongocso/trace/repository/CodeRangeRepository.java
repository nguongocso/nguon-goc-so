package vn.nguongocso.trace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import vn.nguongocso.trace.entity.CodeRange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository quản lý dải mã truy xuất. */
public interface CodeRangeRepository extends JpaRepository<CodeRange, UUID> {
    /** Tìm dải mã truy xuất theo prefix. */
    Optional<CodeRange> findByPrefix(String prefix);

    /** Tìm dải mã truy xuất mới nhất của một tổ chức có khóa dòng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CodeRange> findFirstByOrganizationOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    /** Tìm dải mã truy xuất mới nhất của một tổ chức chế độ chỉ đọc. */
    Optional<CodeRange> findFirstReadOnlyByOrganizationOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    /** Hoàn trả hạn ngạch dải mã khi hủy tem. */
    @Modifying
    @Query("UPDATE CodeRange cr SET cr.usedCount = cr.usedCount - :count, cr.updatedAt = CURRENT_TIMESTAMP WHERE cr.id = :id AND cr.usedCount >= :count")
    int refundQuota(@Param("id") UUID id, @Param("count") Long count);

    /** Tìm tất cả dải mã của một tổ chức. */
    List<CodeRange> findByOrganizationOrganizationId(UUID organizationId);
}
