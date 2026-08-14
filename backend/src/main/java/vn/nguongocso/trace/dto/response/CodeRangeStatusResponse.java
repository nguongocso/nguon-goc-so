package vn.nguongocso.trace.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Response trả về thông tin trạng thái của một dải mã truy xuất.
 */
@Data
@Builder
public class CodeRangeStatusResponse {
    private UUID id;

    private UUID organizationId;

    private String organizationName;

    private String prefix;

    private Long totalLimit;

    private Long usedCount;

    private Double usagePercent;

    private String status; // OK, NEARLY_EXHAUSTED, EXHAUSTED
}
