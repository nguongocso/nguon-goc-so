package vn.nguongocso.report.dto.response;

import lombok.*;
import java.util.List;
import java.util.UUID;

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationScanStats {
        private String location;
        private long scanCount;
    }

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
