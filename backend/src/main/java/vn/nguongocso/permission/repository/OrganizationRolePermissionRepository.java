package vn.nguongocso.permission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.permission.entity.OrganizationRolePermission;

/**
 * Repository cho thực thể OrganizationRolePermission.
 */
@Repository
public interface OrganizationRolePermissionRepository extends JpaRepository<OrganizationRolePermission, UUID> {

    /**
     * Lấy toàn bộ quyền đã cấu hình cho một vai trò trong một tổ chức.
     *
     * @param organizationId ID tổ chức
     * @param roleId         ID vai trò
     * @return danh sách quyền đã cấu hình
     */
    List<OrganizationRolePermission> findByOrganization_OrganizationIdAndRole_RoleId(
            UUID organizationId,
            Integer roleId);

    /**
     * Lấy cấu hình của một permission cụ thể.
     *
     * @param organizationId ID tổ chức
     * @param roleId         ID vai trò
     * @param permissionId   ID quyền
     * @return cấu hình quyền nếu tồn tại
     */
    Optional<OrganizationRolePermission> findByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
            UUID organizationId,
            Integer roleId,
            Integer permissionId);

    /**
     * Kiểm tra permission đã được cấu hình hay chưa.
     *
     * @param organizationId ID tổ chức
     * @param roleId         ID vai trò
     * @param permissionId   ID quyền
     * @return true nếu đã cấu hình, ngược lại false
     */
    boolean existsByOrganization_OrganizationIdAndRole_RoleIdAndPermission_PermissionId(
            UUID organizationId,
            Integer roleId,
            Integer permissionId);
}
