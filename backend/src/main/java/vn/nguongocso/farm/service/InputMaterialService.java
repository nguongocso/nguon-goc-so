package vn.nguongocso.farm.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.farm.dto.request.CreateInputMaterialRequest;
import vn.nguongocso.farm.dto.request.UpdateInputMaterialRequest;
import vn.nguongocso.farm.dto.response.InputMaterialResponse;
import vn.nguongocso.farm.enums.MaterialGroup;

/**
 * Service quản lý danh mục vật tư đầu vào.
 */
public interface InputMaterialService {

	/**
	 * Tạo mới vật tư đầu vào.
	 */
	InputMaterialResponse createInputMaterial(CreateInputMaterialRequest request, UUID currentUserId);

	/**
	 * Cập nhật thông tin vật tư đầu vào.
	 */
	InputMaterialResponse updateInputMaterial(UUID id, UpdateInputMaterialRequest request, UUID currentUserId);

	/**
	 * Ngừng sử dụng hoặc kích hoạt lại vật tư đầu vào.
	 */
	InputMaterialResponse toggleActiveStatus(UUID id, Boolean isActive);

	/**
	 * Xóa vật tư đầu vào (Chỉ khi chưa xuất hiện trong nhật ký canh tác).
	 */
	void deleteInputMaterial(UUID id);

	/**
	 * Lấy chi tiết vật tư đầu vào theo ID.
	 */
	InputMaterialResponse getInputMaterialById(UUID id);

	/**
	 * Tìm kiếm và phân trang danh mục vật tư đầu vào.
	 */
	Page<InputMaterialResponse> searchMaterials(String keyword, MaterialGroup group, Boolean isActive, Pageable pageable);
}
