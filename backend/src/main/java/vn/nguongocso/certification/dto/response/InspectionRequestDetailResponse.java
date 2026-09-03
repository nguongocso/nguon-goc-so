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

    /**
     * Tổng số chỉ tiêu kiểm nghiệm của yêu cầu.
     */
    @JsonProperty("totalCriteria")
    private int totalCriteria;

    /**
     * Số chỉ tiêu đã có kết quả kiểm nghiệm được ghi nhận.
     */
    @JsonProperty("evaluatedCriteria")
    private int evaluatedCriteria;

    /**
     * Số chỉ tiêu đạt (passed = true).
     */
    @JsonProperty("passedCriteria")
    private int passedCriteria;

    /**
     * Số chỉ tiêu không đạt (passed = false).
     * Không tính các chỉ tiêu hết hạn hoặc chưa có kết quả.
     */
    @JsonProperty("failedCriteriaCount")
    private int failedCriteriaCount;

    /**
     * Tỷ lệ chỉ tiêu không đạt trên tổng số chỉ tiêu (%),
     * làm tròn 1 chữ số thập phân. Bằng 0.0 khi không có chỉ tiêu.
     */
    @JsonProperty("failedRatio")
    private double failedRatio;

    @JsonProperty("criteria")
    private List<InspectionRequestDetailCriterionResponse> criteria;
}