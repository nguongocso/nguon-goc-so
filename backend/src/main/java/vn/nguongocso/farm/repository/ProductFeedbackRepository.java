package vn.nguongocso.farm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.nguongocso.farm.entity.ProductFeedback;
import java.util.UUID;

/**
 * Repository thao tác dữ liệu phản hồi sản phẩm.
 */
public interface ProductFeedbackRepository extends JpaRepository<ProductFeedback, UUID> {

    Page<ProductFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ProductFeedback> findByProductionLot_Organization_OrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
}
