package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for inspection criterion catalog entries.
 * Story: NCL-09-CN-009
 */
@Getter
@Builder
public class InspectionCriterionCatalogResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("maxThreshold")
    private BigDecimal maxThreshold;

    @JsonProperty("referenceStandard")
    private String referenceStandard;

    @JsonProperty("status")
    private String status;

    @JsonProperty("referenced")
    private boolean referenced;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
