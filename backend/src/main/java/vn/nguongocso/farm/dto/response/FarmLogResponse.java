package vn.nguongocso.farm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Thông tin trả về nhật ký canh tác.
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

    private LocalDateTime createdAt;

    private List<AttachmentResponse> attachments;

    private Integer attachmentCount;
}