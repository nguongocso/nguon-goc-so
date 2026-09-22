package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Chỉ tiêu kiểm nghiệm trong chi tiết yêu cầu kiểm nghiệm.
 */
@Getter
@Builder
public class InspectionRequestDetailCriterionResponse {
    @JsonProperty("criterionId")
    private UUID criterionId;

    @JsonProperty("criterionDefinitionId")
    private Long criterionDefinitionId;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("standardName")
    private String standardName;

    @JsonProperty("result")
    private InspectionCriterionResultResponse result;
}