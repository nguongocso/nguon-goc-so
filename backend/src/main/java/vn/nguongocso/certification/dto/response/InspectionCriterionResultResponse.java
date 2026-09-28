package vn.nguongocso.certification.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import vn.nguongocso.certification.enums.InspectionResultEntrySource;

/**
 * DTO phản hồi kết quả kiểm nghiệm cho một chỉ tiêu.
 */
@Getter
@Setter
@Builder
public class InspectionCriterionResultResponse {
    private String resultId;

    private String criterionId;

    private Long criterionDefinitionId;

    private String criterionCode;

    private String criterionName;

    private LocalDate resultDate;

    private LocalDate expiryDate;

    private Boolean passed;

    private String filePath;

    private String createdByName;

    private InspectionResultEntrySource entrySource;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
