package vn.nguongocso.trace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** DTO response thống kê quét mã trong truy xuất. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanStatsTraceDto {
    private long totalScans;

    private LocalDateTime recentScanAt;

    private long suspectCount;
}
