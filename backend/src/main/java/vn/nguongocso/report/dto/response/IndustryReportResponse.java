package vn.nguongocso.report.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Báo cáo tổng hợp ngành theo địa bàn. */
@Getter
@Setter
@Builder
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
