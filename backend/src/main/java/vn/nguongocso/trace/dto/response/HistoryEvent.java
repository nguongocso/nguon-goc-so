package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho một sự kiện trong lịch sử vòng đời của mã tem (NCL-04-CN-008).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryEvent {
    /**
     * Loại sự kiện: CREATED, PRINTED, ACTIVATED, CANCELLED, LOCKED, UNLOCKED, SCANNED, RECALLED.
     */
    private String type;

    /**
     * Thời điểm diễn ra sự kiện.
     */
    private LocalDateTime timestamp;

    /**
     * Chi tiết sự kiện (lý do, khổ in, địa điểm quét, IP,...).
     */
    private String details;

    /**
     * Tên người thực hiện sự kiện hoặc đối tượng tác động (Actor).
     */
    private String actorName;
}
