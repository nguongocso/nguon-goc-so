package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO đại diện cho một sự kiện trong lịch sử vòng đời của mã tem (NCL-04-CN-008). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryEvent {
    private String type;

    private LocalDateTime timestamp;

    private String details;

    private String actorName;
}
