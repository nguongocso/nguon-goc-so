package vn.nguongocso.report.pdf;

import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse;

/** Giao diện định nghĩa phương thức tạo báo cáo mức độ sử dụng nền tảng theo tổ chức dưới dạng PDF (NCL-07-CN-008). */
public interface OrganizationUsagePdfGenerator {
    /** Tạo báo cáo mức độ sử dụng nền tảng dưới dạng PDF. */
    byte[] generate(OrganizationUsageDashboardResponse dashboard);
}
