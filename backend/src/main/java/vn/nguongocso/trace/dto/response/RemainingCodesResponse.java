package vn.nguongocso.trace.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response trả về số lượng mã truy xuất còn lại của một tổ chức.
 */
@Data
@Builder
public class RemainingCodesResponse {
    private long remainingCount;
    private long totalLimit;
    private long usedCount;
    private boolean hasCodeRange;
}
