package vn.nguongocso.unit.auth;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import vn.nguongocso.auth.dto.request.ForgotPasswordRequest;
import vn.nguongocso.auth.dto.request.ResetPasswordRequest;
import vn.nguongocso.auth.entity.PasswordResetToken;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.PasswordResetTokenRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.PasswordResetService;
import vn.nguongocso.auth.service.impl.PasswordResetServiceImpl;
import vn.nguongocso.mail.service.EmailService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử tích hợp luồng quên và đặt lại mật khẩu người dùng (NCL-01-CN-008).
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private PasswordResetService passwordResetService;

    private User testUser;
    private UUID testUserId;
    private final List<PasswordResetToken> tokenDb = new ArrayList<>();

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetServiceImpl(
                userRepository,
                tokenRepository,
                emailService,
                passwordEncoder
        );
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:3000");

        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .userId(testUserId)
                .userName("farmer_test")
                .fullName("Farmer Test")
                .email("farmer@nguongocso.vn")
                .passwordHash(passwordEncoder.encode("OldPass@123"))
                .status(UserStatus.ACTIVE)
                .build();

        tokenDb.clear();
    }

    @Test
    @DisplayName("VERIFY 1 & 3: Old Token Invalidation & Email URL generation")
    void testOldTokenInvalidationAndEmailUrl() {
        when(userRepository.findByEmail("farmer@nguongocso.vn")).thenReturn(Optional.of(testUser));
        when(tokenRepository.countByUser_UserIdAndCreatedAtAfter(eq(testUserId), any())).thenReturn(0L);

        // Giả lập lưu token vào tokenDb
        doAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            tokenDb.add(token);
            return token;
        }).when(tokenRepository).save(any(PasswordResetToken.class));

        when(tokenRepository.findByUser_UserIdAndIsUsedFalse(testUserId)).thenAnswer(inv ->
                tokenDb.stream().filter(token -> !token.isUsed()).toList()
        );

        // Lần 1: Yêu cầu reset -> sinh Token 1
        passwordResetService.requestPasswordReset(new ForgotPasswordRequest("farmer@nguongocso.vn"));
        assertEquals(1, tokenDb.size());
        PasswordResetToken token1 = tokenDb.get(0);
        assertFalse(token1.isUsed());

        // Lần 2: Yêu cầu reset lần 2 -> Token 1 phải bị vô hiệu hóa (isUsed = true) và sinh Token 2
        passwordResetService.requestPasswordReset(new ForgotPasswordRequest("farmer@nguongocso.vn"));
        assertEquals(2, tokenDb.size());
        PasswordResetToken token2 = tokenDb.get(1);

        assertTrue(token1.isUsed(), "Token 1 phải bị vô hiệu hóa sau khi sinh Token 2");
        assertFalse(token2.isUsed(), "Token 2 mới tạo phải ở trạng thái hợp lệ");

        // Xác nhận EmailService được gọi đúng với URL chứa reset-password?token=
        verify(emailService, times(2)).sendPasswordResetEmail(
                eq("farmer@nguongocso.vn"),
                eq("Farmer Test"),
                contains("http://localhost:3000/reset-password?token="),
                eq(30)
        );
    }

    @Test
    @DisplayName("VERIFY 4: Full Flow - Đặt lại mật khẩu thành công & Đăng nhập bằng mật khẩu mới")
    void testFullResetPasswordFlow() {
        String newRawPassword = "NewSecurePass@2026";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid_token_string")
                .newPassword(newRawPassword)
                .confirmPassword(newRawPassword)
                .build();

        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash("hash_val")
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(25))
                .build();

        when(tokenRepository.consumeToken(anyString(), any(LocalDateTime.class))).thenReturn(1);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenEntity));

        // Thực hiện đổi mật khẩu
        passwordResetService.resetPassword(request);

        // Xác nhận mật khẩu mới đã được mã hóa bằng BCrypt
        assertTrue(passwordEncoder.matches(newRawPassword, testUser.getPasswordHash()),
                "Mật khẩu mới phải khớp khi kiểm tra với PasswordEncoder");

        // Xác nhận mật khẩu cũ không còn dùng được
        assertFalse(passwordEncoder.matches("OldPass@123", testUser.getPasswordHash()),
                "Mật khẩu cũ không được phép đăng nhập thành công");
    }

    @Test
    @DisplayName("VERIFY 5: Account Enumeration Protection")
    void testAccountEnumerationProtection() {
        when(userRepository.findByEmail("non_existing@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUserName("non_existing@example.com")).thenReturn(Optional.empty());

        // Gọi với email không tồn tại -> Không ném exception, không gửi email
        assertDoesNotThrow(() ->
                passwordResetService.requestPasswordReset(new ForgotPasswordRequest("non_existing@example.com"))
        );

        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
        verify(tokenRepository, never()).save(any());
    }
}
