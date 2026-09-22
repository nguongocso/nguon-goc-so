package vn.nguongocso.report.dto.response;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO phản hồi dữ liệu dashboard lô sản xuất. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionLotDashboardResponse {

    private SummaryDto summary;

    private Map<String, Long> byStatus;

    private List<TimeSeriesDto> timeSeries;

    /** Thống kê tổng hợp. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDto {

        private Long totalLots;

        private Double totalExpectedYield;

        private Double totalActualYield;
    }

    /** Dữ liệu chuỗi thời gian. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDto {

        private String period;

        private Long lotCount;

        private Double expectedYield;

        private Double actualYield;
    }
}