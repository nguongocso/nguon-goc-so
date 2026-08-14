package vn.nguongocso.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import vn.nguongocso.auth.dto.request.LoginRequest;
import vn.nguongocso.auth.dto.request.SelectOrganizationRequest;
import vn.nguongocso.auth.dto.response.LoginResponse;
import vn.nguongocso.auth.dto.response.OrganizationSelectionResponse;
import vn.nguongocso.auth.dto.response.SelectOrganizationResponse;
import vn.nguongocso.auth.dto.response.UserProfileResponse;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.auth.service.AuthService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.exception.BusinessException;

/**
 * REST controller providing authentication-related endpoints.
 *
 * <p>
 * This controller handles user authentication and exposes APIs
 * for retrieving information about the currently authenticated user.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PermissionChecker permissionChecker;

    /**
     * Authenticates a user using the provided credentials.
     *
     * <p>
     * If the credentials are valid, a JWT access token and the
     * associated user information are returned.
     * </p>
     *
     * @param request login request containing username, password and
     *                organization information
     * @return authenticated user information and JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResult<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResult.success(authService.login(request)));
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * <p>
     * The user information is obtained from the Spring Security
     * authentication context.
     * </p>
     *
     * @return profile of the authenticated user
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserProfileResponse>> getCurrentUser() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        java.util.List<String> permissions = permissionChecker.getPermissionsForCurrentUser();

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .fullName(userDetails.getFullName())
                .roleCode(userDetails.getRoleCode())
                .roleName(userDetails.getRoleName())
                .organizationId(userDetails.getOrganizationId())
                .organizationCode(userDetails.getOrganizationCode())
                .organizationName(userDetails.getOrganizationName())
                .organizationType(userDetails.getOrganizationType())
                .permissions(permissions)
                .build();

        return ResponseEntity.ok(ApiResult.success(response));
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
     * Selection JWT không tạo SecurityContext nên không sử dụng
     * @PreAuthorize("isAuthenticated()").
     * </p>
     *
     * @param authorization Authorization header
     * @return danh sách organization của user
     */
    @GetMapping("/organizations")
    public ResponseEntity<ApiResult<List<OrganizationSelectionResponse>>> getOrganizations(
            @RequestHeader("Authorization") String authorization) {

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

        @GetMapping("/my-organizations")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<List<OrganizationSelectionResponse>>> getMyOrganizations() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                return ResponseEntity.ok(
                                ApiResult.success(authService.getOrganizationsForUser(userDetails.getUserId())));
        }

    /**
     * Lựa chọn organization mà user muốn sử dụng.
     *
     * <p>
     * Sau khi lựa chọn, hệ thống sẽ cấp Access JWT dùng cho các API nghiệp vụ.
     * </p>
     *
     * <p>
     * Endpoint này sử dụng Selection JWT được cấp sau khi
     * username/password authentication thành công.
     * </p>
     *
     * <p>
     * Selection JWT không tạo SecurityContext nên không sử dụng
     * @PreAuthorize("isAuthenticated()").
     * </p>
     *
     * @param request           request chứa thông tin organization mà user muốn
     *                          lựa chọn
     * @param authorizationHeader Authorization header chứa Selection JWT
     * @return Access JWT và thông tin user trong organization đã lựa chọn
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

        String selectionToken = authorizationHeader.substring(7);

        SelectOrganizationResponse response = authService.selectOrganization(
                selectionToken,
                request);

        return ResponseEntity.ok(
                ApiResult.success(response));
    }

        @PostMapping("/switch-organization")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<SelectOrganizationResponse>> switchOrganization(
                        @Valid @RequestBody SelectOrganizationRequest request) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

                return ResponseEntity.ok(ApiResult.success(
                                authService.switchOrganization(userDetails.getUserId(), request)));
        }
}
