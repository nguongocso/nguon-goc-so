package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO cho yêu cầu kiểm nghiệm
 */
@Getter
@Builder
public class InspectionRequestResponse {

    @JsonProperty("testRequestId")
    private UUID testRequestId;

    @JsonProperty("lotId")
    private UUID lotId;

    @JsonProperty("lotCode")
    private String lotCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("testingUnit")
    private String testingUnit;

    @JsonProperty("sampleSentDate")
    private LocalDate sampleSentDate;

    @JsonProperty("criteria")
    private List<InspectionCriterionResponse> criteria;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}