package vn.nguongocso.certification.service;

import vn.nguongocso.farm.entity.ProductionLot;

import java.util.List;

/**
 * Service for validating milestone completion before packaging.
 * Story: NCL-09-CN-011
 */
public interface MilestoneValidationService {

    /**
     * Validate that all mandatory milestones are satisfied by farm logs.
     * Returns empty list if all milestones are satisfied, otherwise returns
     * the names of missing milestones.
     *
     * @param lot the production lot to validate
     * @return list of missing mandatory milestone names (empty = all satisfied)
     */
    List<String> validateMilestoneCompletion(ProductionLot lot);
}
