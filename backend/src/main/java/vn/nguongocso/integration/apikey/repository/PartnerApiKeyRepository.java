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
}
