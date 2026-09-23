package vn.nguongocso.integration.apikey.event;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Sự kiện lượt gọi chạm ngưỡng cảnh báo hạn mức của khóa truy cập.
*/
@Getter
@Builder
public class ApiKeyQuotaThresholdEvent {
    private UUID apiKeyId;

    private UUID organizationId;

    private String partnerName;

    private int rateLimitPerHour;

    private int usedCalls;

    private int warningThreshold;
}
