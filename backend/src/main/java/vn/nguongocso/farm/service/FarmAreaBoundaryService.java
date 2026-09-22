package vn.nguongocso.farm.service;

import java.util.UUID;

import vn.nguongocso.farm.dto.request.UpdateFarmAreaBoundaryRequest;
import vn.nguongocso.farm.dto.response.FarmAreaBoundaryResponse;
/**
 * Nghiệp vụ ranh giới vùng trồng.
*/
public interface FarmAreaBoundaryService {
    /** Lấy ranh giới vùng trồng. */
    FarmAreaBoundaryResponse getBoundary(UUID farmAreaId);

    /** Cập nhật ranh giới vùng trồng. */
    FarmAreaBoundaryResponse updateBoundary(UUID farmAreaId, UpdateFarmAreaBoundaryRequest request);
}
