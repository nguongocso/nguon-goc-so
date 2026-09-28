package vn.nguongocso.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi số lượng cảnh báo đang mở phục vụ hiển thị huy hiệu trên thanh điều hướng (NCL-08-CN-016).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnviewedAlertCountResponse {
    private long unviewedCount;

    private boolean hasHighSeverity;
}
