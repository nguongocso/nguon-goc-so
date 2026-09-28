package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionValidityStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO chi tiết hiệu lực của từng tiêu chí kiểm nghiệm (NCL-11-CN-004).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterionValidityResponse {
    private Long criterionId;

    private String criterionCode;

    private String criterionName;

    private Boolean passed;

    private LocalDate expiryDate;

    private Long daysRemaining;

    private Long daysOverdue;

    private InspectionValidityStatus status;
}
