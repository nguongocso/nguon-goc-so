package vn.nguongocso.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để trả về kết quả khoá/mở khoá tài khoản.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLockResponse {
    private UUID accountId;

    private String status;

    private String lockedBy;

    private OffsetDateTime lockedAt;

    private OffsetDateTime lockUntil;

    private Boolean permanent;

    private String unlockedBy;

    private OffsetDateTime unlockedAt;

    private String reason;

    private Boolean notificationSent;
}
