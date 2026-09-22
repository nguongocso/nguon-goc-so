package vn.nguongocso.integration.apikey.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.nguongocso.integration.apikey.entity.PartnerApiKeyDailyUsage;

/**
 * Repository cho bộ đếm lượt gọi theo ngày của khóa truy cập.
*/
@Repository
public interface PartnerApiKeyDailyUsageRepository extends JpaRepository<PartnerApiKeyDailyUsage, UUID> {
    /** Tìm bản ghi theo khóa và ngày sử dụng. */
    Optional<PartnerApiKeyDailyUsage> findByApiKeyIdAndUsageDate(UUID apiKeyId, LocalDate usageDate);

    /** Tìm các bản ghi theo danh sách khóa và ngày sử dụng. */
    List<PartnerApiKeyDailyUsage> findByApiKeyIdInAndUsageDate(Collection<UUID> apiKeyIds, LocalDate usageDate);

    /** Tìm các bản ghi chưa gửi cảnh báo theo ngày. */
    List<PartnerApiKeyDailyUsage> findByUsageDateAndWarningSentAtIsNull(LocalDate usageDate);

    /** Cộng thêm một lượt gọi cho khóa trong ngày. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PartnerApiKeyDailyUsage u "
            + "SET u.callCount = u.callCount + 1, u.updatedAt = :now "
            + "WHERE u.apiKeyId = :apiKeyId AND u.usageDate = :usageDate")
    int incrementCallCount(@Param("apiKeyId") UUID apiKeyId,
            @Param("usageDate") LocalDate usageDate,
            @Param("now") LocalDateTime now);

    /** Giành quyền gửi cảnh báo hạn mức cho một dòng usage. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PartnerApiKeyDailyUsage u "
            + "SET u.warningSentAt = :now, u.updatedAt = :now "
            + "WHERE u.id = :id AND u.warningSentAt IS NULL")
    int claimWarning(@Param("id") UUID id, @Param("now") LocalDateTime now);

    /** Nhả quyền gửi cảnh báo để lần đối soát sau thử lại. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PartnerApiKeyDailyUsage u "
            + "SET u.warningSentAt = NULL, u.updatedAt = :now "
            + "WHERE u.id = :id")
    int releaseWarning(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
