package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO cho danh sách chỉ tiêu kiểm nghiệm áp dụng cho lô sản xuất.
 */
@Getter
@Builder
public class ProductionLotTestCriteriaResponse {
    @JsonProperty("lotId")
    private UUID lotId;

    @JsonProperty("standardId")
    private UUID standardId;

    @JsonProperty("standardName")
    private String standardName;

    @JsonProperty("criteria")
    private List<TestCriterionItemResponse> criteria;

    @Getter
    @Builder
    public static class TestCriterionItemResponse {
        @JsonProperty("criteriaId")
        private Long criteriaId;

        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("referenceStandard")
        private String referenceStandard;
    }
}
