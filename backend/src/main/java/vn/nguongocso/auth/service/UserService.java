package vn.nguongocso.auth.service;

import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.auth.dto.request.ChangePasswordRequest;
import vn.nguongocso.auth.dto.request.UpdateUserProfileRequest;
import vn.nguongocso.auth.dto.response.AvatarUploadResponse;
import vn.nguongocso.auth.dto.response.UserProfileResponse;

/**
 * Service quản lý hồ sơ cá nhân và đổi mật khẩu chủ động của người dùng (NCL-01-CN-010).
 */
public interface UserService {
    /**
     * Lấy thông tin hồ sơ của người dùng hiện tại.
     */
    UserProfileResponse getCurrentUserProfile(CustomUserDetails currentUser);

    /**
     * Cập nhật thông tin hồ sơ cá nhân (họ tên, số điện thoại, email, ảnh đại diện).
     */
    UserProfileResponse updateUserProfile(CustomUserDetails currentUser, UpdateUserProfileRequest request);

    /**
     * Thay đổi mật khẩu chủ động của người dùng.
     */
    void changePassword(CustomUserDetails currentUser, ChangePasswordRequest request);

    /**
     * Tải lên ảnh đại diện cá nhân của người dùng.
     */
    AvatarUploadResponse uploadAvatar(CustomUserDetails currentUser, MultipartFile file);
}
