package vn.nguongocso.certification.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO cho yêu cầu ghi nhận toàn bộ kết quả kiểm nghiệm của một yêu cầu kiểm nghiệm.
 */
@Getter
@Setter
@Builder
public class RecordInspectionResultsRequest {
    @NotEmpty(message = "Danh sách kết quả kiểm nghiệm không được để trống.")
    private List<@Valid InspectionCriterionResultRequest> results;
}