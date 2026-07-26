package vn.nguongocso.trace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import vn.nguongocso.trace.entity.CodeRange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodeRangeRepository extends JpaRepository<CodeRange, UUID> {

    Optional<CodeRange> findByPrefix(String prefix);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CodeRange> findByOrganizationOrganizationId(UUID organizationId);

    List<CodeRange> findAllByOrganizationOrganizationId(UUID organizationId);
}
