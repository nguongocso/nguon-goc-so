package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import vn.nguongocso.alert.dto.response.AnomalyThresholdResponse;

import java.time.LocalDateTime;
import java.util.List;

/** DTO response chi tiết mã truy xuất nghi vấn. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SuspectTraceCodeDetailResponse extends SuspectTraceCodeResponse {
    private List<ScanLogDetail> scanLogs;

    private AnomalyDetails anomalyDetails;

    private LocalDateTime evaluatedAt;

    private AnomalyThresholdResponse effectiveThreshold;

    private String productCategoryName;
}
