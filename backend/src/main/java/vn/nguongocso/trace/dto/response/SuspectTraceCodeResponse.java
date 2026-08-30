package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SuspectTraceCodeResponse {
    private UUID id;
    private String codeValue;
    private String shipmentName;
    private String status;
    private Integer suspicionScore;
    private String suspicionReason;
    private Integer scanCount;
    private Integer uniqueLocations;
    private LocalDateTime firstScannedAt;
    private LocalDateTime lastScannedAt;
    private LocalDateTime lockedAt;
    private UUID lockedBy;
    private String lockedByName;
    private String lockReason;
    private LocalDateTime unlockedAt;
    private UUID unlockedBy;
    private String unlockedByName;
    private String unlockConclusion;
    private String unlockEvidence;
    private String verificationNote;
}