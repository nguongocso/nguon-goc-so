package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetails {
    private Integer totalScans;
    private Integer uniqueLocations;
    private Integer impossibleTravelCount;
    private ScoreBreakdown scoreBreakdown;
}