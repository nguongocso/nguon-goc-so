package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class InspectionRequestListResponse {

    @JsonProperty("testRequestId")
    private UUID testRequestId;

    @JsonProperty("lotCode")
    private String lotCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("testingUnit")
    private String testingUnit;

    @JsonProperty("sampleSentDate")
    private LocalDate sampleSentDate;

    @JsonProperty("criteriaCount")
    private int criteriaCount;
}
