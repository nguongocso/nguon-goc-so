package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.nguongocso.certification.entity.InspectionCriterion;

import java.util.UUID;

/**
 * Repository cho thực thể InspectionCriterion.
 */
public interface InspectionCriterionRepository
        extends JpaRepository<InspectionCriterion, UUID> {
}
