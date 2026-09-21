package vn.nguongocso.permission.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.permission.entity.Permission;

/**
 * Repository cho thực thể Permission.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    /**
     * Lấy tất cả quyền theo nhóm chức năng (resource).
     *
     * @param resource tên nhóm chức năng
     * @return danh sách quyền
     */
    List<Permission> findByResource(String resource);

    /**
     * Lấy một quyền theo resource và action.
     *
     * @param resource tên nhóm chức năng
     * @param action   tên hành động
     * @return quyền tương ứng nếu tồn tại
     */
    Optional<Permission> findByResourceAndAction(String resource, String action);

    /**
     * Kiểm tra quyền đã tồn tại hay chưa.
     *
     * @param resource tên nhóm chức năng
     * @param action   tên hành động
     * @return true nếu đã tồn tại, ngược lại false
     */
    boolean existsByResourceAndAction(String resource, String action);
}
