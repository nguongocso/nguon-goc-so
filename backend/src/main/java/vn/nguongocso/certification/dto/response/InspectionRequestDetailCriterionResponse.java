package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Chỉ tiêu kiểm nghiệm trong chi tiết yêu cầu kiểm nghiệm.
 *
 * criterionId là UUID snapshot của chỉ tiêu thuộc yêu cầu
 * (inspection_criteria.id), dùng để ghi nhận kết quả qua
 * POST /api/v1/inspection-criteria/{criterionId}/results.
 */
@Getter
@Builder
public class InspectionRequestDetailCriterionResponse {

    @JsonProperty("criterionId")
    private UUID criterionId;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("standardName")
    private String standardName;

    /**
     * Kết quả kiểm nghiệm đã ghi cho chỉ tiêu (null nếu chưa có).
     */
    @JsonProperty("result")
    private InspectionCriterionResultResponse result;
}