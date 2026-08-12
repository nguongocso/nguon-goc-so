package vn.nguongocso.loginanomaly.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import vn.nguongocso.auth.enums.UserStatus;

/** Phản hồi sau khi quản trị viên khóa tạm một tài khoản. */
@Getter
@Builder
public class LockLoginAnomalyResponse {

    private UUID id;

    private String username;

    private UserStatus accountStatus;

    private LocalDateTime lockedAt;

    /** Người thực hiện khóa (họ tên của user đang đăng nhập). */
    private String lockedBy;
}
