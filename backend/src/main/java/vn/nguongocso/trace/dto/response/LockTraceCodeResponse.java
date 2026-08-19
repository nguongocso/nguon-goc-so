package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockTraceCodeResponse {
    private UUID id;
    private String codeValue;
    private String status;
    private LocalDateTime lockedAt;
    private UUID lockedBy;
    private String lockedByName;
    private String lockReason;
    private Boolean notificationSent;
}