package vn.nguongocso.certification.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO cho yêu cầu ghi nhận toàn bộ kết quả kiểm nghiệm của một yêu cầu kiểm nghiệm.
 *
 * <p>
 * Payload phải chứa kết quả cho tất cả chỉ tiêu của yêu cầu; backend sẽ
 * validate toàn bộ trước khi lưu trong một giao dịch (all-or-nothing).
 * </p>
 */
@Getter
@Setter
@Builder
public class RecordInspectionResultsRequest {

    /**
     * Danh sách kết quả kiểm nghiệm cho từng chỉ tiêu của yêu cầu.
     */
    @NotEmpty(message = "Danh sách kết quả kiểm nghiệm không được để trống.")
    private List<@Valid InspectionCriterionResultRequest> results;
}