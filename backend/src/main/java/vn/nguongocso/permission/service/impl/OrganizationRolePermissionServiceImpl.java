package vn.nguongocso.permission.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.permission.constant.ResourceLabelConstants;
import vn.nguongocso.permission.dto.request.UpdateRolePermissionRequest;
import vn.nguongocso.permission.dto.response.PermissionItemResponse;
import vn.nguongocso.permission.dto.response.RolePermissionGroupResponse;
import vn.nguongocso.permission.dto.response.RolePermissionResponse;
import vn.nguongocso.permission.entity.OrganizationRolePermission;
import vn.nguongocso.permission.entity.Permission;
import vn.nguongocso.permission.entity.RolePermission;
import vn.nguongocso.permission.repository.OrganizationRolePermissionRepository;
import vn.nguongocso.permission.repository.PermissionRepository;
import vn.nguongocso.permission.repository.RolePermissionRepository;
import vn.nguongocso.permission.service.OrganizationRolePermissionService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Triển khai các phương thức quản lý quyền của vai trò trong tổ chức.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationRolePermissionServiceImpl
                implements OrganizationRolePermissionService {
        private static final String ORG_MANAGER_ROLE = "VT-02";
        private static final String ADMIN_ROLE = "VT-01";

        private final PermissionRepository permissionRepository;
        private final RolePermissionRepository rolePermissionRepository;
        private final OrganizationRolePermissionRepository organizationRolePermissionRepository;

        private final OrganizationRepository organizationRepository;

        private final RoleRepository roleRepository;
        private final UserRepository userRepository;

        private final PermissionChecker permissionChecker;

        private CustomUserDetails getCurrentUser() {
                return SecurityUtils.getCurrentUserDetails();
        }

        /**
         * Kiểm tra người dùng có quyền Quản lý HTX
         * và thuộc đúng tổ chức hay không.
         */
        @Override
        public CustomUserDetails validateOrganizationManager(UUID organizationId) {

                CustomUserDetails currentUser = getCurrentUser();

                if (!ORG_MANAGER_ROLE.equals(currentUser.getRoleCode())) {
                        throw new BusinessException(
                                        "Bạn không có quyền cấu hình phân quyền.");
                }

                if (!organizationId.equals(currentUser.getOrganizationId())) {
                        throw new BusinessException(
                                        "Bạn không có quyền cấu hình phân quyền cho tổ chức này.");
                }

                return currentUser;
        }

        /**
         * Không cho phép chỉnh sửa quyền của Admin hệ thống.
         */
        private void validateTargetRole(Integer roleId) {

                Role role = roleRepository.findById(roleId)
                                .orElseThrow(() -> new BusinessException("Vai trò không tồn tại."));

                if (ADMIN_ROLE.equals(role.getCode())) {
                        throw new BusinessException(
                                        "Không được phép cấu hình quyền cho quản trị viên hệ thống.");
                }
        }

        /**
         * Lấy entity Organization.
         */
        private Organization getOrganization(UUID organizationId) {
                return organizationRepository.findById(organizationId)
                                .orElseThrow(() -> new BusinessException("Tổ chức không tồn tại."));
        }

        /**
         * Lấy entity Role.
         */
        private Role getRole(Integer roleId) {
                return roleRepository.findById(roleId)
                                .orElseThrow(() -> new BusinessException("Vai trò không tồn tại."));
        }

        /**
         * Lấy entity User hiện tại.
         */
        private User getCurrentUserEntity() {

                UUID userId = getCurrentUser().getUserId();

                return userRepository.findById(userId)
                                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại."));
        }

        /**
         * Lấy danh sách quyền hệ thống, nhóm theo resource.
         */
        @Override
        @Transactional(readOnly = true)
        public List<RolePermissionGroupResponse> getSystemPermissions() {

                CustomUserDetails currentUser = getCurrentUser();

                if (!ORG_MANAGER_ROLE.equals(currentUser.getRoleCode())) {
                        throw new BusinessException("Bạn không có quyền xem danh sách quyền.");
                }

                List<Permission> permissions = permissionRepository.findAll();

                Map<String, List<Permission>> groupedPermissions = permissions.stream()
                                .sorted(Comparator
                                                .comparing(Permission::getResource)
                                                .thenComparing(Permission::getAction))
                                .collect(Collectors.groupingBy(
                                                Permission::getResource,
                                                LinkedHashMap::new,
                                                Collectors.toList()));

                List<RolePermissionGroupResponse> responses = new ArrayList<>();

                for (Map.Entry<String, List<Permission>> entry : groupedPermissions.entrySet()) {

                        String resource = entry.getKey();

                        List<PermissionItemResponse> permissionItems = entry.getValue()
                                        .stream()
                                        .map(permission -> PermissionItemResponse.builder()
                                                        .permissionId(permission.getPermissionId())
                                                        .action(permission.getAction())
                                                        .description(permission.getDescription())
                                                        // Chưa áp dụng theo role/tổ chức
                                                        .isEnabled(null)
                                                        .isDefault(null)
                                                        .build())
                                        .toList();

                        responses.add(
                                        RolePermissionGroupResponse.builder()
                                                        .resource(resource)
                                                        .resourceLabel(ResourceLabelConstants.getLabel(resource))
                                                        .permissions(permissionItems)
                                                        .build());
                }

                return responses;
        }

        /**
         * Lấy danh sách quyền của một vai trò trong tổ chức, nhóm theo resource.
         */
        @Override
        @Transactional(readOnly = true)
        public RolePermissionResponse getRolePermissions(
                        UUID organizationId,
                        Integer roleId) {

                // Chỉ VT-02 của đúng tổ chức mới được xem

                validateOrganizationManager(organizationId);

                Role role = getRole(roleId);

                List<Permission> permissions = permissionRepository.findAll();

                Map<Integer, RolePermission> defaultPermissions = getDefaultPermissionMap(roleId);

                Map<Integer, OrganizationRolePermission> organizationPermissions = getOrganizationPermissionMap(
                                organizationId,
                                roleId);

                Map<String, List<PermissionItemResponse>> grouped = new LinkedHashMap<>();

                permissions.stream()
                                .sorted(Comparator
                                                .comparing(Permission::getResource)
                                                .thenComparing(Permission::getAction))
                                .forEach(permission -> {

                                        RolePermission defaultPermission = defaultPermissions
                                                        .get(permission.getPermissionId());

                                        OrganizationRolePermission overridePermission = organizationPermissions
                                                        .get(permission.getPermissionId());

                                        boolean enabled;
                                        boolean isDefault;

                                        if (overridePermission != null) {

                                                enabled = Boolean.TRUE.equals(
                                                                overridePermission.getEnabled());

                                                isDefault = false;

                                        } else {

                                                enabled = defaultPermission != null
                                                                && Boolean.TRUE.equals(
                                                                                defaultPermission.getEnabled());

                                                isDefault = true;
                                        }

                                        PermissionItemResponse item = PermissionItemResponse.builder()
                                                        .permissionId(permission.getPermissionId())
                                                        .action(permission.getAction())
                                                        .description(permission.getDescription())
                                                        .isEnabled(enabled)
                                                        .isDefault(isDefault)
                                                        .build();

                                        grouped.computeIfAbsent(
                                                        permission.getResource(),
                                                        key -> new ArrayList<>())
                                                        .add(item);
                                });

                List<RolePermissionGroupResponse> groups = grouped.entrySet()
                                .stream()
                                .map(entry -> RolePermissionGroupResponse.builder()
                                                .resource(entry.getKey())
                                                .resourceLabel(ResourceLabelConstants.getLabel(entry.getKey()))
                                                .permissions(entry.getValue())
                                                .build())
                                .toList();

                return RolePermissionResponse.builder()
                                .organizationId(organizationId)
                                .roleId(role.getRoleId())
                                .roleCode(role.getCode())
                                .roleName(role.getName())
                                .groups(groups)
                                .build();
        }

        private Map<Integer, RolePermission> getDefaultPermissionMap(Integer roleId) {

                return rolePermissionRepository.findByRole_RoleId(roleId)
                                .stream()
                                .collect(Collectors.toMap(
                                                rp -> rp.getPermission().getPermissionId(),
                                                rp -> rp));
        }

        private Map<Integer, OrganizationRolePermission> getOrganizationPermissionMap(
                        UUID organizationId,
                        Integer roleId) {

                return organizationRolePermissionRepository
                                .findByOrganization_OrganizationIdAndRole_RoleId(
                                                organizationId,
                                                roleId)
                                .stream()
                                .collect(Collectors.toMap(
                                                rp -> rp.getPermission().getPermissionId(),
                                                rp -> rp));
        }

        /**
         * Cập nhật quyền của một vai trò trong tổ chức.
         */
        @Override
        @Transactional
        public RolePermissionResponse updateRolePermissions(
                        UUID organizationId,
                        Integer roleId,
                        UpdateRolePermissionRequest request) {

                // Kiểm tra quyền
                validateOrganizationManager(organizationId);

                // Không cho phép sửa quyền Admin hệ thống
                validateTargetRole(roleId);

                Organization organization = getOrganization(organizationId);
                Role role = getRole(roleId);
                User currentUser = getCurrentUserEntity();

                if (request.getPermissions() == null
                                || request.getPermissions().isEmpty()) {
                        throw new BusinessException("Danh sách quyền không được để trống.");
                }

                for (UpdateRolePermissionRequest.PermissionToggle item : request.getPermissions()) {

                        Permission permission = permissionRepository
                                        .findById(item.getPermissionId())
                                        .orElseThrow(() -> new BusinessException("Permission không tồn tại."));

                        Optional<OrganizationRolePermission> optional = organizationRolePermissionRepository
                                        .findByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
                                                        organizationId,
                                                        roleId,
                                                        permission.getPermissionId());

                        if (optional.isPresent()) {

                                OrganizationRolePermission override = optional.get();
                                override.setEnabled(item.getIsEnabled());
                                override.setUpdatedBy(currentUser);

                                organizationRolePermissionRepository.save(override);

                        } else {

                                OrganizationRolePermission override = OrganizationRolePermission.builder()
                                                .organization(organization)
                                                .role(role)
                                                .permission(permission)
                                                .enabled(item.getIsEnabled())
                                                .updatedBy(currentUser)
                                                .build();

                                organizationRolePermissionRepository.save(override);
                        }
                }

                return getRolePermissions(organizationId, roleId);
        }
}