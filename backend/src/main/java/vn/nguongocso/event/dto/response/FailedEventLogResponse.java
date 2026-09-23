package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi nhật ký sự kiện bị chặn.
 *
 * @author Triệu Văn Đại
 */
@Data
@Builder
public class FailedEventLogResponse {
    private UUID id;

    private UUID userId;

    private String userFullName;

    private String eventType;

    private UUID lotId;

    private String lotCode;

    private String failureReason;

    private LocalDateTime attemptedAt;
}
