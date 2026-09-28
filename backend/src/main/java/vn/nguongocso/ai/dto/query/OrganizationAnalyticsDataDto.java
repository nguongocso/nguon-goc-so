package vn.nguongocso.ai.dto.query;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO đóng gói toàn diện các dữ liệu thống kê nghiệp vụ của tổ chức phục vụ ngữ cảnh AI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationAnalyticsDataDto {
    /** Định danh tổ chức (hoặc null nếu là số liệu tổng hợp địa bàn của cơ quan quản lý). */
    private UUID organizationId;

    /** Tên tổ chức hoặc tên địa bàn quản lý. */
    private String organizationName;

    /** Mã tổ chức. */
    private String organizationCode;

    /** Tổng hợp số liệu lô sản xuất và diện tích. */
    private ProductionLotSummaryDto lotSummary;

    /** Danh sách chứng nhận sắp hết hiệu lực trong 30 ngày. */
    private List<CertificationStatusDto> expiringCertifications;

    /** Tổng hợp các cảnh báo quét dị thường và sự cố thu hồi. */
    private RecentAlertsSummaryDto alertsSummary;

    /** Tổng hợp tình trạng lô hàng và tiến độ bàn giao. */
    private ShipmentSummaryDto shipmentSummary;
}
