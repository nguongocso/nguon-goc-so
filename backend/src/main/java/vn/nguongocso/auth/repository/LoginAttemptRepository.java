package vn.nguongocso.auth.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.auth.entity.LoginAttempt;
import vn.nguongocso.auth.enums.LoginResult;

/**
 * Repository quản lý các bản ghi đăng nhập.
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {
    
    /**
     * Lấy lịch sử đăng nhập của một người dùng, sắp xếp theo thời gian mới nhất trước.
     */
    Page<LoginAttempt> findByUser_UserIdOrderByCreatedAtDesc(
        UUID userId,
        Pageable pageable
    );
    
    /**
     * Lấy top 5 lần đăng nhập gần nhất có kết quả cho trước, sắp xếp mới nhất trước.
     * Dùng để kiểm tra bất thường (ví dụ: 5 lần FAILED gần nhất).
     */
    List<LoginAttempt> findTop5ByUser_UserIdAndResultOrderByCreatedAtDesc(
        UUID userId,
        LoginResult result
    );
    
    /**
     * Kiểm tra xem một tài khoản đã từng đăng nhập thành công từ quốc gia nào đó không.
     */
    boolean existsByUser_UserIdAndResultAndCountryCode(
        UUID userId,
        LoginResult result,
        String countryCode
    );
    
    /**
     * Lấy các lần đăng nhập FAILED của người dùng trong khoảng thời gian (dùng để phát hiện repeated failed login).
     */
    List<LoginAttempt> findByUser_UserIdAndResultAndCreatedAtAfterOrderByCreatedAtDesc(
        UUID userId,
        LoginResult result,
        OffsetDateTime startTime
    );
}
