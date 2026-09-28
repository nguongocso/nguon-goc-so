package vn.nguongocso.recall.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Yêu cầu phê duyệt thu hồi hàng loạt. */
@Getter
@Setter
public class ApproveBulkRecallRequest {
    private String remarks;
}
