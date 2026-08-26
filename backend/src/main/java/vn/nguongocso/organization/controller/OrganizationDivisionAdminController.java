package vn.nguongocso.organization.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.dto.request.UpdateOrganizationDivisionsRequest;
import vn.nguongocso.organization.service.AreaAssignmentService;

/**
 * API map tổ chức vào đơn vị hành chính (phục vụ lọc báo cáo theo địa bàn).
 */
@RestController
@RequestMapping("/api/v1/admin/organizations")
@RequiredArgsConstructor
public class OrganizationDivisionAdminController {

	private final AreaAssignmentService areaAssignmentService;

	/**
	 * Cập nhật province_id / commune_id của tổ chức.
	 */
	@PutMapping("/{organizationId}/divisions")
	@PreAuthorize("hasRole('VT-01')")
	public ResponseEntity<ApiResult<Void>> updateDivisions(
			@AuthenticationPrincipal CustomUserDetails currentUser,
			@PathVariable UUID organizationId,
			@Valid @RequestBody UpdateOrganizationDivisionsRequest request) {

		areaAssignmentService.updateOrganizationDivisions(currentUser, organizationId, request);
		return ResponseEntity.ok(ApiResult.success(null));
	}
}
