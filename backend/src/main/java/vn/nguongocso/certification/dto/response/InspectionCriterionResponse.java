package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Response DTO cho chỉ tiêu kiểm nghiệm
 */
@Getter
@Builder
public class InspectionCriterionResponse {

    @JsonProperty("criteriaId")
    private Integer criteriaId;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("standardId")
    private UUID standardId;

    @JsonProperty("standardName")
    private String standardName;
}