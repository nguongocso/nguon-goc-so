package vn.nguongocso.unit.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import vn.nguongocso.auth.dto.request.ForgotPasswordRequest;
import vn.nguongocso.auth.dto.request.ResetPasswordRequest;
import vn.nguongocso.auth.dto.response.ValidateResetTokenResponse;
import vn.nguongocso.auth.entity.PasswordResetToken;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.PasswordResetTokenRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.impl.PasswordResetServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.mail.service.EmailService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User activeUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.builder()
                .userId(userId)
                .userName("nongdan01")
                .fullName("Nguyen Van A")
                .email("nongdan@nguongocso.vn")
                .passwordHash("$2a$10$OldPasswordHashedValue")
                .status(UserStatus.ACTIVE)
                .build();

        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("Yêu cầu đặt lại MK thành công khi tài khoản tồn tại và có email")
    void requestPasswordReset_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nongdan@nguongocso.vn");

        when(userRepository.findByEmail("nongdan@nguongocso.vn")).thenReturn(Optional.of(activeUser));
        when(tokenRepository.countByUser_UserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0L);
        when(tokenRepository.findByUser_UserIdAndIsUsedFalse(userId)).thenReturn(Collections.emptyList());

        passwordResetService.requestPasswordReset(request);

        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(
                eq("nongdan@nguongocso.vn"),
                eq("Nguyen Van A"),
                contains("http://localhost:3000/reset-password?token="),
                eq(30)
        );
    }

    @Test
    @DisplayName("Chống dò quét tài khoản: Không gửi mail và không ném lỗi khi user không tồn tại")
    void requestPasswordReset_UserNotFound_SilentSuccess() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@gmail.com");

        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUserName("unknown@gmail.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(request));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Báo lỗi khi tài khoản chưa được cấu hình địa chỉ email")
    void requestPasswordReset_UserHasNoEmail_ThrowsBusinessException() {
        User userWithoutEmail = User.builder()
                .userId(UUID.randomUUID())
                .userName("nongdan_no_email")
                .fullName("No Email User")
                .email(null)
                .status(UserStatus.ACTIVE)
                .build();

        ForgotPasswordRequest request = new ForgotPasswordRequest("nongdan_no_email");

        when(userRepository.findByEmail("nongdan_no_email")).thenReturn(Optional.empty());
        when(userRepository.findByUserName("nongdan_no_email")).thenReturn(Optional.of(userWithoutEmail));

        BusinessException ex = assertThrows(BusinessException.class, () -> passwordResetService.requestPasswordReset(request));
        assertEquals("Tài khoản chưa được cấu hình địa chỉ email để thực hiện đặt lại mật khẩu. Vui lòng liên hệ quản trị viên.", ex.getMessage());

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Rate limiting: Chặn gửi email khi yêu cầu vượt quá 5 lần/giờ")
    void requestPasswordReset_RateLimitExceeded() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nongdan@nguongocso.vn");

        when(userRepository.findByEmail("nongdan@nguongocso.vn")).thenReturn(Optional.of(activeUser));
        when(tokenRepository.countByUser_UserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(5L);

        passwordResetService.requestPasswordReset(request);

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Vô hiệu hóa các token cũ chưa sử dụng khi tạo token mới")
    void requestPasswordReset_InvalidatesOldTokens() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nongdan01");

        PasswordResetToken oldToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .tokenHash("oldhash")
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();

        when(userRepository.findByEmail("nongdan01")).thenReturn(Optional.empty());
        when(userRepository.findByUserName("nongdan01")).thenReturn(Optional.of(activeUser));
        when(tokenRepository.countByUser_UserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0L);
        when(tokenRepository.findByUser_UserIdAndIsUsedFalse(userId)).thenReturn(List.of(oldToken));

        passwordResetService.requestPasswordReset(request);

        assertTrue(oldToken.isUsed());
        verify(tokenRepository).saveAll(List.of(oldToken));
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("Validate token hợp lệ: trả về valid = true")
    void validateToken_Valid() {
        when(tokenRepository.findByTokenHashAndIsUsedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(PasswordResetToken.builder().build()));

        ValidateResetTokenResponse response = passwordResetService.validateToken("some-valid-token");

        assertTrue(response.isValid());
        assertEquals("Liên kết hợp lệ", response.getMessage());
    }

    @Test
    @DisplayName("Validate token không hợp lệ hoặc hết hạn: trả về valid = false")
    void validateToken_InvalidOrExpired() {
        when(tokenRepository.findByTokenHashAndIsUsedFalseAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        ValidateResetTokenResponse response = passwordResetService.validateToken("expired-token");

        assertFalse(response.isValid());
        assertEquals("Liên kết đặt lại mật khẩu đã hết hạn hoặc không còn hiệu lực", response.getMessage());
    }

    @Test
    @DisplayName("Reset password thành công: mã hóa mật khẩu mới và cập nhật user")
    void resetPassword_Success() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-raw-token")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .tokenHash("hashedtoken")
                .isUsed(true)
                .expiresAt(LocalDateTime.now().plusMinutes(25))
                .build();

        when(tokenRepository.consumeToken(anyString(), any(LocalDateTime.class))).thenReturn(1);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("NewPass@123", activeUser.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("$2a$10$NewHashedPasswordValue");

        assertDoesNotThrow(() -> passwordResetService.resetPassword(request));

        assertEquals("$2a$10$NewHashedPasswordValue", activeUser.getPasswordHash());
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("Reset password thất bại khi mật khẩu xác nhận không khớp")
    void resetPassword_PasswordMismatch() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-raw-token")
                .newPassword("NewPass@123")
                .confirmPassword("MismatchPass@123")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> passwordResetService.resetPassword(request));
        assertEquals("Xác nhận mật khẩu mới không khớp", ex.getMessage());
        verify(tokenRepository, never()).consumeToken(any(), any());
    }

    @Test
    @DisplayName("Reset password thất bại khi mật khẩu mới trùng mật khẩu cũ")
    void resetPassword_SameAsOldPassword() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-raw-token")
                .newPassword("OldPassword@123")
                .confirmPassword("OldPassword@123")
                .build();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .tokenHash("hashedtoken")
                .build();

        when(tokenRepository.consumeToken(anyString(), any(LocalDateTime.class))).thenReturn(1);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("OldPassword@123", activeUser.getPasswordHash())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> passwordResetService.resetPassword(request));
        assertEquals("Mật khẩu mới không được trùng với mật khẩu hiện tại", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Atomic Token Concurrency: Không cho phép đổi mật khẩu khi token đã bị tiêu thụ trước đó")
    void resetPassword_TokenAlreadyConsumed() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("already-used-token")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .build();

        // Giả lập token đã bị request khác consume trước -> affectedRows = 0
        when(tokenRepository.consumeToken(anyString(), any(LocalDateTime.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> passwordResetService.resetPassword(request));
        assertEquals("Liên kết đặt lại mật khẩu đã hết hạn hoặc không còn hiệu lực", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}
