package vn.nguongocso.farm.service;

import java.util.UUID;

import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;
/**
 * Nghiệp vụ thu hoạch.
*/
public interface HarvestEligibilityService {
    /** Tính điều kiện thu hoạch của lô sản xuất. */
    HarvestEligibilityResponse calculateHarvestEligibility(UUID productionLotId);
}
