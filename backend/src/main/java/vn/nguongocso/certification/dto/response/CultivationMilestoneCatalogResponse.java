package vn.nguongocso.certification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * Response DTO for cultivation milestone catalog entries.
 * Story: NCL-09-CN-011
 */
@Getter
@Builder
public class CultivationMilestoneCatalogResponse {

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

    @JsonProperty("status")
    private String status;

    @JsonProperty("referenced")
    private boolean referenced;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
