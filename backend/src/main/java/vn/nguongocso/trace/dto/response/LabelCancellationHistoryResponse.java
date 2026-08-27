package vn.nguongocso.trace.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCancellationHistoryResponse {
    private UUID id;
    private UUID shipmentId;
    private String shipmentName;
    private String cancelledByName;
    private LocalDateTime cancelledAt;
    private Integer quantity;
    private String cancellationType;
    private String rangeFromCode;
    private String rangeToCode;
    private String reasonType;
    private String reasonNote;
}
