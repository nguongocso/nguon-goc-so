package vn.nguongocso.loginanomaly.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.nguongocso.loginanomaly.entity.LoginAttempt;

/** Repository cho thực thể LoginAttempt. */
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Đếm số lần đăng nhập thất bại của một tài khoản kể từ một mốc thời gian.
     *
     * @param username tên đăng nhập
     * @param after    mốc thời gian (đầu cửa sổ theo dõi)
     * @return số lần thất bại trong cửa sổ
     */
    long countByUsernameAndFailedAtAfter(String username, LocalDateTime after);
}
