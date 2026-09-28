package vn.nguongocso.report.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO mức độ sử dụng nền tảng của từng tổ chức. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationUsageDashboardResponse {
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate previousStartDate;

    private LocalDate previousEndDate;

    private int totalOrganizations;

    private List<OrganizationUsageItem> items;

    /** Thông tin so sánh chỉ số giữa kỳ hiện tại và kỳ trước. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricComparison {

        private long current;

        private long previous;

        private long change;

        private Double changePercent;
    }

    /** Thông tin mức độ sử dụng của một tổ chức. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizationUsageItem {

        private UUID organizationId;

        private String organizationCode;

        private String organizationName;

        private String organizationType;

        private String organizationStatus;

        private LocalDateTime createdAt;

        private boolean hasData;

        private LocalDateTime lastActivityAt;

        private boolean needsSupport;

        private MetricComparison productionLots;

        private MetricComparison farmLogs;

        private MetricComparison chainEvents;

        private MetricComparison activatedLabels;

        private MetricComparison publicLookups;

        private MetricComparison activeUsers;
    }
}
