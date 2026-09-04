package vn.nguongocso.auth.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 *
 * <p>
 * Controller này xử lý:
 * </p>
 * <ul>
 *     <li>Đăng nhập bằng username/password.</li>
 *     <li>Quên mật khẩu và đặt lại mật khẩu.</li>
 *     <li>Lấy thông tin user hiện tại.</li>
 *     <li>Lấy danh sách organization mà user có thể lựa chọn.</li>
 *     <li>Lựa chọn organization sau khi đăng nhập.</li>
 *     <li>Chuyển đổi organization khi đã đăng nhập.</li>
 * </ul>
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
     *
     * <p>
     * Bước này chỉ xác thực ở cấp User. Organization và Role
     * chưa được xác định tại bước đăng nhập.
     * </p>
     *
     * <p>
     * Nếu đăng nhập thành công, hệ thống trả về Selection JWT
     * để người dùng tiếp tục lựa chọn organization.
     * </p>
     *
     * @param request thông tin đăng nhập
     * @return thông tin user và Selection JWT
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResult<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                ApiResult.success(
                        authService.login(request)
                )
        );
    }

    /**
     * Tiếp nhận yêu cầu đặt lại mật khẩu và gửi email hướng dẫn (NCL-01-CN-008).
     * Luôn trả về 200 OK để chống dò quét tài khoản.
     *
     * @param request chứa email hoặc username
     * @return kết quả thông báo chung
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResult<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(
                ApiResult.success(200, null)
        );
    }

    /**
     * Kiểm tra token đặt lại mật khẩu có hợp lệ và còn thời hạn hay không (NCL-01-CN-008).
     *
     * @param token chuỗi token từ URL
     * @return trạng thái hợp lệ của token
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<ApiResult<ValidateResetTokenResponse>> validateResetToken(
            @RequestParam("token") String token) {

        return ResponseEntity.ok(
                ApiResult.success(
                        passwordResetService.validateToken(token)
                )
        );
    }

    /**
     * Đặt lại mật khẩu mới bằng token đã xác thực (NCL-01-CN-008).
     *
     * @param request chứa token, mật khẩu mới và xác nhận mật khẩu
     * @return kết quả đặt lại mật khẩu
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResult<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResult.success(200, null)
        );
    }

    /**
     * Lấy thông tin profile của user hiện tại.
     *
     * <p>
     * User phải được xác thực bằng Access JWT.
     * </p>
     *
     * @return thông tin user hiện tại
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserProfileResponse>> getCurrentUser() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) auth.getPrincipal();

        List<String> permissions =
                permissionChecker.getPermissionsForCurrentUser();

        // Lấy thông tin mới nhất từ DB
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
                ApiResult.success(response)
        );
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân của user hiện tại.
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) auth.getPrincipal();

        List<String> permissions =
                permissionChecker.getPermissionsForCurrentUser();

        UserProfileResponse response = authService.updateProfile(
                userDetails.getUserId(),
                userDetails,
                request,
                permissions
        );

        return ResponseEntity.ok(
                ApiResult.success(response)
        );
    }

    /**
     * Lấy danh sách organization mà user có thể lựa chọn.
     *
     * <p>
     * Endpoint này sử dụng Selection JWT được cấp sau khi
     * username/password authentication thành công.
     * </p>
     *
     * <p>
     * Selection JWT chưa tạo SecurityContext nên endpoint này
     * không sử dụng {@code @PreAuthorize("isAuthenticated()")}.
     * </p>
     *
     * @param authorization Authorization header chứa Selection JWT
     * @return danh sách organization của user
     */
    @GetMapping("/organizations")
    public ResponseEntity<ApiResult<List<OrganizationSelectionResponse>>> getOrganizations(
            @RequestHeader(value = "Authorization", required = false)
            String authorization) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            throw new BusinessException(
                    "Thiếu Authorization Bearer token"
            );
        }

        String selectionToken =
                authorization.substring("Bearer ".length());

        return ResponseEntity.ok(
                ApiResult.success(
                        authService.getOrganizations(selectionToken)
                )
        );
    }

    /**
     * Lấy danh sách tất cả organization mà user hiện tại
     * đang có quyền tham gia.
     *
     * <p>
     * Endpoint này yêu cầu Access JWT hợp lệ.
     * </p>
     *
     * @return danh sách organization của user
     */
    @GetMapping("/my-organizations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<List<OrganizationSelectionResponse>>> getMyOrganizations() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) auth.getPrincipal();

        return ResponseEntity.ok(
                ApiResult.success(
                        authService.getOrganizationsForUser(
                                userDetails.getUserId()
                        )
                )
        );
    }

    /**
     * Lựa chọn organization mà user muốn sử dụng.
     *
     * <p>
     * Endpoint này sử dụng Selection JWT được cấp sau khi
     * username/password authentication thành công.
     * </p>
     *
     * <p>
     * Sau khi lựa chọn organization thành công, hệ thống
     * cấp Access JWT chứa thông tin organization và role.
     * </p>
     *
     * @param request thông tin organization cần lựa chọn
     * @param authorizationHeader Authorization header chứa Selection JWT
     * @return Access JWT và thông tin user trong organization đã chọn
     */
    @PostMapping("/select-organization")
    public ResponseEntity<ApiResult<SelectOrganizationResponse>> selectOrganization(
            @Valid @RequestBody SelectOrganizationRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            throw new BusinessException(
                    "Thiếu Selection Token"
            );
        }

        String selectionToken =
                authorizationHeader.substring("Bearer ".length());

        SelectOrganizationResponse response =
                authService.selectOrganization(
                        selectionToken,
                        request
                );

        return ResponseEntity.ok(
                ApiResult.success(response)
        );
    }

    /**
     * Chuyển đổi sang organization khác khi user đã đăng nhập.
     *
     * <p>
     * Endpoint này yêu cầu Access JWT hợp lệ.
     * </p>
     *
     * <p>
     * Sau khi chuyển organization, hệ thống cấp Access JWT mới
     * tương ứng với organization được lựa chọn.
     * </p>
     *
     * @param request thông tin organization cần chuyển sang
     * @return Access JWT và thông tin organization mới
     */
    @PostMapping("/switch-organization")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<SelectOrganizationResponse>> switchOrganization(
            @Valid @RequestBody SelectOrganizationRequest request) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) auth.getPrincipal();

        return ResponseEntity.ok(
                ApiResult.success(
                        authService.switchOrganization(
                                userDetails.getUserId(),
                                request
                        )
                )
        );
    }
}