package vn.nguongocso.auth.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.auth.entity.SuspiciousCase;
import vn.nguongocso.auth.enums.AnomalyStatus;

@Repository
public interface SuspiciousCaseRepository extends JpaRepository<SuspiciousCase, UUID> {

    Page<SuspiciousCase> findByOrganization_OrganizationIdOrderByLastDetectedAtDesc(
        UUID organizationId,
        Pageable pageable
    );

    Page<SuspiciousCase> findAllByOrderByLastDetectedAtDesc(Pageable pageable);

    List<SuspiciousCase> findByUser_UserIdOrderByLastDetectedAtDesc(UUID userId);

    List<SuspiciousCase> findByUser_UserIdAndStatusOrderByLastDetectedAtDesc(
        UUID userId,
        AnomalyStatus status
    );

    Page<SuspiciousCase> findByStatusOrderByLastDetectedAtDesc(
        AnomalyStatus status,
        Pageable pageable
    );

    List<SuspiciousCase> findByUser_UserIdAndLastDetectedAtAfter(
        UUID userId,
        OffsetDateTime threshold
    );

    boolean existsByUser_UserIdAndStatusAndLastDetectedAtAfter(
        UUID userId,
        AnomalyStatus status,
        OffsetDateTime threshold
    );
}
