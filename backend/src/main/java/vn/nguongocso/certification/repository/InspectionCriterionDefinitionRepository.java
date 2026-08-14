package vn.nguongocso.certification.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.certification.entity.InspectionCriterionDefinition;

public interface InspectionCriterionDefinitionRepository extends JpaRepository<InspectionCriterionDefinition, Integer> {

    List<InspectionCriterionDefinition> findByStandard_IdOrderByIdAsc(UUID standardId);

    boolean existsByStandard_IdAndCodeIgnoreCase(UUID standardId, String code);
}
