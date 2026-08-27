package vn.nguongocso.organization.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.dto.request.AssignAreasRequest;
import vn.nguongocso.organization.dto.response.AssignAreasResult;
import vn.nguongocso.organization.dto.response.AssignedAreaResponse;
import vn.nguongocso.organization.dto.response.RegulatorUserResponse;
import vn.nguongocso.organization.dto.response.UnassignAreaResult;
import vn.nguongocso.organization.service.AreaAssignmentService;

/**
 * API phân công địa bàn quản lý cho cán bộ quản lý ngành (NCL-743).
 *
 * <p>
 * Toàn bộ endpoint admin chỉ dành cho VT-01: chặn bằng {@code @PreAuthorize}
 * và kiểm tra lại trong service (belt-and-suspenders).
 * </p>
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AreaAssignmentAdminController {

	private final AreaAssignmentService areaAssignmentService;

	/**
	 * Danh sách tài khoản cán bộ quản lý ngành để gán địa bàn.
	 */
	@GetMapping
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<PageResponse<RegulatorUserResponse>>> listRegulators(
			@RequestParam(defaultValue = "VT-05") String role,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		if (!RoleCode.REGULATOR.equals(role)) {
			throw new BusinessException("Chỉ hỗ trợ liệt kê tài khoản vai trò Cán bộ quản lý ngành.");
		}

		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
		return ResponseEntity.ok(ApiResult.success(areaAssignmentService.listRegulators(keyword, pageable)));
	}

	/**
	 * Xem địa bàn đã gán của một tài khoản.
	 */
	@GetMapping("/{userId}/areas")
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<List<AssignedAreaResponse>>> getAssignedAreas(
			@PathVariable UUID userId) {

		return ResponseEntity.ok(ApiResult.success(areaAssignmentService.getAssignedAreas(userId)));
	}

	/**
	 * Gán hàng loạt địa bàn (all-or-nothing).
	 */
	@PostMapping("/{userId}/areas")
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<AssignAreasResult>> assignAreas(
			@AuthenticationPrincipal CustomUserDetails currentUser,
			@PathVariable UUID userId,
			@Valid @RequestBody AssignAreasRequest request) {

		return ResponseEntity.ok(ApiResult.success(areaAssignmentService.assignAreas(currentUser, userId, request)));
	}

	/**
	 * Gỡ một địa bàn khỏi tài khoản.
	 */
	@DeleteMapping("/{userId}/areas/{unitId}")
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<UnassignAreaResult>> unassignArea(
			@AuthenticationPrincipal CustomUserDetails currentUser,
			@PathVariable UUID userId,
			@PathVariable UUID unitId) {

		return ResponseEntity.ok(ApiResult.success(areaAssignmentService.unassignArea(currentUser, userId, unitId)));
	}
}
