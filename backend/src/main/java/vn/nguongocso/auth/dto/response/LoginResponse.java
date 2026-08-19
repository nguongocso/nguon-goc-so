package vn.nguongocso.auth.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response cho phép đăng nhập.
 */
@Data
@Builder
public class LoginResponse {

    private String selectionToken;

    private String tokenType;

    private Long expiresIn;

    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {

        private String userId;

        private String username;

        private String fullName;
    }
}