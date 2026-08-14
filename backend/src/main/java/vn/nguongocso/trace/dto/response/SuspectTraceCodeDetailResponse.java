package vn.nguongocso.trace.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
public class SuspectTraceCodeDetailResponse extends SuspectTraceCodeResponse {
    private List<ScanLogDetail> scanLogs;
    private AnomalyDetails anomalyDetails;
}