package vn.nguongocso.certification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.nguongocso.certification.entity.InspectionCriterionDefinition;

public interface InspectionCriterionDefinitionRepository extends JpaRepository<InspectionCriterionDefinition, Integer> {
}
