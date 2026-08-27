package vn.nguongocso.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.nguongocso.auth.entity.PasswordResetToken;

/**
 * Repository thao tác dữ liệu token đặt lại mật khẩu trong cơ sở dữ liệu.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Tìm kiếm bản ghi token theo chuỗi băm token hash.
     *
     * @param tokenHash chuỗi SHA-256 băm của token
     * @return Optional chứa thực thể token nếu tìm thấy
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Tìm kiếm token chưa sử dụng và chưa hết hạn theo token hash.
     *
     * @param tokenHash chuỗi SHA-256 băm của token
     * @param now       thời điểm hiện tại
     * @return Optional chứa thực thể token nếu hợp lệ
     */
    Optional<PasswordResetToken> findByTokenHashAndIsUsedFalseAndExpiresAtAfter(
            String tokenHash,
            LocalDateTime now
    );

    /**
     * Lấy danh sách các token chưa sử dụng của một người dùng.
     *
     * @param userId định danh người dùng
     * @return danh sách token còn hiệu lực
     */
    List<PasswordResetToken> findByUser_UserIdAndIsUsedFalse(UUID userId);

    /**
     * Đếm số lượng yêu cầu đặt lại mật khẩu được tạo sau một thời điểm nhất định.
     *
     * @param userId định danh người dùng
     * @param time   thời điểm mốc
     * @return số lượng yêu cầu
     */
    long countByUser_UserIdAndCreatedAtAfter(UUID userId, LocalDateTime time);

    /**
     * Tiêu thụ token một cách atomic để chống race condition khi 2 request gửi đồng thời.
     *
     * @param tokenHash chuỗi hash của token
     * @param now       thời điểm hiện tại
     * @return số dòng cập nhật (1 nếu thành công, 0 nếu token đã dùng hoặc hết hạn)
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true "
            + "WHERE p.tokenHash = :tokenHash AND p.isUsed = false AND p.expiresAt > :now")
    int consumeToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );
}
