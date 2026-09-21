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
 * Repository cho bộ đếm lượt gọi theo ngày của khóa truy cập (NCL-12-CN-005).
 * <p>
 * Các câu lệnh cập nhật dùng JPQL (không dùng SQL native) để chạy được trên cả
 * MySQL (môi trường thật) và H2 MODE=MySQL (môi trường kiểm thử).
 */
@Repository
public interface PartnerApiKeyDailyUsageRepository extends JpaRepository<PartnerApiKeyDailyUsage, UUID> {

    /**
     * Tìm dòng usage của một khóa trong một ngày.
     */
    Optional<PartnerApiKeyDailyUsage> findByApiKeyIdAndUsageDate(UUID apiKeyId, LocalDate usageDate);

    /**
     * Tìm usage theo ngày của nhiều khóa (phục vụ danh sách khóa và cảnh báo tổng hợp).
     */
    List<PartnerApiKeyDailyUsage> findByApiKeyIdInAndUsageDate(Collection<UUID> apiKeyIds, LocalDate usageDate);

    /**
     * Lấy các dòng usage trong ngày chưa gửi cảnh báo (phục vụ job đối soát).
     */
    List<PartnerApiKeyDailyUsage> findByUsageDateAndWarningSentAtIsNull(LocalDate usageDate);

    /**
     * Cộng thêm 1 lượt gọi cho khóa trong ngày (nguyên tử ở tầng DB).
     *
     * @return số dòng được cập nhật (0 nghĩa là chưa có dòng cho khóa + ngày này)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PartnerApiKeyDailyUsage u "
            + "SET u.callCount = u.callCount + 1, u.updatedAt = :now "
            + "WHERE u.apiKeyId = :apiKeyId AND u.usageDate = :usageDate")
    int incrementCallCount(@Param("apiKeyId") UUID apiKeyId,
            @Param("usageDate") LocalDate usageDate,
            @Param("now") LocalDateTime now);

    /**
     * Giành quyền gửi cảnh báo hạn mức cho một dòng usage.
     *
     * @return 1 nếu giành được quyền gửi, 0 nếu đã có tiến trình gửi trước
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PartnerApiKeyDailyUsage u "
            + "SET u.warningSentAt = :now, u.updatedAt = :now "
            + "WHERE u.id = :id AND u.warningSentAt IS NULL")
    int claimWarning(@Param("id") UUID id, @Param("now") LocalDateTime now);

    /**
     * Nhả quyền gửi cảnh báo (dùng khi gửi thông báo thất bại để lần đối soát sau thử lại).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PartnerApiKeyDailyUsage u "
            + "SET u.warningSentAt = NULL, u.updatedAt = :now "
            + "WHERE u.id = :id")
    int releaseWarning(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
