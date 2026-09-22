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

    /**
     * Khóa bản rõ đầy đủ (chỉ hiển thị DUY NHẤT 1 LẦN khi tạo mới khóa thành công).
     * Khi lấy danh sách hoặc xem chi tiết, trường này sẽ là null và bị loại bỏ khỏi JSON.
     */
    private String rawApiKey;

    private Integer rateLimitPerHour;
    private LocalDateTime expiresAt;
    private PartnerApiKeyStatus status;

    /**
     * Đánh dấu khóa thử nghiệm (Sandbox).
     */
    @com.fasterxml.jackson.annotation.JsonProperty("is_test")
    private Boolean isTest;

    private Long totalCalls;
    private Long failedCalls;

    /**
     * Số lượt gọi trong ngày hôm nay của khóa (chỉ trả ở danh sách khóa - NCL-12-CN-005).
     * <p>
     * Chỉ dùng để hiển thị thống kê; không dùng để kích hoạt cảnh báo hạn mức.
     */
    private Integer usedCallsToday;

    /**
     * Số lượt gọi THÀNH CÔNG trong giờ đồng hồ hiện tại (NCL-12-CN-005, QTN-20).
     * <p>
     * Đây là cơ sở kích hoạt cảnh báo "sắp chạm hạn mức" vì QTN-20 quy định hạn
     * mức theo giờ. Request bị 429 không được tính.
     */
    private Integer currentHourCalls;

    /**
     * Ngưỡng lượt gọi trong GIỜ kích hoạt cảnh báo "sắp chạm hạn mức"
     * (chỉ trả ở danh sách khóa - NCL-12-CN-005).
     */
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
