package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Tóm tắt phạm vi công nhận của một đơn vị kiểm nghiệm
 * (NCL-11-CN-006 Phase 2).
 */
@Getter
@Builder
public class AccreditationScopeSummaryResponse {

    @JsonProperty("testingUnitId")
    private UUID testingUnitId;

    @JsonProperty("testingUnitName")
    private String testingUnitName;

    @JsonProperty("accreditedCriteria")
    private List<AccreditedCriterionItem> accreditedCriteria;

    @Getter
    @Builder
    public static class AccreditedCriterionItem {

        /** Id chỉ tiêu trong danh mục dùng chung. */
        @JsonProperty("id")
        private Long id;

        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;
    }
}
