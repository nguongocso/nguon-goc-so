package vn.nguongocso.unit.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.dto.request.ChangePasswordRequest;
import vn.nguongocso.auth.dto.request.UpdateUserProfileRequest;
import vn.nguongocso.auth.dto.response.AvatarUploadResponse;
import vn.nguongocso.auth.dto.response.UserProfileResponse;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.impl.UserServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.permission.service.PermissionChecker;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử đơn vị cho UserService (NCL-01-CN-010).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    @TempDir
    Path tempDir;

    private UUID userId;
    private UUID orgId;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .userName("farmer01")
                .fullName("Nguyễn Văn Nông Dân")
                .phone("0987654321")
                .email("farmer01@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(userId);
        lenient().when(userDetails.getUsername()).thenReturn("farmer01");
        lenient().when(userDetails.getFullName()).thenReturn("Nguyễn Văn Nông Dân");
        lenient().when(userDetails.getRoleCode()).thenReturn("VT-03");
        lenient().when(userDetails.getRoleName()).thenReturn("Người ghi sự kiện");
        lenient().when(userDetails.getOrganizationId()).thenReturn(orgId);
        lenient().when(userDetails.getOrganizationCode()).thenReturn("HTX_XANH");
        lenient().when(userDetails.getOrganizationName()).thenReturn("Hợp tác xã Nông nghiệp Xanh");
        lenient().when(userDetails.getOrganizationType()).thenReturn(OrganizationType.COOPERATIVE);

        ReflectionTestUtils.setField(userService, "baseDir", tempDir.toString());
        ReflectionTestUtils.setField(userService, "avatarRelativePath", "avatars");
        ReflectionTestUtils.setField(userService, "maxAvatarSize", 5242880L);
    }

    @Test
    @DisplayName("Lấy hồ sơ cá nhân thành công")
    void testGetCurrentUserProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(permissionChecker.getPermissionsForCurrentUser()).thenReturn(List.of("farm_log.CREATE"));

        UserProfileResponse response = userService.getCurrentUserProfile(userDetails);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("farmer01");
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn Nông Dân");
        assertThat(response.getEmail()).isEqualTo("farmer01@example.com");
        assertThat(response.getPhone()).isEqualTo("0987654321");
        assertThat(response.getPermissions()).contains("farm_log.CREATE");
    }

    @Test
    @DisplayName("Cập nhật số điện thoại và email thành công (TC-01)")
    void testUpdateUserProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndUserIdNot("new.email@example.com", userId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(permissionChecker.getPermissionsForCurrentUser()).thenReturn(List.of("farm_log.CREATE"));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .fullName("Nguyễn Văn Nông Dân Mới")
                .phone("0912345678")
                .email("new.email@example.com")
                .build();

        UserProfileResponse response = userService.updateUserProfile(userDetails, request);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn Nông Dân Mới");
        assertThat(response.getPhone()).isEqualTo("0912345678");
        assertThat(response.getEmail()).isEqualTo("new.email@example.com");
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    @DisplayName("Cập nhật với email đã tồn tại ở tài khoản khác thì ném lỗi")
    void testUpdateUserProfile_DuplicateEmail_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndUserIdNot("duplicate@example.com", userId)).thenReturn(true);

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .email("duplicate@example.com")
                .build();

        assertThatThrownBy(() -> userService.updateUserProfile(userDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Địa chỉ email đã được sử dụng");
    }

    @Test
    @DisplayName("Cập nhật hồ sơ không có trường nào thay đổi thì ném lỗi")
    void testUpdateUserProfile_NoChanges_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder().build();

        assertThatThrownBy(() -> userService.updateUserProfile(userDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không có dữ liệu thay đổi");
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công khi thông tin hợp lệ")
    void testChangePassword_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "$2a$10$hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@456", "$2a$10$hashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@456")).thenReturn("$2a$10$newHashedPassword");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword@123")
                .newPassword("NewPassword@456")
                .confirmNewPassword("NewPassword@456")
                .build();

        userService.changePassword(userDetails, request);

        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$newHashedPassword");
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại khi mật khẩu hiện tại không đúng (TC-02)")
    void testChangePassword_WrongCurrentPassword_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword")
                .newPassword("NewPassword@456")
                .confirmNewPassword("NewPassword@456")
                .build();

        assertThatThrownBy(() -> userService.changePassword(userDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mật khẩu hiện tại không chính xác");
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại khi mật khẩu xác nhận không khớp")
    void testChangePassword_PasswordMismatch_ThrowsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword@123")
                .newPassword("NewPassword@456")
                .confirmNewPassword("DifferentPassword@456")
                .build();

        assertThatThrownBy(() -> userService.changePassword(userDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mật khẩu xác nhận không khớp");
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại khi mật khẩu mới trùng mật khẩu cũ")
    void testChangePassword_SameAsOldPassword_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "$2a$10$hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("OldPassword@123", "$2a$10$hashedPassword")).thenReturn(true);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword@123")
                .newPassword("OldPassword@123")
                .confirmNewPassword("OldPassword@123")
                .build();

        assertThatThrownBy(() -> userService.changePassword(userDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mật khẩu mới không được trùng");
    }

    @Test
    @DisplayName("Tải lên ảnh đại diện hợp lệ thành công")
    void testUploadAvatar_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "dummy image content".getBytes()
        );

        AvatarUploadResponse response = userService.uploadAvatar(userDetails, file);

        assertThat(response).isNotNull();
        assertThat(response.getAvatarUrl()).contains("/uploads/avatars/");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Tải lên ảnh đại diện với loại tệp không hợp lệ thì ném lỗi")
    void testUploadAvatar_InvalidFileType_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        assertThatThrownBy(() -> userService.uploadAvatar(userDetails, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Định dạng tệp không hợp lệ");
    }

    @Test
    @DisplayName("Tải lên ảnh đại diện với tệp rỗng thì ném lỗi")
    void testUploadAvatar_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> userService.uploadAvatar(userDetails, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không được để trống");
    }
}
