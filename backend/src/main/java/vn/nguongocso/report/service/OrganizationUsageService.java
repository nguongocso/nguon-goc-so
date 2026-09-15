package vn.nguongocso.report.service;

import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service tổng hợp mức độ sử dụng nền tảng theo từng tổ chức (NCL-07-CN-008).
 */
public interface OrganizationUsageService {

    /**
     * Lấy dữ liệu mức độ sử dụng của từng tổ chức trong kỳ.
     *
     * @param startDate      ngày bắt đầu kỳ hiện tại (null = 30 ngày gần nhất)
     * @param endDate        ngày kết thúc kỳ hiện tại (null = hôm nay)
     * @param organizationId lọc một tổ chức cụ thể (null = tất cả)
     * @return dữ liệu dashboard kèm kỳ trước tương đương
     */
    OrganizationUsageDashboardResponse getDashboard(LocalDate startDate, LocalDate endDate, UUID organizationId);

    /**
     * Xuất báo cáo mức độ sử dụng theo kỳ ra file CSV.
     *
     * @param startDate      ngày bắt đầu kỳ hiện tại (null = 30 ngày gần nhất)
     * @param endDate        ngày kết thúc kỳ hiện tại (null = hôm nay)
     * @param organizationId lọc một tổ chức cụ thể (null = tất cả)
     * @return nội dung file CSV (UTF-8, có BOM để mở đúng tiếng Việt trong Excel)
     */
    byte[] exportCsv(LocalDate startDate, LocalDate endDate, UUID organizationId);
}
