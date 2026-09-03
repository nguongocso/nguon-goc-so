package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for product-category-milestone assignment.
 * Story: NCL-09-CN-011
 */
@Getter
@Builder
public class ProductCategoryMilestoneResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("milestone")
    private CultivationMilestoneCatalogResponse milestone;

    @JsonProperty("standardId")
    private String standardId;

    @JsonProperty("standardName")
    private String standardName;

    @JsonProperty("isMandatory")
    private boolean isMandatory;
}
