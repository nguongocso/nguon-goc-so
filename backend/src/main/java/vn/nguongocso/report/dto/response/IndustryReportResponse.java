package vn.nguongocso.report.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response chứa dữ liệu báo cáo tổng hợp theo địa bàn và khoảng thời gian.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndustryReportResponse {
    private String region;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Boolean hasData;

    private Integer totalOrganizations;

    private Integer totalShipments;

    private Double totalQuantity;

    private List<ProductBreakdownItem> productBreakdown;

    private String message;
}