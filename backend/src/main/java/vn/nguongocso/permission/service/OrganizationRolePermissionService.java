package vn.nguongocso.permission.service;

import vn.nguongocso.permission.dto.request.UpdateRolePermissionRequest;
import vn.nguongocso.permission.dto.response.RolePermissionGroupResponse;
import vn.nguongocso.permission.dto.response.RolePermissionResponse;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;

/**
 * Service quản lý quyền của vai trò trong tổ chức.
 */
public interface OrganizationRolePermissionService {
        /**
         * Lấy toàn bộ danh mục quyền hệ thống,
         * nhóm theo resource.
         */
        List<RolePermissionGroupResponse> getSystemPermissions();

        /**
         * Lấy cấu hình quyền của một vai trò trong tổ chức.
         */
        RolePermissionResponse getRolePermissions(
                        UUID organizationId,
                        Integer roleId);

        /**
         * Cập nhật cấu hình quyền của vai trò trong tổ chức.
         */
        RolePermissionResponse updateRolePermissions(
                        UUID organizationId,
                        Integer roleId,
                        UpdateRolePermissionRequest request);

        /**
         * Xác thực rằng người dùng hiện tại là VT-02 và thuộc đúng tổ chức.
         * Trả về CustomUserDetails nếu hợp lệ, nếu không ném BusinessException.
         */
        CustomUserDetails validateOrganizationManager(UUID organizationId);
}
