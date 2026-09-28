package vn.nguongocso.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để trả về thông tin chi tiết một bản ghi bất thường đăng nhập.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAnomalyResponse {
    private UUID id;

    private UUID userId;

    private String username;

    private String fullName;

    private String roleCode;

    private UUID organizationId;

    private String organizationName;

    private String reasonCode;

    private Integer attemptCount;

    private String ipAddress;

    private String countryCode;

    private OffsetDateTime detectedAt;

    private String status;

    private boolean accountLocked;

    private OffsetDateTime lockUntil;

    private boolean permanentLock;

    private UUID notificationId;
}
