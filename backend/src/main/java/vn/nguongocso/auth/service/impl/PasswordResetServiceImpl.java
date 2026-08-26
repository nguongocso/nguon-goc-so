package vn.nguongocso.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int EXPIRY_MINUTES = 30;
    private static final int MAX_REQUESTS_PER_HOUR = 5;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String identifier = request.getEmailOrUsername() != null ? request.getEmailOrUsername().trim() : "";
        if (identifier.isBlank()) {
            return;
        }

        // Tìm kiếm theo email trước, nếu không có thì tìm theo username
        Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUserName(identifier);
        }

        // Chống Account Enumeration: Nếu user không tồn tại hoặc không có email, âm thầm return
        if (userOpt.isEmpty()) {
            log.info("Yêu cầu đặt lại mật khẩu cho tài khoản không tồn tại trong hệ thống");
            return;
        }

        User user = userOpt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Tài khoản userId={} không có địa chỉ email để nhận liên kết", user.getUserId());
            return;
        }

        // Không gửi email cho tài khoản đã bị vô hiệu hóa
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Tài khoản userId={} không ở trạng thái ACTIVE (status={})", user.getUserId(), user.getStatus());
            return;
        }

        // Kiểm tra Rate Limiting (chống spam/bruteforce)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = tokenRepository.countByUser_UserIdAndCreatedAtAfter(user.getUserId(), oneHourAgo);
        if (recentCount >= MAX_REQUESTS_PER_HOUR) {
            log.warn("Tài khoản userId={} đã đạt giới hạn yêu cầu đặt lại mật khẩu trong 1 giờ (count={})",
                    user.getUserId(), recentCount);
            return;
        }

        // Vô hiệu hóa các token cũ chưa sử dụng của user này
        List<PasswordResetToken> oldTokens = tokenRepository.findByUser_UserIdAndIsUsedFalse(user.getUserId());
        if (!oldTokens.isEmpty()) {
            for (PasswordResetToken oldToken : oldTokens) {
                oldToken.setUsed(true);
            }
            tokenRepository.saveAll(oldTokens);
        }

        // Sinh token ngẫu nhiên bảo mật 256-bit entropy
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES))
                .isUsed(false)
                .build();

        tokenRepository.save(tokenEntity);

        // Xây dựng đường dẫn và gửi email
        String baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        String resetUrl = baseUrl + "/reset-password?token=" + rawToken;

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetUrl, EXPIRY_MINUTES);
        log.info("Đã tạo token và kích hoạt gửi email đặt lại mật khẩu cho userId={}", user.getUserId());
    }

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
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenHashAndIsUsedFalseAndExpiresAtAfter(
                tokenHash, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
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

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Xác nhận mật khẩu mới không khớp");
        }

        String tokenHash = hashToken(request.getToken());

        // Tiêu thụ token atomic để chống race condition (chỉ 1 request thành công duy nhất)
        int updatedRows = tokenRepository.consumeToken(tokenHash, LocalDateTime.now());
        if (updatedRows == 0) {
            throw new BusinessException("Liên kết đặt lại mật khẩu đã hết hạn hoặc không còn hiệu lực");
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Liên kết đặt lại mật khẩu không hợp lệ"));

        User user = resetToken.getUser();

        // Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        // Cập nhật mật khẩu mới được băm bằng BCrypt
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Vô hiệu hóa tất cả các token còn lại của user
        List<PasswordResetToken> remainingTokens = tokenRepository.findByUser_UserIdAndIsUsedFalse(user.getUserId());
        if (!remainingTokens.isEmpty()) {
            for (PasswordResetToken t : remainingTokens) {
                t.setUsed(true);
            }
            tokenRepository.saveAll(remainingTokens);
        }

        log.info("Đặt lại mật khẩu thành công cho userId={}", user.getUserId());
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
            throw new RuntimeException("Thuật toán SHA-256 không khả dụng trên hệ thống", e);
        }
    }
}
