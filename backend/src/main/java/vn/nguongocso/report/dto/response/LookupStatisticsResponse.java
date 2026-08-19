package vn.nguongocso.report.dto.response;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi thống kê tra cứu.
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupStatisticsResponse {
    private SummaryStats summary;

    private List<LocationScanStats> byLocation;

    private List<LotScanStats> byProductionLot;

    private List<TimeSeriesData> timeSeries;

    /**
     * Thống kê theo từng lô sản xuất.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryStats {
        private long totalScans;

        private long totalUniqueCodes;

        private long abnormalScansCount;
    }

    /**
     * Thống kê theo từng địa điểm quét.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationScanStats {
        private String location;

        private long scanCount;
    }

    /**
     * Thống kê theo từng lô sản xuất.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LotScanStats {
        private UUID lotId;

        private String lotName;

        private long scanCount;

        private long abnormalScansCount;
    }

    /**
     * Thống kê theo từng khoảng thời gian.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSeriesData {
        private String period;

        private long scanCount;
    }
}
