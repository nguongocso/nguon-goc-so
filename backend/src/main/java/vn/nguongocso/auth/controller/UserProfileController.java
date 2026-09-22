package vn.nguongocso.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.nguongocso.auth.dto.request.ChangePasswordRequest;
import vn.nguongocso.auth.dto.request.UpdateUserProfileRequest;
import vn.nguongocso.auth.dto.response.AvatarUploadResponse;
import vn.nguongocso.auth.dto.response.UserProfileResponse;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.UserService;
import vn.nguongocso.common.ApiResult;

/**
 * Controller quản lý hồ sơ cá nhân và đổi mật khẩu chủ động của người dùng (NCL-01-CN-010).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {
        private final UserService userService;

        /**
         * Lấy thông tin hồ sơ của người dùng hiện tại.
         */
        @GetMapping("/profile")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<UserProfileResponse>> getProfile(
                        @AuthenticationPrincipal CustomUserDetails currentUser) {
                return ResponseEntity.ok(
                                ApiResult.success(userService.getCurrentUserProfile(currentUser)));
        }

        /**
         * Cập nhật thông tin hồ sơ cá nhân của người dùng hiện tại.
         */
        @PutMapping("/profile")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<UserProfileResponse>> updateProfile(
                        @AuthenticationPrincipal CustomUserDetails currentUser,
                        @Valid @RequestBody UpdateUserProfileRequest request) {
                return ResponseEntity.ok(
                                ApiResult.success(userService.updateUserProfile(currentUser, request)));
        }

        /**
         * Thay đổi mật khẩu chủ động của người dùng.
         */
        @PostMapping("/change-password")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<Void>> changePassword(
                        @AuthenticationPrincipal CustomUserDetails currentUser,
                        @Valid @RequestBody ChangePasswordRequest request) {
                userService.changePassword(currentUser, request);
                return ResponseEntity.ok(
                                ApiResult.success(200, null));
        }

        /**
         * Tải lên ảnh đại diện cá nhân.
         */
        @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<AvatarUploadResponse>> uploadAvatar(
                        @AuthenticationPrincipal CustomUserDetails currentUser,
                        @RequestParam("file") MultipartFile file) {
                return ResponseEntity.ok(
                                ApiResult.success(userService.uploadAvatar(currentUser, file)));
        }
}
