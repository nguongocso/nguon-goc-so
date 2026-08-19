package vn.nguongocso.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.auth.entity.User;

/**
 * Repository cho thực thể User.
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Kiểm tra sự tồn tại của người dùng theo tên đăng nhập.
     */
    boolean existsByUserName(String userName);

    /**
     * Kiểm tra sự tồn tại của người dùng theo email.
     */
    boolean existsByEmail(String email);

    /**
     * Tìm kiếm người dùng theo tên đăng nhập.
     */
    Optional<User> findByUserName(String userName);

    /**
     * Tìm kiếm người dùng theo email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Kiểm tra sự tồn tại của người dùng theo số điện thoại.
     */
    boolean existsByPhone(String phone);
}
