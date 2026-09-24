package vn.nguongocso.farm.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.farm.dto.request.CreateFarmAreaRequest;
import vn.nguongocso.farm.dto.request.UpdateFarmAreaRequest;
import vn.nguongocso.farm.dto.response.FarmAreaResponse;
import vn.nguongocso.farm.enums.AreaUnit;

/**
 * Nghiệp vụ vùng trồng.
*/
public interface FarmAreaService {
    /** Tạo vùng trồng mới. */
    FarmAreaResponse create(CreateFarmAreaRequest request);

    /** Lấy danh sách vùng trồng. */
    List<FarmAreaResponse> getFarmAreas();

    /** Lấy danh sách vùng trồng theo trạng thái. */
    List<FarmAreaResponse> getFarmAreas(Boolean activeOnly);

    /** Lấy chi tiết vùng trồng theo ID. */
    FarmAreaResponse getFarmAreaById(UUID id);

    /** Cập nhật vùng trồng. */
    FarmAreaResponse update(UUID id, UpdateFarmAreaRequest request);

    /** Đổi trạng thái kích hoạt vùng trồng. */
    FarmAreaResponse toggleStatus(UUID id, boolean isActive);

    /** Xóa vùng trồng. */
    void delete(UUID id);

    /** Lấy danh mục đơn vị diện tích. */
    List<AreaUnit> getAreaUnits();
}