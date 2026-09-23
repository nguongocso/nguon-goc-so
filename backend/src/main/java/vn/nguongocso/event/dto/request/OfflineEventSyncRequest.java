package vn.nguongocso.event.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * DTO cho yêu cầu đồng bộ các sự kiện ngoại tuyến.
 */
@Getter
@Setter
public class OfflineEventSyncRequest {
    @NotNull(message = "Mã phiên đồng bộ (syncId) không được để trống")
    private UUID syncId;

    @NotEmpty(message = "Danh sách các sự kiện đồng bộ không được để trống")
    @Valid
    private List<RecordOfflineEventDto> events;
}
