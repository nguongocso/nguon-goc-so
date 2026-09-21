package vn.nguongocso.auth.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để trả về một bản ghi lịch sử đăng nhập.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryResponse {
    private UUID id;

    private UUID userId;

    private String usernameInput;

    private String roleCode;

    private String result;

    private String ipAddress;

    private String countryCode;

    private Boolean isNewCountry;

    private OffsetDateTime createdAt;
}
