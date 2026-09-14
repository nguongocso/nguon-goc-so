package vn.nguongocso.report.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.report.enums.LotAlertType;

/**
 * Tóm tắt ngắn gọn từng cảnh báo hiển thị dạng huy hiệu (badge).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertBadgeSummary {
    private LotAlertType alertType;
    private String alertName;
    private String severity;
    private LocalDateTime triggeredAt;
    private String briefNote;
}
