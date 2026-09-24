package vn.nguongocso.report.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO phản hồi thống kê tra cứu. */
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

    /** Thống kê tổng hợp. */
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

    /** Thống kê theo địa điểm quét. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationScanStats {

        private String location;

        private long scanCount;
    }

    /** Thống kê theo lô sản xuất. */
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

    /** Thống kê theo khoảng thời gian. */
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
