package vn.nguongocso.farm.service;

import java.util.UUID;

import vn.nguongocso.farm.dto.request.UpdateFarmAreaBoundaryRequest;
import vn.nguongocso.farm.dto.response.FarmAreaBoundaryResponse;

/** Nghiệp vụ đọc và cập nhật ranh giới vùng trồng. */
public interface FarmAreaBoundaryService {

    /** Lấy ranh giới vùng trồng thuộc tổ chức hiện tại. */
    FarmAreaBoundaryResponse getBoundary(UUID farmAreaId);

    /** Thiết lập hoặc cập nhật ranh giới vùng trồng thuộc tổ chức hiện tại. */
    FarmAreaBoundaryResponse updateBoundary(UUID farmAreaId, UpdateFarmAreaBoundaryRequest request);
}
