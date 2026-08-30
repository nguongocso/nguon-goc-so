package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phản hồi sau khi mở khóa mã tem (NCL-08-CN-013).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnlockTraceCodeResponse {
    private UUID id;
    private String codeValue;
    private String status;
    private LocalDateTime unlockedAt;
    private UUID unlockedBy;
    private String unlockedByName;
    private String unlockConclusion;
    private String unlockEvidence;
    private String verificationNote;
    private Boolean notificationSent;
}
