package vn.nguongocso.organization.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.dto.response.AdministrativeUnitNode;
import vn.nguongocso.organization.service.AdministrativeUnitService;

/**
 * API danh mục đơn vị hành chính dùng chung (mọi người dùng đã đăng nhập).
 */
@RestController
@RequestMapping("/api/v1/administrative-units")
@RequiredArgsConstructor
public class AdministrativeUnitController {

	private final AdministrativeUnitService administrativeUnitService;

	/**
	 * Cây đơn vị hành chính 2 cấp (tỉnh chứa xã/phường), sắp xếp theo tên.
	 */
	@GetMapping("/tree")
	public ResponseEntity<ApiResult<List<AdministrativeUnitNode>>> getUnitTree() {
		return ResponseEntity.ok(ApiResult.success(administrativeUnitService.getUnitTree()));
	}
}
