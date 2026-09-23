package vn.nguongocso.integration.partner.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO payload gửi tới địa chỉ Webhook của đối tác khi lô bị thu hồi.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerRecallPayloadDto {
    private UUID eventId;

    private String eventType;

    private UUID shipmentId;

    private String shipmentCode;

    private UUID productionLotId;

    private String productionLotCode;

    private String productName;

    private String previousStatus;

    private String newStatus;

    private LocalDateTime timestamp;

    private String publicReason;

    private String remediationSummary;
}
