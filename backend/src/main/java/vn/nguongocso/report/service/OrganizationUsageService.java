package vn.nguongocso.report.service;

import java.time.LocalDate;
import java.util.UUID;

import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse;

/** Service tổng hợp mức độ sử dụng nền tảng theo từng tổ chức. */
public interface OrganizationUsageService {

    /**
     * Lấy dữ liệu mức độ sử dụng của từng tổ chức trong kỳ.
     */
    OrganizationUsageDashboardResponse getDashboard(
        LocalDate startDate,
        LocalDate endDate,
        UUID organizationId
    );

    /**
     * Xuất báo cáo mức độ sử dụng theo kỳ ra file CSV.
     */
    byte[] exportCsv(
        LocalDate startDate,
        LocalDate endDate,
        UUID organizationId
    );

    /**
     * Xuất báo cáo mức độ sử dụng theo kỳ ra file PDF.
     */
    byte[] exportPdf(
        LocalDate startDate,
        LocalDate endDate,
        UUID organizationId
    );
}
