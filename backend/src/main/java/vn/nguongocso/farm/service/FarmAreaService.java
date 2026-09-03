package vn.nguongocso.farm.service;

import java.util.List;

import java.util.UUID;

import vn.nguongocso.farm.dto.request.CreateFarmAreaRequest;
import vn.nguongocso.farm.dto.request.UpdateFarmAreaRequest;
import vn.nguongocso.farm.dto.response.FarmAreaResponse;
import vn.nguongocso.farm.enums.AreaUnit;

/**
 * Service xử lý nghiệp vụ vùng trồng.
 */
public interface FarmAreaService {

	/**
	 * Tạo mới vùng trồng.
	 *
	 * @param request thông tin vùng trồng cần tạo
	 * @return thông tin vùng trồng sau khi tạo
	 */
	FarmAreaResponse create(CreateFarmAreaRequest request);

	/**
	 * Lấy danh sách vùng trồng thuộc tổ chức của người dùng đang đăng nhập.
	 *
	 * @return danh sách vùng trồng
	 */
	List<FarmAreaResponse> getFarmAreas();

	/**
	 * Lấy danh sách vùng trồng thuộc tổ chức, có thể lọc theo trạng thái kích hoạt.
	 */
	List<FarmAreaResponse> getFarmAreas(Boolean activeOnly);

	/**
	 * Lấy chi tiết vùng trồng theo ID (US NCL-02-CN-005).
	 */
	FarmAreaResponse getFarmAreaById(UUID id);

	/**
	 * Cập nhật thông tin vùng trồng (US NCL-02-CN-005).
	 */
	FarmAreaResponse update(UUID id, UpdateFarmAreaRequest request);

	/**
	 * Đổi trạng thái kích hoạt / ngừng sử dụng vùng trồng (US NCL-02-CN-005).
	 */
	FarmAreaResponse toggleStatus(UUID id, boolean isActive);

	/**
	 * Xóa vùng trồng (US NCL-02-CN-005). Chặn xóa nếu có lô sản xuất liên quan.
	 */
	void delete(UUID id);

	/**
	 * Lấy danh mục đơn vị diện tích hợp lệ để hiển thị cho người dùng lựa chọn.
	 *
	 * @return danh sách đơn vị diện tích
	 */
	List<AreaUnit> getAreaUnits();
}