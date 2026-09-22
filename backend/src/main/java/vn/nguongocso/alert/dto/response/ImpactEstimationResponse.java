package vn.nguongocso.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi kết quả ước lượng tác động của ngưỡng dự thảo (NCL-08-CN-014).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactEstimationResponse {
    private long estimatedAnomaliesCount;

    private long totalScansAnalyzed;

    private long totalTraceCodesAnalyzed;

    private long highFrequencyCount;

    private long impossibleTravelCount;

    private long activationAgeCount;

    private int analysisPeriodDays;

    private String message;
}
