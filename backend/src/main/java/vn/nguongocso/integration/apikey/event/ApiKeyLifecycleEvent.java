package vn.nguongocso.integration.apikey.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;

/**
 * Sự kiện vòng đời khóa truy cập vừa được cấp hoặc gia hạn.
*/
@Getter
@Builder
public class ApiKeyLifecycleEvent {
    private UUID apiKeyId;

    private UUID organizationId;

    private String partnerName;

    private LocalDateTime expiresAt;

    private PartnerApiKeyStatus status;
}
