package vn.nguongocso.report.dto.response;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * DTO phản hồi dữ liệu dashboard lô sản xuất.
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionLotDashboardResponse {
    // Thống kê tổng hợp
    private SummaryDto summary;

    // Số lượng lô theo trạng thái
    private Map<String, Long> byStatus;

    // Dữ liệu chuỗi thời gian
    private List<TimeSeriesDto> timeSeries;

    /**
     * DTO thống kê tổng hợp.
     */
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

    /**
     * DTO dữ liệu chuỗi thời gian.
     */
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