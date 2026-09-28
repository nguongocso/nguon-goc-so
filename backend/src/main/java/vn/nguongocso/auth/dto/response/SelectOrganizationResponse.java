package vn.nguongocso.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response sau khi người dùng lựa chọn tổ chức thành công.
 */
@Getter
@Builder
public class SelectOrganizationResponse {
    private String accessToken;

    private String tokenType;

    private long expiresIn;

    private UserInfo user;

    /**
     * Thông tin người dùng.
     */
    @Getter
    @Builder
    public static class UserInfo {
        private String userId;

        private String username;

        private String fullName;

        private String phone;

        private String email;

        private String organizationId;

        private String organizationCode;

        private String organizationName;

        private String organizationType;

        private String organizationProvinceId;

        private String organizationCommuneId;

        private String roleCode;

        private String roleName;
    }
}