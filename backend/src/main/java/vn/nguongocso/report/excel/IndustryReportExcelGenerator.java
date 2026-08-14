package vn.nguongocso.report.excel;

import vn.nguongocso.report.dto.response.IndustryReportResponse;

/**
 * Sinh file Excel cho báo cáo tổng hợp ngành.
 */
public interface IndustryReportExcelGenerator {
    /**
     * Tạo nội dung file Excel dạng byte[].
     *
     * @param report dữ liệu báo cáo đã tính toán
     * @return nội dung file .xlsx
     */
    byte[] generate(IndustryReportResponse report);
}