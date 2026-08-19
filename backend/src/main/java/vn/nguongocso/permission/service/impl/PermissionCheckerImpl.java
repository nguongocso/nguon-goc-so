package vn.nguongocso.permission.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.permission.entity.OrganizationRolePermission;
import vn.nguongocso.permission.entity.Permission;
import vn.nguongocso.permission.entity.RolePermission;
import vn.nguongocso.permission.repository.OrganizationRolePermissionRepository;
import vn.nguongocso.permission.repository.PermissionRepository;
import vn.nguongocso.permission.repository.RolePermissionRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.auth.repository.RoleRepository;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Service kiểm tra quyền của người dùng.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionCheckerImpl implements PermissionChecker {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final OrganizationRolePermissionRepository organizationRolePermissionRepository;

    /**
     * Kiểm tra quyền của người dùng hiện tại đối với một resource và action cụ thể.
     * Nếu người dùng không có quyền, ném ra BusinessException với mã lỗi 403.
     *
     * @param resource Tên resource (ví dụ: "production_lot").
     * @param action   Tên action (ví dụ: "view", "create", "update", "delete").
     */
    @Override
    public void check(String resource, String action) {

        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();

        Permission permission = permissionRepository
                .findByResourceAndAction(resource, action)
                .orElseThrow(() -> new BusinessException("Permission không tồn tại."));

        Role role = roleRepository.findByCode(currentUser.getRoleCode())
                .orElseThrow(() -> new BusinessException("Vai trò không tồn tại."));

        Optional<OrganizationRolePermission> organizationPermission = organizationRolePermissionRepository
                .findByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
                        currentUser.getOrganizationId(),
                        role.getRoleId(),
                        permission.getPermissionId());

        boolean enabled;

        if (organizationPermission.isPresent()) {

            enabled = Boolean.TRUE.equals(
                    organizationPermission.get().getEnabled());

        } else {

            RolePermission defaultPermission = rolePermissionRepository
                    .findByRole_RoleIdAndPermission_PermissionId(
                            role.getRoleId(),
                            permission.getPermissionId())
                    .orElseThrow(() -> new BusinessException(
                            "Permission mặc định chưa được cấu hình."));

            enabled = Boolean.TRUE.equals(defaultPermission.getEnabled());
        }

        if (!enabled) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền thực hiện chức năng này.");
        }
    }

    /**
     * Lấy danh sách tất cả permissions mà người dùng hiện tại có quyền truy cập.
     *
     * @return Danh sách permission codes (resource:action) mà người dùng có quyền.
     */
    @Override
    public List<String> getPermissionsForCurrentUser() {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserDetails();
        if (currentUser == null) {
            return Collections.emptyList();
        }

        Role role = roleRepository.findByCode(currentUser.getRoleCode())
                .orElse(null);
        if (role == null) {
            return Collections.emptyList();
        }

        // Lấy tất cả permissions mặc định của vai trò
        List<RolePermission> defaultPermissions = rolePermissionRepository.findByRole_RoleId(role.getRoleId());

        Map<Integer, Boolean> permissionStatusMap = new HashMap<>();
        for (RolePermission rp : defaultPermissions) {
            if (rp.getPermission() != null) {
                permissionStatusMap.put(rp.getPermission().getPermissionId(), Boolean.TRUE.equals(rp.getEnabled()));
            }
        }

        // Lấy tất cả ghi đè của HTX cho vai trò đó (nếu user thuộc HTX)
        if (currentUser.getOrganizationId() != null) {
            List<OrganizationRolePermission> orgPermissions = organizationRolePermissionRepository
                    .findByOrganization_OrganizationIdAndRole_RoleId(
                            currentUser.getOrganizationId(),
                            role.getRoleId());
            for (OrganizationRolePermission orp : orgPermissions) {
                if (orp.getPermission() != null) {
                    permissionStatusMap.put(orp.getPermission().getPermissionId(),
                            Boolean.TRUE.equals(orp.getEnabled()));
                }
            }
        }

        // Lấy danh sách permission codes (resource:action) có trạng thái enabled = true
        List<String> enabledPermissions = new ArrayList<>();
        List<Permission> allPermissions = permissionRepository.findAll();

        for (Permission p : allPermissions) {
            Boolean enabled = permissionStatusMap.get(p.getPermissionId());
            if (Boolean.TRUE.equals(enabled)) {
                enabledPermissions.add(p.getResource() + ":" + p.getAction());
            }
        }

        return enabledPermissions;
    }
}