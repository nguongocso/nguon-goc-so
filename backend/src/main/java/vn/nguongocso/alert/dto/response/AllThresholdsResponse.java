package vn.nguongocso.alert.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi toàn bộ cấu hình ngưỡng (Global + Overrides) (NCL-08-CN-014).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllThresholdsResponse {

    private AnomalyThresholdResponse global;

    private List<AnomalyThresholdResponse> categoryOverrides;
}
