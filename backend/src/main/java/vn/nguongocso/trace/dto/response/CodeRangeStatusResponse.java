package vn.nguongocso.trace.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** DTO response trạng thái dải mã truy xuất. */
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

    private String status;
}
