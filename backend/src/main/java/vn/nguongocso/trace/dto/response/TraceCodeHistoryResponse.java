package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.trace.enums.TraceCodeStatus;

/** DTO response lịch sử chi tiết của một mã tem truy xuất. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceCodeHistoryResponse {
    private String codeValue;

    private TraceCodeStatus status;

    private UUID shipmentId;

    private String shipmentName;

    private long scanCount;

    private LocalDateTime createdAt;

    private List<HistoryEvent> events;
}
