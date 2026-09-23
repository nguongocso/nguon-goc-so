package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi kết quả đồng bộ các sự kiện ngoại tuyến.
 */
@Getter
@Builder
public class OfflineEventSyncResponse {
    private UUID syncId;

    private int totalEvents;

    private int successCount;

    private int duplicateCount;

    private int failedCount;

    private List<OfflineEventSyncResultDto> results;
}
