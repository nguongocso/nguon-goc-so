package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.report.enums.LotAlertType;

/**
 * Chi tiết bằng chứng cảnh báo của một lô sản xuất.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotAlertEvidenceDetail {
    private LotAlertType alertType;
    private String severity;
    private LocalDateTime triggeredAt;
    private String title;
    private String message;
    private Map<String, Object> evidenceData;
}
