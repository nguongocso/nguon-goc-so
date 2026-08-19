package vn.nguongocso.organization.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.dto.request.OrganizationUpdateRequest;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.service.OrganizationService;
import vn.nguongocso.permission.service.PermissionChecker;

@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
/** Quản lý hồ sơ tổ chức hiện tại. */
public class OrganizationProfileController {
    private final OrganizationService organizationService;
    private final PermissionChecker permissionChecker;

    /** Lấy hồ sơ tổ chức hiện tại. */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<OrganizationProfileResponse>> getProfile() {
        log.info("Lấy hồ sơ tổ chức hiện tại");
        return ResponseEntity.ok(ApiResult.success(organizationService.getCurrentOrganizationProfile()));
    }

    /** Cập nhật hồ sơ tổ chức hiện tại. */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<OrganizationProfileResponse>> updateProfile(
            @Valid @RequestBody OrganizationUpdateRequest request) {
        log.info("Cập nhật hồ sơ tổ chức hiện tại");
        return ResponseEntity.ok(ApiResult.success(organizationService.updateCurrentOrganization(request)));
    }
}
