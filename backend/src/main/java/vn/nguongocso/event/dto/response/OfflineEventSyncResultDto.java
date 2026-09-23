package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

/**
 * DTO phản hồi kết quả đồng bộ một sự kiện ngoại tuyến.
 */
@Getter
@Builder
public class OfflineEventSyncResultDto {
    private UUID offlineEventId;

    private String status; // SUCCESS, DUPLICATE, FAILED

    private UUID eventId; // ID sự kiện trên server nếu thành công hoặc trùng

    private String message;
}
