package vn.nguongocso.integration.apikey.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;

@Repository
public interface PartnerApiKeyRepository extends JpaRepository<PartnerApiKey, UUID> {

    Optional<PartnerApiKey> findByKeyHash(String keyHash);

    Page<PartnerApiKey> findByOrganizationOrganizationId(UUID organizationId, Pageable pageable);

    Page<PartnerApiKey> findByOrganizationOrganizationIdAndStatus(UUID organizationId, PartnerApiKeyStatus status, Pageable pageable);

    @Query("SELECT k FROM PartnerApiKey k WHERE k.id = :id AND k.organization.organizationId = :organizationId")
    Optional<PartnerApiKey> findByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    /**
     * Tìm khóa kèm khóa ghi bi quan để cộng dồn hạn mức an toàn khi nhiều yêu cầu nâng đồng thời.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM PartnerApiKey k WHERE k.id = :id AND k.organization.organizationId = :organizationId")
    Optional<PartnerApiKey> findByIdAndOrganizationIdForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    /**
     * Quét toàn bộ khóa theo trạng thái (phục vụ scheduler cảnh báo NCL-12-CN-005).
     */
    List<PartnerApiKey> findByStatus(PartnerApiKeyStatus status);

    /**
     * Tìm các khóa đối tác đủ điều kiện nhận thông báo Webhook thu hồi (NCL-12-CN-006).
     */
    @Query("""
            SELECT k FROM PartnerApiKey k
            WHERE k.id IN :ids
              AND k.status = vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus.ACTIVE
              AND k.expiresAt > :now
              AND k.webhookUrl IS NOT NULL
              AND k.webhookUrl <> ''
              AND k.isWebhookActive = true
              AND (k.isTest IS NULL OR k.isTest = false)
            """)
    java.util.List<PartnerApiKey> findEligibleWebhookKeys(
            @Param("ids") java.util.List<UUID> ids,
            @Param("now") java.time.LocalDateTime now);
}
