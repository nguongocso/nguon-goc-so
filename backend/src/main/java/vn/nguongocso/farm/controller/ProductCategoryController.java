package vn.nguongocso.farm.controller;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.farm.dto.request.CreateProductCategoryRequest;
import vn.nguongocso.farm.dto.request.UpdateProductCategoryRequest;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;
import vn.nguongocso.farm.service.ProductCategoryService;
import vn.nguongocso.permission.service.PermissionChecker;

/**
 * REST Controller quản lý các API liên quan đến loại cây trồng.
 */
@RestController
@RequestMapping("/api/v1/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

	private final ProductCategoryService productCategoryService;
	private final PermissionChecker permissionChecker;

	/**
	 * Lấy và lọc danh sách loại nông sản.
	 * (THAY THẾ HOÀN TOÀN CHO PHƯƠNG THỨC getAll() CŨ)
	 */
	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResult<List<ProductCategoryResponse>>> search(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String categoryGroup,
			@RequestParam(required = false) Boolean isActive,
			@AuthenticationPrincipal CustomUserDetails currentUser) {

		List<ProductCategoryResponse> response = productCategoryService.search(name, categoryGroup, isActive,
				currentUser);
		return ResponseEntity.ok(ApiResult.success(response));
	}

	/**
	 * Thêm mới loại nông sản (Chỉ cho phép Admin hệ thống).
	 */
	@PostMapping
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<ProductCategoryResponse>> create(
			@Valid @RequestBody CreateProductCategoryRequest request) {

		permissionChecker.check("PRODUCT_CATEGORY", "CREATE");
		ProductCategoryResponse response = productCategoryService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(HttpStatus.CREATED.value(), response));
	}

	/**
	 * Cập nhật thông tin/trạng thái ẩn hiện loại nông sản (Chỉ cho phép Admin hệ
	 * thống).
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<ProductCategoryResponse>> update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateProductCategoryRequest request) {

		permissionChecker.check("PRODUCT_CATEGORY", "UPDATE");
		ProductCategoryResponse response = productCategoryService.update(id, request);
		return ResponseEntity.ok(ApiResult.success(response));
	}
}
