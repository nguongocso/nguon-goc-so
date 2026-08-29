package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response một dòng phạm vi công nhận của đơn vị kiểm nghiệm
 * (NCL-11-CN-006 Phase 2).
 */
@Getter
@Builder
public class AccreditationScopeResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("testingUnitId")
    private UUID testingUnitId;

    @JsonProperty("testingUnitName")
    private String testingUnitName;

    /** Id chỉ tiêu trong danh mục dùng chung (inspection_criterion_catalog.id). */
    @JsonProperty("criterionDefinitionId")
    private Long criterionDefinitionId;

    @JsonProperty("criterionCode")
    private String criterionCode;

    @JsonProperty("criterionName")
    private String criterionName;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
