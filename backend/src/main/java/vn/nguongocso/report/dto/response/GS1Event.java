package vn.nguongocso.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO biểu diễn một sự kiện trong hồ sơ GS1 mô phỏng theo bốn chiều:
 * who (recordedBy), when (recordedAt), where (location), why (eventType + details).
 *
 * @author NCL-12-CN-003
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GS1Event {
    // ID sự kiện (who/when/where/why)
    private UUID eventId;

    // Loại sự kiện (why - eventTypeCode)
    private String eventType;

    // Nhãn hiển thị của loại sự kiện
    private String eventTypeLabel;

    // Thời điểm ghi nhận sự kiện (when - eventDateTime)
    private LocalDateTime recordedAt;

    // Người ghi nhận sự kiện (who - actorName)
    private String recordedBy;

    // Địa điểm sự kiện (where - eventLocation)
    private EventLocation location;

    // Dữ liệu chi tiết sự kiện (why - eventData)
    private Map<String, Object> details;
}