package vn.nguongocso.trace.dto.response;

import lombok.Builder;
import lombok.Data;

/** DTO response số lượng mã truy xuất còn lại của tổ chức. */
@Data
@Builder
public class RemainingCodesResponse {
    private long remainingCount;

    private long totalLimit;

    private long usedCount;

    private boolean hasCodeRange;
}
