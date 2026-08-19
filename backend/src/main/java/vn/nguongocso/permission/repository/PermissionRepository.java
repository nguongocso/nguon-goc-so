package vn.nguongocso.permission.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.permission.entity.Permission;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho thực thể Permission.
 */
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    /**
     * Lấy tất cả quyền theo nhóm chức năng (resource).
     */
    List<Permission> findByResource(String resource);

    /**
     * Lấy một quyền theo resource và action.
     */
    Optional<Permission> findByResourceAndAction(
            String resource,
            String action
    );

    /**
     * Kiểm tra quyền đã tồn tại hay chưa.
     */
    boolean existsByResourceAndAction(
            String resource,
            String action
    );
}