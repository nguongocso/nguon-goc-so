package vn.nguongocso.integration.apikey.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;

/** DTO phản hồi thông tin khóa API tích hợp của đối tác. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerApiKeyResponse {
    private UUID id;

    private UUID organizationId;

    private String partnerName;

    private String keyPrefix;

    private String rawApiKey;

    private Integer rateLimitPerHour;

    private LocalDateTime expiresAt;

    private PartnerApiKeyStatus status;

    @com.fasterxml.jackson.annotation.JsonProperty("is_test")
    private Boolean isTest;

    private Long totalCalls;

    private Long failedCalls;

    private Integer usedCallsToday;

    private Integer currentHourCalls;

    private Integer quotaWarningThreshold;

    private LocalDateTime lastCalledAt;

    private Integer lastCallStatus;

    private String lastCallIp;

    private String createdByName;

    private LocalDateTime createdAt;

    private String revokedByName;

    private LocalDateTime revokedAt;

    private String webhookUrl;

    private String webhookSecret;

    private Boolean isWebhookActive;
}
