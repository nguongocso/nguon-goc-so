package vn.nguongocso.report.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi phân tích diện tích canh tác.
 *
 * @author Triệu Văn Đại
 */
@Getter
@Builder
public class CropAreaAnalysisResponse {
    private SummaryStats summary;

    private List<AreaAnalysisStats> byArea;

    private List<SeasonAnalysisStats> bySeason;

    /**
     * DTO phản hồi thống kê theo khu vực canh tác.
     */
    @Getter
    @Builder
    public static class SummaryStats {
        private long totalLots;

        private double totalExpectedYield;

        private double totalActualYield;

        private double totalArea;
    }

    /**
     * DTO phản hồi thống kê theo mùa vụ.
     */
    @Getter
    @Builder
    public static class AreaAnalysisStats {
        private UUID farmAreaId;

        private String farmAreaName;

        private double areaSize;

        private String organizationName;

        private long totalLots;

        private double expectedYield;

        private double actualYield;

        private List<AreaSeasonStats> seasons;
    }

    /**
     * DTO phản hồi thống kê theo mùa vụ trong khu vực canh tác.
     */
    @Getter
    @Builder
    public static class AreaSeasonStats {
        private String seasonCode;

        private String seasonName;

        private int year;

        private long lotCount;

        private double expectedYield;

        private double actualYield;
    }

    /**
     * DTO phản hồi thống kê theo mùa vụ.
     */
    @Getter
    @Builder
    public static class SeasonAnalysisStats {
        private String seasonCode;

        private String seasonName;

        private int year;

        private long totalLots;

        private double expectedYield;

        private double actualYield;

        private List<SeasonAreaStats> areas;
    }

    /**
     * DTO phản hồi thống kê theo khu vực canh tác trong mùa vụ.
     */
    @Getter
    @Builder
    public static class SeasonAreaStats {
        private UUID farmAreaId;

        private String farmAreaName;

        private long lotCount;

        private double expectedYield;

        private double actualYield;
    }
}
