package vn.nguongocso.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response sau khi người dùng lựa chọn tổ chức thành công.
 *
 * <p>
 * Access Token được cấp sau khi hệ thống xác định được
 * organization và role của người dùng.
 * </p>
 */
@Getter
@Builder
public class SelectOrganizationResponse {

    /**
     * Access JWT dùng cho các API nghiệp vụ.
     */
    private String accessToken;

    /**
     * Loại token.
     */
    private String tokenType;

    /**
     * Thời gian hết hạn của access token, tính bằng giây.
     */
    private long expiresIn;

    /**
     * Thông tin user trong organization đã lựa chọn.
     */
    private UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {

        private String userId;

        private String username;

        private String fullName;

        private String organizationId;

        private String organizationCode;

        private String organizationName;

        private String organizationType;

        private String roleCode;

        private String roleName;
    }
}