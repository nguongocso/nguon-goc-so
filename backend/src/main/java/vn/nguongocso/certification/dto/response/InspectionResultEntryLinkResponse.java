package vn.nguongocso.certification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;

/**
 * DTO phản hồi thông tin liên kết nhập kết quả kiểm nghiệm.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InspectionResultEntryLinkResponse {
    private UUID id;

    private InspectionResultEntryLinkStatus status;

    private String recipientEmail;

    private String tokenPrefix;

    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    private LocalDateTime createdAt;

    private String entryUrl;
}
