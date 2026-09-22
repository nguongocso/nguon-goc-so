package vn.nguongocso.report.excel;

import vn.nguongocso.report.dto.response.IndustryReportResponse;

/** Giao diện sinh tệp Excel cho báo cáo tổng hợp ngành. */
public interface IndustryReportExcelGenerator {

    /** Tạo nội dung tệp Excel từ dữ liệu báo cáo tổng hợp ngành. */
    byte[] generate(IndustryReportResponse report);
}