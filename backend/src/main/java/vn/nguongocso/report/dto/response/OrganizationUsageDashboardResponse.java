package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO tổng hợp mức độ sử dụng nền tảng của từng tổ chức (NCL-07-CN-008).
 *
 * <p>Mỗi tổ chức có 6 chỉ số, mỗi chỉ số gồm giá trị kỳ hiện tại, kỳ trước
 * và mức thay đổi so với kỳ trước.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationUsageDashboardResponse {

    /** Ngày bắt đầu kỳ hiện tại. */
    private LocalDate startDate;

    /** Ngày kết thúc kỳ hiện tại. */
    private LocalDate endDate;

    /** Ngày bắt đầu kỳ trước (độ dài tương đương kỳ hiện tại). */
    private LocalDate previousStartDate;

    /** Ngày kết thúc kỳ trước (ngày liền trước kỳ hiện tại). */
    private LocalDate previousEndDate;

    /** Tổng số tổ chức trong kết quả. */
    private int totalOrganizations;

    /** Dữ liệu chi tiết theo từng tổ chức. */
    private List<OrganizationUsageItem> items;

    /**
     * So sánh một chỉ số giữa kỳ hiện tại và kỳ trước.
     *
     * <p>Khi kỳ trước bằng 0 thì {@code changePercent} là {@code null}
     * (không trả Infinity/NaN); frontend hiển thị {@code +100.0%}
     * (quy ước tăng từ 0).</p>
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricComparison {

        /** Giá trị kỳ hiện tại. */
        private long current;

        /** Giá trị kỳ trước. */
        private long previous;

        /** Chênh lệch tuyệt đối (current - previous). */
        private long change;

        /** Chênh lệch tương đối (%), null khi previous == 0. */
        private Double changePercent;
    }

    /**
     * Mức độ sử dụng của một tổ chức trong kỳ.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizationUsageItem {

        /** ID tổ chức. */
        private UUID organizationId;

        /** Mã tổ chức. */
        private String organizationCode;

        /** Tên tổ chức. */
        private String organizationName;

        /** Loại tổ chức. */
        private String organizationType;

        /** Trạng thái tổ chức. */
        private String organizationStatus;

        /** Thời điểm tổ chức được tạo. */
        private LocalDateTime createdAt;

        /**
         * True khi tổ chức có ít nhất một hoạt động trong kỳ hiện tại
         * hoặc kỳ trước. False nghĩa là "Chưa có dữ liệu" cho kỳ được chọn
         * (tổ chức mới tạo hoặc chưa phát sinh hoạt động trong kỳ).
         */
        private boolean hasData;

        /** Thời điểm có hoạt động gần nhất (từ mọi nguồn hoạt động thực tế). */
        private LocalDateTime lastActivityAt;

        /** True khi không có hoạt động nào trong 30 ngày gần nhất. */
        private boolean needsSupport;

        /** Số lô sản xuất tạo mới (bảng production_lot, theo created_at). */
        private MetricComparison productionLots;

        /** Số mục nhật ký canh tác (bảng farm_logs, theo created_at). */
        private MetricComparison farmLogs;

        /** Số sự kiện chuỗi (bảng chain_events, loại trừ đính chính). */
        private MetricComparison chainEvents;

        /** Số tem kích hoạt (bảng trace_codes, theo activated_at). */
        private MetricComparison activatedLabels;

        /** Số lượt tra cứu công khai (bảng trace_code_scan_logs). */
        private MetricComparison publicLookups;

        /** Số người dùng hoạt động (userId phân biệt trong activity_logs). */
        private MetricComparison activeUsers;
    }
}
