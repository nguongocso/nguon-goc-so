package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết yêu cầu kiểm nghiệm dùng cho màn hình nhập kết quả.
 *
 * GET /api/v1/inspection-requests/{requestId}
 */
@Getter
@Builder
public class InspectionRequestDetailResponse {

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
    private List<InspectionRequestDetailCriterionResponse> criteria;
}