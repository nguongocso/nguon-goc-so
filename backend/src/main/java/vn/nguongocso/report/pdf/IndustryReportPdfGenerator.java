package vn.nguongocso.report.pdf;

import vn.nguongocso.report.dto.response.IndustryReportResponse;

/**
 * Giao diện định nghĩa phương thức tạo báo cáo tổng hợp ngành dưới dạng PDF.
 */
public interface IndustryReportPdfGenerator {
    /**
     * Tạo báo cáo tổng hợp ngành dưới dạng PDF.
     *
     * @param report Dữ liệu báo cáo tổng hợp ngành
     * @return Mảng byte đại diện cho tệp PDF đã tạo
     */
    byte[] generate(IndustryReportResponse report);
}
