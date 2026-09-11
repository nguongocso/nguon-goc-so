package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mục dòng sự kiện ở chế độ chỉ đọc trong chi tiết lô cảnh báo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadonlyChainEventItem {
    private UUID eventId;
    private String eventType;
    private String eventTypeName;
    private LocalDateTime recordedAt;
    private String recordedByName;
    private String location;
    private Boolean earlyHarvest;
    private String description;
}
