package vn.nguongocso.farm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository thao tác dữ liệu phản hồi sản phẩm.
 */
public interface ProductFeedbackRepository extends JpaRepository<ProductFeedback, UUID>, JpaSpecificationExecutor<ProductFeedback> {

    Page<ProductFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ProductFeedback> findByProductionLot_Organization_OrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Optional<ProductFeedback> findByIdAndProductionLot_Organization_OrganizationId(UUID id, UUID organizationId);

    Optional<ProductFeedback> findByLookupCodeHash(String lookupCodeHash);

    boolean existsByLookupCodeHash(String lookupCodeHash);

    boolean existsByTraceCode_IdAndSeverity(UUID traceCodeId, ProductFeedbackSeverity severity);
}
