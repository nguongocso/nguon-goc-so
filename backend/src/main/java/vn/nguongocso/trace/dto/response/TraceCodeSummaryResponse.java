package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.trace.enums.TraceCodeStatus;

/** DTO response tóm tắt trạng thái của một mã tem truy xuất. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceCodeSummaryResponse {
    private UUID id;

    private String codeValue;

    private TraceCodeStatus status;

    private LocalDateTime activatedAt;

    private LocalDateTime printedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime lockedAt;

    private long scanCount;

    private LocalDateTime createdAt;
}
