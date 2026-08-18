package vn.nguongocso.integration.apikey.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;

@Repository
public interface PartnerApiKeyRepository extends JpaRepository<PartnerApiKey, UUID> {

    Optional<PartnerApiKey> findByKeyHash(String keyHash);

    Page<PartnerApiKey> findByOrganizationOrganizationId(UUID organizationId, Pageable pageable);

    Page<PartnerApiKey> findByOrganizationOrganizationIdAndStatus(UUID organizationId, PartnerApiKeyStatus status, Pageable pageable);

    @Query("SELECT k FROM PartnerApiKey k WHERE k.id = :id AND k.organization.organizationId = :organizationId")
    Optional<PartnerApiKey> findByIdAndOrganizationId(@Param("id") UUID id, @Param("organizationId") UUID organizationId);
}
