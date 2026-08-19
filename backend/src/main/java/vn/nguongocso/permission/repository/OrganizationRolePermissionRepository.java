package vn.nguongocso.permission.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.nguongocso.permission.entity.OrganizationRolePermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể OrganizationRolePermission.
 */
public interface OrganizationRolePermissionRepository
                extends JpaRepository<OrganizationRolePermission, UUID> {
        /**
         * Lấy toàn bộ quyền đã cấu hình cho một vai trò trong một tổ chức.
         */
        List<OrganizationRolePermission> findByOrganization_OrganizationIdAndRole_RoleId(
                        UUID organizationId,
                        Integer roleId);

        /**
         * Lấy cấu hình của một permission cụ thể.
         */
        Optional<OrganizationRolePermission> findByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
                        UUID organizationId,
                        Integer roleId,
                        Integer permissionId);

        /**
         * Kiểm tra permission đã được cấu hình hay chưa.
         */
        boolean existsByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
                        UUID organizationId,
                        Integer roleId,
                        Integer permissionId);
}