package vn.nguongocso.permission.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.permission.dto.request.UpdateRolePermissionRequest;
import vn.nguongocso.permission.dto.response.RolePermissionGroupResponse;
import vn.nguongocso.permission.dto.response.RolePermissionResponse;

/**
 * Service quản lý quyền của vai trò trong tổ chức.
 */
public interface OrganizationRolePermissionService {

    /**
     * Lấy toàn bộ danh mục quyền hệ thống, nhóm theo resource.
     *
     * @return danh sách nhóm quyền hệ thống
     */
    List<RolePermissionGroupResponse> getSystemPermissions();

    /**
     * Lấy cấu hình quyền của một vai trò trong tổ chức.
     *
     * @param organizationId ID tổ chức
     * @param roleId         ID vai trò
     * @return cấu hình quyền của vai trò
     */
    RolePermissionResponse getRolePermissions(
            UUID organizationId,
            Integer roleId);

    /**
     * Cập nhật cấu hình quyền của vai trò trong tổ chức.
     *
     * @param organizationId ID tổ chức
     * @param roleId         ID vai trò
     * @param request        danh sách quyền cần cập nhật
     * @return cấu hình quyền sau cập nhật
     */
    RolePermissionResponse updateRolePermissions(
            UUID organizationId,
            Integer roleId,
            UpdateRolePermissionRequest request);

    /**
     * Xác thực rằng người dùng hiện tại là VT-02 và thuộc đúng tổ chức.
     * Trả về CustomUserDetails nếu hợp lệ, nếu không ném BusinessException.
     *
     * @param organizationId ID tổ chức
     * @return thông tin người dùng quản lý tổ chức
     */
    CustomUserDetails validateOrganizationManager(UUID organizationId);
}
