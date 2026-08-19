package vn.nguongocso.event.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Kết quả kiểm chứng tính toàn vẹn dòng sự kiện của một lô hàng.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
public class ChainVerificationResponse {
    private UUID shipmentId;
    private String shipmentName;
    private Integer totalEvents;
    private Boolean isIntegrityVerified;
    private String verificationStatus; // "INTACT", "BROKEN"
    private Integer failedEventIndex;
    private UUID failedEventId;
    private String failureReason;
    private LocalDateTime verifiedAt;
    private String hashAlgorithm;
    private List<EventVerificationItem> events;
}