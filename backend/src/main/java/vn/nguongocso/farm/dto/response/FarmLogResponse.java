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

    // ===== NCL-03-CN-006: Đính chính nhật ký canh tác =====

    /** ID của bản gốc nếu đây là bản đính chính. */
    private UUID originalFarmLogId;

    /** true nếu đây là bản ghi đính chính. */
    private Boolean isCorrection;

    /** Lý do đính chính (chỉ có trên bản đính chính). */
    private String correctionReason;

    /** Tên người thực hiện đính chính (chỉ có trên bản đính chính). */
    private String correctedByName;

    /** true nếu bản ghi này đã bị thay thế hiệu lực bởi bản đính chính khác. */
    private Boolean isCorrected;
}