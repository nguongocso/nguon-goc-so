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

    /**
     * Tìm các phản ánh nghiêm trọng chưa đóng theo danh sách lô sản xuất (NCL-07-CN-006).
     */
    @org.springframework.data.jpa.repository.Query("SELECT pf FROM ProductFeedback pf WHERE pf.productionLot.id IN :lotIds AND pf.severity IN :severities AND pf.status <> :closedStatus")
    java.util.List<ProductFeedback> findSeriousOpenFeedbacksByLotIds(
            @org.springframework.data.repository.query.Param("lotIds") java.util.Collection<UUID> lotIds,
            @org.springframework.data.repository.query.Param("severities") java.util.Collection<ProductFeedbackSeverity> severities,
            @org.springframework.data.repository.query.Param("closedStatus") vn.nguongocso.farm.enums.ProductFeedbackStatus closedStatus);
}

