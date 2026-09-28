package vn.nguongocso.farm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Thông tin nhật ký canh tác.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FarmLogResponse {
    private UUID id;

    private UUID productionLotId;

    private String productionLotName;

    private FarmActivityType activityType;

    private String material;

    private Double quantity;

    private String unit;

    private LocalDate executedDate;

    private String notes;

    private String createdByName;

    private UUID createdById;

    private LocalDateTime createdAt;

    private List<AttachmentResponse> attachments;

    private Integer attachmentCount;

    private UUID originalFarmLogId;

    private Boolean isCorrection;

    private String correctionReason;

    private String correctedByName;

    private Boolean isCorrected;
}