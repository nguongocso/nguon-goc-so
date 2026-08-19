package vn.nguongocso.permission.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.permission.dto.request.UpdateRolePermissionRequest;
import vn.nguongocso.permission.dto.response.RolePermissionGroupResponse;
import vn.nguongocso.permission.dto.response.RolePermissionResponse;
import vn.nguongocso.permission.service.OrganizationRolePermissionService;

import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý vai trò và quyền của tổ chức.
 */
@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
public class OrganizationRolePermissionController {

    private final OrganizationRolePermissionService organizationRolePermissionService;
    private final RoleRepository roleRepository;

    /**
     * Danh sách toàn bộ permission của hệ thống.
     */
    @GetMapping("/permissions")
    public ApiResult<List<RolePermissionGroupResponse>> getSystemPermissions() {

        return ApiResult.success(
                organizationRolePermissionService.getSystemPermissions());
    }

    /**
     * Lấy danh sách vai trò khả dụng trong tổ chức.
     * Chỉ VT-02 của tổ chức đó mới được gọi.
     * Trả về tất cả các vai trò hệ thống (trừ VT-01 được frontend tự lọc).
     */
    @GetMapping("/organizations/{organizationId}/roles")
    public ApiResult<List<Role>> getOrganizationRoles(
            @PathVariable UUID organizationId) {

        // Xác thực quyền VT-02
        organizationRolePermissionService.validateOrganizationManager(organizationId);

        List<Role> roles = roleRepository.findAll();
        return ApiResult.success(roles);
    }

    /**
     * Lấy cấu hình quyền của một vai trò trong tổ chức.
     */
    @GetMapping("/organizations/{organizationId}/roles/{roleId}/permissions")
    public ApiResult<RolePermissionResponse> getRolePermissions(
            @PathVariable UUID organizationId,
            @PathVariable Integer roleId) {

        return ApiResult.success(
                organizationRolePermissionService.getRolePermissions(
                        organizationId,
                        roleId));
    }

    /**
     * Cập nhật quyền của một vai trò trong tổ chức.
     */
    @PutMapping("/organizations/{organizationId}/roles/{roleId}/permissions")
    public ApiResult<RolePermissionResponse> updateRolePermissions(
            @PathVariable UUID organizationId,
            @PathVariable Integer roleId,
            @Valid @RequestBody UpdateRolePermissionRequest request) {

        return ApiResult.success(
                organizationRolePermissionService.updateRolePermissions(
                        organizationId,
                        roleId,
                        request));
    }
}