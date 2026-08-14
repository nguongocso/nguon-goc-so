package vn.nguongocso.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.nguongocso.auth.entity.Role;

/**
 * Repository cho thực thể Role.
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {
    /**
     * Tìm kiếm vai trò theo mã.
     */
    Optional<Role> findByCode(String code);
}
