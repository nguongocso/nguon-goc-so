package vn.nguongocso.permission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.permission.entity.OrganizationRolePermission;
import vn.nguongocso.permission.entity.Permission;
import vn.nguongocso.permission.entity.RolePermission;
import vn.nguongocso.permission.repository.OrganizationRolePermissionRepository;
import vn.nguongocso.permission.repository.PermissionRepository;
import vn.nguongocso.permission.repository.RolePermissionRepository;

/**
 * Unit tests cho PermissionCheckerImpl — regression NCL-08-CN-011
 * (lỗi "Permission không tồn tại." khi phê duyệt yêu cầu thu hồi).
 *
 * <p>Phân biệt 2 loại lỗi authorization:</p>
 * <ul>
 *   <li>CASE A: permission tồn tại trong DB nhưng vai trò/tổ chức chưa
 *       được cấp (kể cả không có dòng mapping) → 403 "Bạn không có quyền
 *       thực hiện chức năng này.".</li>
 *   <li>CASE B: permission được code yêu cầu chưa được seed trong DB →
 *       lỗi cấu hình "Permission không tồn tại." — không được bypass ngầm.</li>
 * </ul>
 */
class PermissionCheckerImplTest {

    private PermissionRepository permissionRepository;
    private RoleRepository roleRepository;
    private RolePermissionRepository rolePermissionRepository;
    private OrganizationRolePermissionRepository organizationRolePermissionRepository;
    private PermissionCheckerImpl permissionChecker;

    private Role orgManagerRole;
    private Permission recallUpdatePermission;

    private static final String RESOURCE = "recall";
    private static final String ACTION = "UPDATE";
    private static final String ROLE_CODE = "VT-02";
    private static final int ROLE_ID = 2;
    private static final int PERMISSION_ID = 60;

    @BeforeEach
    void setUp() {
        permissionRepository = mock(PermissionRepository.class);
        roleRepository = mock(RoleRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        organizationRolePermissionRepository = mock(OrganizationRolePermissionRepository.class);

        permissionChecker = new PermissionCheckerImpl(
                permissionRepository,
                roleRepository,
                rolePermissionRepository,
                organizationRolePermissionRepository);

        // Người dùng Manager B (VT-02) thuộc một tổ chức hợp tác xã
        Organization organization = mock(Organization.class);
        when(organization.getOrganizationId()).thenReturn(UUID.randomUUID());
        when(organization.getName()).thenReturn("HTX Test");
        when(organization.getCode()).thenReturn("HTX-TEST");

        OrganizationUser organizationUser = mock(OrganizationUser.class);
        when(organizationUser.getOrganization()).thenReturn(organization);

        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName("manager_b");
        user.setPasswordHash("password-hash");
        user.setFullName("Manager B");
        user.setStatus(UserStatus.ACTIVE);

        orgManagerRole = new Role();
        orgManagerRole.setRoleId(ROLE_ID);
        orgManagerRole.setCode(ROLE_CODE);
        orgManagerRole.setName("Quản lý hợp tác xã");

        CustomUserDetails managerB = new CustomUserDetails(user, organizationUser, orgManagerRole);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                managerB, null, managerB.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        recallUpdatePermission = new Permission();
        recallUpdatePermission.setPermissionId(PERMISSION_ID);
        recallUpdatePermission.setResource(RESOURCE);
        recallUpdatePermission.setAction(ACTION);
        recallUpdatePermission.setDescription("Phê duyệt / từ chối yêu cầu thu hồi");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TEST 5: Permission chưa được seed trong DB - throw rõ ràng, không bypass ngầm")
    void check_ThrowsClearError_WhenPermissionNotSeeded() {
        // Mô phỏng đúng sự cố NCL-08-CN-011: bảng permissions thiếu recall:UPDATE
        when(permissionRepository.findByResourceAndAction(RESOURCE, ACTION))
                .thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> permissionChecker.check(RESOURCE, ACTION),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Permission không tồn tại.");

        // Không được âm thầm coi là có quyền hay không có quyền — phải dừng ngay
        verify(roleRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("TEST 3: Permission tồn tại nhưng vai trò chưa được cấp - lỗi 403 (CASE A)")
    void check_ThrowsForbidden_WhenRoleNotGrantedPermission() {
        when(permissionRepository.findByResourceAndAction(RESOURCE, ACTION))
                .thenReturn(Optional.of(recallUpdatePermission));
        when(roleRepository.findByCode(ROLE_CODE)).thenReturn(Optional.of(orgManagerRole));
        when(organizationRolePermissionRepository
                .findByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
                        any(), any(), any()))
                .thenReturn(Optional.empty());

        RolePermission disabledDefault = RolePermission.builder()
                .role(orgManagerRole)
                .permission(recallUpdatePermission)
                .enabled(false)
                .build();
        when(rolePermissionRepository.findByRole_RoleIdAndPermission_PermissionId(ROLE_ID, PERMISSION_ID))
                .thenReturn(Optional.of(disabledDefault));

        BusinessException exception = catchThrowableOfType(
                () -> permissionChecker.check(RESOURCE, ACTION),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getMessage()).isEqualTo("Bạn không có quyền thực hiện chức năng này.");
    }

    @Test
    @DisplayName("TEST 3b: Ghi đè của tổ chức bật quyền - cho phép thao tác")
    void check_Passes_WhenOrganizationGrantsPermissionToRole() {
        when(permissionRepository.findByResourceAndAction(RESOURCE, ACTION))
                .thenReturn(Optional.of(recallUpdatePermission));
        when(roleRepository.findByCode(ROLE_CODE)).thenReturn(Optional.of(orgManagerRole));

        OrganizationRolePermission orgOverride = OrganizationRolePermission.builder()
                .organization(mock(Organization.class))
                .role(orgManagerRole)
                .permission(recallUpdatePermission)
                .enabled(true)
                .build();
        when(organizationRolePermissionRepository
                .findByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
                        any(), any(), any()))
                .thenReturn(Optional.of(orgOverride));

        permissionChecker.check(RESOURCE, ACTION);
    }

    @Test
    @DisplayName("TEST 3c: Không có ghi đè tổ chức - dùng quyền mặc định của vai trò (đã bật)")
    void check_Passes_WhenDefaultRolePermissionEnabled() {
        when(permissionRepository.findByResourceAndAction(RESOURCE, ACTION))
                .thenReturn(Optional.of(recallUpdatePermission));
        when(roleRepository.findByCode(ROLE_CODE)).thenReturn(Optional.of(orgManagerRole));
        when(organizationRolePermissionRepository
                .findByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
                        any(), any(), any()))
                .thenReturn(Optional.empty());

        RolePermission enabledDefault = RolePermission.builder()
                .role(orgManagerRole)
                .permission(recallUpdatePermission)
                .enabled(true)
                .build();
        when(rolePermissionRepository.findByRole_RoleIdAndPermission_PermissionId(ROLE_ID, PERMISSION_ID))
                .thenReturn(Optional.of(enabledDefault));

        permissionChecker.check(RESOURCE, ACTION);
    }

    @Test
    @DisplayName("TEST 3d: Vai trò chưa được cấp quyền (không có dòng mapping) - lỗi 403")
    void check_ThrowsForbidden_WhenRoleHasNoPermissionMapping() {
        when(permissionRepository.findByResourceAndAction(RESOURCE, ACTION))
                .thenReturn(Optional.of(recallUpdatePermission));
        when(roleRepository.findByCode(ROLE_CODE)).thenReturn(Optional.of(orgManagerRole));
        when(organizationRolePermissionRepository
                .findByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
                        any(), any(), any()))
                .thenReturn(Optional.empty());
        when(rolePermissionRepository.findByRole_RoleIdAndPermission_PermissionId(ROLE_ID, PERMISSION_ID))
                .thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> permissionChecker.check(RESOURCE, ACTION),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getMessage()).isEqualTo("Bạn không có quyền thực hiện chức năng này.");
    }
}
