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

    /**
     * Số chỉ tiêu không đạt (có kết quả với passed = false).
     * Không tính các chỉ tiêu chưa có kết quả hoặc hết hạn.
     */
    @JsonProperty("failedCriteriaCount")
    private int failedCriteriaCount;

    /**
     * Tỷ lệ chỉ tiêu không đạt trên tổng số chỉ tiêu (%),
     * làm tròn 1 chữ số thập phân. Bằng 0.0 khi không có chỉ tiêu.
     */
    @JsonProperty("failedRatio")
    private double failedRatio;
}
