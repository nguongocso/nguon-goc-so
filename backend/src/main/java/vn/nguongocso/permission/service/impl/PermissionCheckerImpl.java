package vn.nguongocso.permission.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.repository.RoleRepository;
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

/** Service kiểm tra quyền của người dùng. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionCheckerImpl implements PermissionChecker {
    private final PermissionRepository permissionRepository;

    private final RoleRepository roleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final OrganizationRolePermissionRepository organizationRolePermissionRepository;

    /** Kiểm tra quyền của người dùng hiện tại đối với tài nguyên và hành động. */
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
            enabled = Boolean.TRUE.equals(organizationPermission.get().getEnabled());
        } else {
            // Dùng quyền mặc định của vai trò
            enabled = rolePermissionRepository
                    .findByRole_RoleIdAndPermission_PermissionId(
                            role.getRoleId(),
                            permission.getPermissionId())
                    .map(rolePermission -> Boolean.TRUE.equals(rolePermission.getEnabled()))
                    .orElse(Boolean.FALSE);
        }

        if (!enabled) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền thực hiện chức năng này.");
        }
    }

    /** Lấy danh sách tất cả permissions mà người dùng hiện tại có quyền truy cập. */
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

        // Lấy tất cả ghi đè của HTX cho vai trò đó
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

        // Lấy danh sách permission codes đang có hiệu lực
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
