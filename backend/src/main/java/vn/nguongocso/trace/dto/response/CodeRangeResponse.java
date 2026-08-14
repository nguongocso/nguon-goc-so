package vn.nguongocso.trace.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response trả về thông tin của một dải mã truy xuất.
 */
@Data
@Builder
public class CodeRangeResponse {
    private UUID id;

    private UUID organizationId;

    private String organizationName;

    private String prefix;

    private Long totalLimit;

    private Long usedCount;

    private LocalDateTime createdAt;
}
