package vn.nguongocso.permission.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.nguongocso.permission.entity.RolePermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository thao tác RolePermission.
 */
public interface RolePermissionRepository
                extends JpaRepository<RolePermission, UUID> {
        /**
         * Lấy toàn bộ quyền đã cấu hình cho một vai trò.
         */
        List<RolePermission> findByRole_RoleId(Integer roleId);

        /**
         * Lấy cấu hình của một permission cụ thể.
         */
        Optional<RolePermission> findByRole_RoleIdAndPermission_PermissionId(
                        Integer roleId,
                        Integer permissionId);

        /**
         * Kiểm tra permission đã được cấu hình hay chưa.
         */
        boolean existsByRole_RoleIdAndPermission_PermissionId(
                        Integer roleId,
                        Integer permissionId);
}