package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO response chi tiết điểm nghi vấn quét mã. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreBreakdown {
    private Integer highFrequency;

    private Integer impossibleTravel;

    private Integer multipleLocations;
}
