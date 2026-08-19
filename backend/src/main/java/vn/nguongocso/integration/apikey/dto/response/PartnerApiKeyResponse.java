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

    private Long totalCalls;
    private Long failedCalls;
    private LocalDateTime lastCalledAt;
    private Integer lastCallStatus;
    private String lastCallIp;

    private String createdByName;
    private LocalDateTime createdAt;
    private String revokedByName;
    private LocalDateTime revokedAt;
}
