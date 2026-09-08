package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.trace.enums.TraceCodeStatus;

/**
 * DTO tóm tắt trạng thái của một mã tem truy xuất trong lô hàng (NCL-04-CN-008).
 */
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
