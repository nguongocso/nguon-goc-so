package vn.nguongocso.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Kiểm tra sự tồn tại của người dùng theo email ngoại trừ userId chỉ định.
     */
    boolean existsByEmailAndUserIdNot(String email, UUID userId);

    /**
     * Tìm kiếm người dùng theo tên đăng nhập.
     */
    Optional<User> findByUserName(String userName);

    /**
     * Tìm kiếm người dùng theo email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Tìm kiếm người dùng theo số điện thoại.
     */
    boolean existsByPhone(String phone);

    /**
     * Trang danh sách tài khoản đang có ít nhất một membership ACTIVE với vai
     * trò {@code roleCode} (NCL-743: liệt kê cán bộ quản lý ngành VT-05).
     *
     * @param roleCode mã vai trò (VD: VT-05)
     * @param keyword  từ khoá khớp fullName/username; truyền chuỗi rỗng để bỏ lọc
     */
    @Query("""
                SELECT DISTINCT u FROM User u
                JOIN OrganizationUser ou ON ou.user = u
                JOIN ou.role r
                WHERE r.code = :roleCode
                  AND ou.status = 'ACTIVE'
                  AND (:keyword = ''
                       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                       OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                ORDER BY u.fullName ASC, u.userName ASC
            """)
    Page<User> findUsersByActiveRoleCode(
            @Param("roleCode") String roleCode,
            @Param("keyword") String keyword,
            Pageable pageable);
}
