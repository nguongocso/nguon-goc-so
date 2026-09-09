package vn.nguongocso.recall.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO để phê duyệt yêu cầu thu hồi hàng loạt (NCL-08-CN-011).
 */
@Getter
@Setter
public class ApproveBulkRecallRequest {

    /**
     * Ghi chú khi phê duyệt (tùy chọn).
     */
    private String remarks;
}
