package vn.nguongocso.farm.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.request.CreateInputMaterialRequest;
import vn.nguongocso.farm.dto.request.UpdateInputMaterialRequest;
import vn.nguongocso.farm.dto.response.InputMaterialResponse;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.MaterialGroup;
import vn.nguongocso.farm.service.InputMaterialService;
import vn.nguongocso.permission.service.PermissionChecker;

/**
 * REST Controller quản lý danh mục vật tư đầu vào kèm thời gian cách ly (US NCL-09-CN-010).
 */
@RestController
@RequestMapping("/api/v1/input-materials")
@RequiredArgsConstructor
public class InputMaterialController {

	private final InputMaterialService inputMaterialService;
	private final PermissionChecker permissionChecker;

	/**
	 * Tìm kiếm và phân trang danh mục vật tư đầu vào.
	 */
	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResult<Page<InputMaterialResponse>>> search(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) MaterialGroup group,
			@RequestParam(required = false) FarmActivityType activityType,
			@RequestParam(required = false) Boolean isActive,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) Integer limit,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "ASC") String sortDirection) {

		String effectiveKeyword = (keyword != null && !keyword.isBlank()) ? keyword : search;
		int effectiveSize = (limit != null && limit > 0) ? limit : size;

		Sort sort = sortDirection.equalsIgnoreCase("DESC")
				? Sort.by(sortBy).descending()
				: Sort.by(sortBy).ascending();
		Pageable pageable = PageRequest.of(page, effectiveSize, sort);

		Page<InputMaterialResponse> result = inputMaterialService.searchMaterials(effectiveKeyword, group, activityType, isActive, pageable);
		return ResponseEntity.ok(ApiResult.success(result));
	}

	/**
	 * Lấy chi tiết vật tư đầu vào theo ID.
	 */
	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResult<InputMaterialResponse>> getById(@PathVariable UUID id) {
		InputMaterialResponse response = inputMaterialService.getInputMaterialById(id);
		return ResponseEntity.ok(ApiResult.success(response));
	}

	/**
	 * Tạo mới vật tư đầu vào (Chỉ Quản trị viên nền tảng theo QTN-17).
	 */
	@PostMapping
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<InputMaterialResponse>> create(
			@Valid @RequestBody CreateInputMaterialRequest request,
			@AuthenticationPrincipal CustomUserDetails currentUser) {

		permissionChecker.check("input_material", "CREATE");
		UUID currentUserId = currentUser != null ? currentUser.getUserId() : null;
		InputMaterialResponse response = inputMaterialService.createInputMaterial(request, currentUserId);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResult.success(HttpStatus.CREATED.value(), response));
	}

	/**
	 * Cập nhật vật tư đầu vào (Chỉ Quản trị viên nền tảng theo QTN-17).
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<InputMaterialResponse>> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateInputMaterialRequest request,
			@AuthenticationPrincipal CustomUserDetails currentUser) {

		permissionChecker.check("input_material", "UPDATE");
		UUID currentUserId = currentUser != null ? currentUser.getUserId() : null;
		InputMaterialResponse response = inputMaterialService.updateInputMaterial(id, request, currentUserId);
		return ResponseEntity.ok(ApiResult.success(response));
	}

	/**
	 * Đổi trạng thái kích hoạt/ngừng sử dụng của vật tư đầu vào.
	 */
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<InputMaterialResponse>> toggleStatus(
			@PathVariable UUID id,
			@RequestParam Boolean isActive) {

		permissionChecker.check("input_material", "UPDATE");
		InputMaterialResponse response = inputMaterialService.toggleActiveStatus(id, isActive);
		return ResponseEntity.ok(ApiResult.success(response));
	}

	/**
	 * Xóa vật tư đầu vào (Bị chặn nếu vật tư đã được dùng trong nhật ký canh tác theo TC-04).
	 */
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<Void>> delete(@PathVariable UUID id) {
		permissionChecker.check("input_material", "DELETE");
		inputMaterialService.deleteInputMaterial(id);
		return ResponseEntity.ok(ApiResult.success(null));
	}
}
