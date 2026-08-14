package vn.nguongocso.farm.service;

import java.util.List;

import vn.nguongocso.farm.dto.request.CreateFarmAreaRequest;
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
	 * Lấy danh mục đơn vị diện tích hợp lệ để hiển thị cho người dùng lựa chọn.
	 *
	 * @return danh sách đơn vị diện tích
	 */
	List<AreaUnit> getAreaUnits();
}