package vn.nguongocso.farm.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.CreateProductCategoryRequest;
import vn.nguongocso.farm.dto.request.UpdateProductCategoryRequest;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;

/**
 * Định nghĩa các nghiệp vụ liên quan đến danh mục loại cây trồng.
 */
public interface ProductCategoryService {

	/**
	 * Lấy danh sách tất cả các loại cây trồng đang hoạt động.
	 *
	 * @return danh sách thông tin loại cây trồng
	 */
	List<ProductCategoryResponse> getAll();

	/**
	 * Tìm kiếm danh mục loại cây trồng theo tên, nhóm và trạng thái hoạt động.
	 *
	 * @param name        tên loại cây trồng
	 * @param group       nhóm loại cây trồng
	 * @param isActive    trạng thái hoạt động
	 * @param currentUser người dùng hiện tại
	 * @return danh sách loại cây trồng phù hợp với điều kiện tìm kiếm
	 */
	List<ProductCategoryResponse> search(String name, String group, Boolean isActive, CustomUserDetails currentUser);

	/**
	 * Tạo mới một loại cây trồng.
	 *
	 * @param request thông tin loại cây trồng cần tạo
	 * @return thông tin loại cây trồng sau khi tạo
	 */
	ProductCategoryResponse create(CreateProductCategoryRequest request);

	/**
	 * Cập nhật thông tin một loại cây trồng.
	 *
	 * @param id      mã loại cây trồng cần cập nhật
	 * @param request thông tin loại cây trồng cần cập nhật
	 * @return thông tin loại cây trồng sau khi cập nhật
	 */
	ProductCategoryResponse update(UUID id, UpdateProductCategoryRequest request);
}