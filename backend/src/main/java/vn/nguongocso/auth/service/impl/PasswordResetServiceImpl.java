package vn.nguongocso.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.nguongocso.auth.dto.request.ForgotPasswordRequest;
import vn.nguongocso.auth.dto.request.ResetPasswordRequest;
import vn.nguongocso.auth.dto.response.ValidateResetTokenResponse;
import vn.nguongocso.auth.entity.PasswordResetToken;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.PasswordResetTokenRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.PasswordResetService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.mail.service.EmailService;

/**
 * Triển khai dịch vụ quên và đặt lại mật khẩu người dùng (NCL-01-CN-008).
 *
 * <p>
 * Cung cấp luồng bảo mật chống Account Enumeration, Rate Limiting theo giờ,
 * băm token SHA-256 trong cơ sở dữ liệu và tiêu thụ token atomic chống race condition.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int EXPIRY_MINUTES = 30;
    private static final int MAX_REQUESTS_PER_HOUR = 5;
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final String HASH_ALGORITHM = "SHA-256";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Tiếp nhận yêu cầu đặt lại mật khẩu, sinh token bảo mật và gửi email.
     *
     * @param request chứa email hoặc username
     */
    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String identifier = request.getEmailOrUsername() != null ? request.getEmailOrUsername().trim() : "";
        if (identifier.isBlank()) {
            return;
        }

        Optional<User> userOptional = findUserByIdentifier(identifier);
        if (userOptional.isEmpty()) {
            log.info("Yêu cầu đặt lại mật khẩu cho tài khoản không tồn tại trong hệ thống");
            return;
        }

        User user = userOptional.get();
        validateUserForPasswordReset(user);

        if (isRateLimitExceeded(user.getUserId())) {
            return;
        }

        invalidateOldTokens(user.getUserId());

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        saveNewPasswordResetToken(user, tokenHash);

        String resetUrl = buildResetPasswordUrl(rawToken);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetUrl, EXPIRY_MINUTES);
        log.info("Đã tạo token và kích hoạt gửi email đặt lại mật khẩu cho userId={}", user.getUserId());
    }

    /**
     * Xác thực tính hợp lệ và thời hạn của token đặt lại mật khẩu.
     *
     * @param token chuỗi token từ liên kết email
     * @return phản hồi trạng thái hợp lệ
     */
    @Override
    @Transactional(readOnly = true)
    public ValidateResetTokenResponse validateToken(String token) {
        if (token == null || token.isBlank()) {
            return ValidateResetTokenResponse.builder()
                    .valid(false)
                    .message("Mã xác thực không hợp lệ")
                    .build();
        }

        String tokenHash = hashToken(token);
        Optional<PasswordResetToken> tokenOptional =
                tokenRepository.findByTokenHashAndIsUsedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now());

        if (tokenOptional.isEmpty()) {
            return ValidateResetTokenResponse.builder()
                    .valid(false)
                    .message("Liên kết đặt lại mật khẩu đã hết hạn hoặc không còn hiệu lực")
                    .build();
        }

        return ValidateResetTokenResponse.builder()
                .valid(true)
                .message("Liên kết hợp lệ")
                .build();
    }

    /**
     * Xác thực token và cập nhật mật khẩu mới cho người dùng.
     *
     * @param request chứa token, mật khẩu mới và xác nhận mật khẩu
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());

        String tokenHash = hashToken(request.getToken());
        PasswordResetToken resetToken = consumeTokenOrThrow(tokenHash);
        User user = resetToken.getUser();

        validateNewPasswordNotSameAsOld(request.getNewPassword(), user.getPasswordHash());
        updatePasswordAndSaveUser(user, request.getNewPassword());
        invalidateAllRemainingTokens(user.getUserId());

        log.info("Đặt lại mật khẩu thành công cho userId={}", user.getUserId());
    }

    private Optional<User> findUserByIdentifier(String identifier) {
        Optional<User> userByEmail = userRepository.findByEmail(identifier);
        if (userByEmail.isPresent()) {
            return userByEmail;
        }
        return userRepository.findByUserName(identifier);
    }

    private void validateUserForPasswordReset(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Tài khoản userId={} không có địa chỉ email để nhận liên kết", user.getUserId());
            throw new BusinessException(
                    "Tài khoản chưa được cập nhật địa chỉ email trên hệ thống để thực hiện đặt lại mật khẩu."
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Tài khoản userId={} không ở trạng thái ACTIVE (status={})", user.getUserId(), user.getStatus());
            throw new BusinessException("Tài khoản đang bị khóa hoặc ngưng hoạt động. Vui lòng liên hệ quản trị viên.");
        }
    }

    private boolean isRateLimitExceeded(UUID userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = tokenRepository.countByUser_UserIdAndCreatedAtAfter(userId, oneHourAgo);
        if (recentCount >= MAX_REQUESTS_PER_HOUR) {
            log.warn("Tài khoản userId={} đã đạt giới hạn yêu cầu trong 1 giờ (count={})", userId, recentCount);
            return true;
        }
        return false;
    }

    private void invalidateOldTokens(UUID userId) {
        List<PasswordResetToken> oldTokens = tokenRepository.findByUser_UserIdAndIsUsedFalse(userId);
        if (!oldTokens.isEmpty()) {
            for (PasswordResetToken oldToken : oldTokens) {
                oldToken.setUsed(true);
            }
            tokenRepository.saveAll(oldTokens);
        }
    }

    private void saveNewPasswordResetToken(User user, String tokenHash) {
        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES))
                .isUsed(false)
                .build();

        tokenRepository.save(tokenEntity);
    }

    private String buildResetPasswordUrl(String rawToken) {
        String baseUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        return baseUrl + "/reset-password?token=" + rawToken;
    }

    private void validatePasswordConfirmation(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("Xác nhận mật khẩu mới không khớp");
        }
    }

    private PasswordResetToken consumeTokenOrThrow(String tokenHash) {
        int updatedRows = tokenRepository.consumeToken(tokenHash, LocalDateTime.now());
        if (updatedRows == 0) {
            throw new BusinessException("Liên kết đặt lại mật khẩu đã hết hạn hoặc không còn hiệu lực");
        }

        return tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Liên kết đặt lại mật khẩu không hợp lệ"));
    }

    private void validateNewPasswordNotSameAsOld(String newPassword, String oldPasswordHash) {
        if (passwordEncoder.matches(newPassword, oldPasswordHash)) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }
    }

    private void updatePasswordAndSaveUser(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void invalidateAllRemainingTokens(UUID userId) {
        List<PasswordResetToken> remainingTokens = tokenRepository.findByUser_UserIdAndIsUsedFalse(userId);
        if (!remainingTokens.isEmpty()) {
            for (PasswordResetToken remainingToken : remainingTokens) {
                remainingToken.setUsed(true);
            }
            tokenRepository.saveAll(remainingTokens);
        }
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng trên hệ thống", e);
        }
    }
}
