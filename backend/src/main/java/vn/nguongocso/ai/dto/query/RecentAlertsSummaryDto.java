package vn.nguongocso.ai.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO tổng hợp các cảnh báo quét dị thường và sự cố thu hồi gần đây.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentAlertsSummaryDto {
    /** Số lượng cảnh báo quét mã QR bất thường đang chờ xử lý (PENDING). */
    private long pendingScanAnomalyCount;

    /** Số lượng vụ việc thu hồi đang trong quá trình xử lý (OPEN). */
    private long activeRecallCasesCount;
}
