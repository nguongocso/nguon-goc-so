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

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByTokenHashAndIsUsedFalseAndExpiresAtAfter(
            String tokenHash,
            LocalDateTime now
    );

    List<PasswordResetToken> findByUser_UserIdAndIsUsedFalse(UUID userId);

    long countByUser_UserIdAndCreatedAtAfter(UUID userId, LocalDateTime time);

    /**
     * Tiêu thụ token một cách atomic để chống race condition khi 2 request gửi đồng thời.
     * Trả về số dòng cập nhật (1 nếu thành công, 0 nếu token đã dùng hoặc hết hạn).
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.tokenHash = :tokenHash AND p.isUsed = false AND p.expiresAt > :now")
    int consumeToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );
}
