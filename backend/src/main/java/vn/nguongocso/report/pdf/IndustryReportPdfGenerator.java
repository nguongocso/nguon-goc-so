package vn.nguongocso.report.pdf;

import vn.nguongocso.report.dto.response.IndustryReportResponse;

/** Giao diện định nghĩa phương thức tạo báo cáo tổng hợp ngành dưới dạng PDF. */
public interface IndustryReportPdfGenerator {
    /** Tạo báo cáo tổng hợp ngành dưới dạng PDF. */
    byte[] generate(IndustryReportResponse report);
}
