package vn.nguongocso.permission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.permission.entity.RolePermission;

/**
 * Repository thao tác RolePermission.
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    /**
     * Lấy toàn bộ quyền đã cấu hình cho một vai trò.
     *
     * @param roleId ID vai trò
     * @return danh sách quyền đã cấu hình
     */
    List<RolePermission> findByRole_RoleId(Integer roleId);

    /**
     * Lấy cấu hình của một permission cụ thể.
     *
     * @param roleId       ID vai trò
     * @param permissionId ID quyền
     * @return cấu hình quyền nếu tồn tại
     */
    Optional<RolePermission> findByRole_RoleIdAndPermission_PermissionId(
            Integer roleId,
            Integer permissionId);

    /**
     * Kiểm tra permission đã được cấu hình hay chưa.
     *
     * @param roleId       ID vai trò
     * @param permissionId ID quyền
     * @return true nếu đã cấu hình, ngược lại false
     */
    boolean existsByRole_RoleIdAndPermission_PermissionId(
            Integer roleId,
            Integer permissionId);
}
