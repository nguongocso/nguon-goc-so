package vn.nguongocso.trace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import vn.nguongocso.trace.entity.CodeRange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodeRangeRepository extends JpaRepository<CodeRange, UUID> {

    Optional<CodeRange> findByPrefix(String prefix);
    
    List<CodeRange> findAllByOrganizationOrganizationId(UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM CodeRange c
        WHERE c.id = :id
          AND c.organization.organizationId = :organizationId
    """)
    Optional<CodeRange> findByIdAndOrganizationIdForUpdate(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId
    );
}
