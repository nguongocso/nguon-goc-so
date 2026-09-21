package vn.nguongocso.auth.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.nguongocso.auth.dto.request.ForgotPasswordRequest;
import vn.nguongocso.auth.dto.request.LoginRequest;
import vn.nguongocso.auth.dto.request.ResetPasswordRequest;
import vn.nguongocso.auth.dto.request.SelectOrganizationRequest;
import vn.nguongocso.auth.dto.request.UpdateUserProfileRequest;
import vn.nguongocso.auth.dto.response.LoginResponse;
import vn.nguongocso.auth.dto.response.OrganizationSelectionResponse;
import vn.nguongocso.auth.dto.response.SelectOrganizationResponse;
import vn.nguongocso.auth.dto.response.UserProfileResponse;
import vn.nguongocso.auth.dto.response.ValidateResetTokenResponse;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.AuthService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.PasswordResetService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.permission.service.PermissionChecker;

/**
 * REST controller cung cấp các API liên quan đến xác thực người dùng.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
        private final AuthService authService;
        private final PasswordResetService passwordResetService;
        private final PermissionChecker permissionChecker;
        private final UserRepository userRepository;

        /**
         * Xác thực người dùng bằng username và password.
         */
        @PostMapping("/login")
        public ResponseEntity<ApiResult<LoginResponse>> login(
                        @Valid @RequestBody LoginRequest request) {

                return ResponseEntity.ok(
                                ApiResult.success(
                                                authService.login(request)));
        }

        /**
         * Tiếp nhận yêu cầu đặt lại mật khẩu và gửi email hướng dẫn (NCL-01-CN-008).
         * Luôn trả về 200 OK để chống dò quét tài khoản.
         */
        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResult<Void>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {

                passwordResetService.requestPasswordReset(request);
                return ResponseEntity.ok(
                                ApiResult.success(200, null));
        }

        /**
         * Kiểm tra token đặt lại mật khẩu có hợp lệ và còn thời hạn hay không
         * (NCL-01-CN-008).
         */
        @GetMapping("/reset-password/validate")
        public ResponseEntity<ApiResult<ValidateResetTokenResponse>> validateResetToken(
                        @RequestParam("token") String token) {

                return ResponseEntity.ok(
                                ApiResult.success(
                                                passwordResetService.validateToken(token)));
        }

        /**
         * Đặt lại mật khẩu mới bằng token đã xác thực (NCL-01-CN-008).
         */
        @PostMapping("/reset-password")
        public ResponseEntity<ApiResult<Void>> resetPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {

                passwordResetService.resetPassword(request);
                return ResponseEntity.ok(
                                ApiResult.success(200, null));
        }

        /**
         * Lấy thông tin profile của user hiện tại.
         */
        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<UserProfileResponse>> getCurrentUser() {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                List<String> permissions = permissionChecker.getPermissionsForCurrentUser();

                User user = userRepository.findById(userDetails.getUserId())
                                .orElse(userDetails.getUser());

                UserProfileResponse response = UserProfileResponse.builder()
                                .id(userDetails.getUserId())
                                .userId(userDetails.getUserId())
                                .username(userDetails.getUsername())
                                .fullName(user != null ? user.getFullName() : userDetails.getFullName())
                                .phone(user != null ? user.getPhone() : null)
                                .email(user != null ? user.getEmail() : null)
                                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                                .roleCode(userDetails.getRoleCode())
                                .roleName(userDetails.getRoleName())
                                .organizationId(userDetails.getOrganizationId())
                                .organizationCode(userDetails.getOrganizationCode())
                                .organizationName(userDetails.getOrganizationName())
                                .organizationType(userDetails.getOrganizationType())
                                .permissions(permissions)
                                .createdAt(user != null ? user.getCreatedAt() : null)
                                .updatedAt(user != null ? user.getUpdatedAt() : null)
                                .build();

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }

        /**
         * Cập nhật thông tin hồ sơ cá nhân của user hiện tại.
         */
        @PutMapping("/profile")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<UserProfileResponse>> updateProfile(
                        @Valid @RequestBody UpdateUserProfileRequest request) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                List<String> permissions = permissionChecker.getPermissionsForCurrentUser();

                UserProfileResponse response = authService.updateProfile(
                                userDetails.getUserId(),
                                userDetails,
                                request,
                                permissions);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }

        /**
         * Lấy danh sách organization mà user có thể lựa chọn.
         */
        @GetMapping("/organizations")
        public ResponseEntity<ApiResult<List<OrganizationSelectionResponse>>> getOrganizations(
                        @RequestHeader(value = "Authorization", required = false) String authorization) {

                if (authorization == null
                                || !authorization.startsWith("Bearer ")) {

                        throw new BusinessException(
                                        "Thiếu Authorization Bearer token");
                }

                String selectionToken = authorization.substring("Bearer ".length());

                return ResponseEntity.ok(
                                ApiResult.success(
                                                authService.getOrganizations(selectionToken)));
        }

        /**
         * Lấy danh sách tất cả organization mà user hiện tại
         * đang có quyền tham gia.
         */
        @GetMapping("/my-organizations")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<List<OrganizationSelectionResponse>>> getMyOrganizations() {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                return ResponseEntity.ok(
                                ApiResult.success(
                                                authService.getOrganizationsForUser(
                                                                userDetails.getUserId())));
        }

        /**
         * Lựa chọn organization mà user muốn sử dụng.
         */
        @PostMapping("/select-organization")
        public ResponseEntity<ApiResult<SelectOrganizationResponse>> selectOrganization(
                        @Valid @RequestBody SelectOrganizationRequest request,
                        @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

                if (authorizationHeader == null
                                || !authorizationHeader.startsWith("Bearer ")) {

                        throw new BusinessException(
                                        "Thiếu Selection Token");
                }

                String selectionToken = authorizationHeader.substring("Bearer ".length());

                SelectOrganizationResponse response = authService.selectOrganization(
                                selectionToken,
                                request);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }

        /**
         * Chuyển đổi sang organization khác khi user đã đăng nhập.
         */
        @PostMapping("/switch-organization")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<SelectOrganizationResponse>> switchOrganization(
                        @Valid @RequestBody SelectOrganizationRequest request) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                return ResponseEntity.ok(
                                ApiResult.success(
                                                authService.switchOrganization(
                                                                userDetails.getUserId(),
                                                                request)));
        }
}