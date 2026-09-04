package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * Response DTO cho mốc canh tác (bảng hợp nhất).
 * Story: NCL-09-CN-011
 */
@Getter
@Builder
public class CultivationMilestoneResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("activityType")
    private String activityType;

    @JsonProperty("expectedDaysFromPlanting")
    private Integer expectedDaysFromPlanting;

    @JsonProperty("productCategoryId")
    private String productCategoryId;

    @JsonProperty("productCategoryName")
    private String productCategoryName;

    @JsonProperty("standardId")
    private String standardId;

    @JsonProperty("standardName")
    private String standardName;

    @JsonProperty("isMandatory")
    private boolean isMandatory;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
