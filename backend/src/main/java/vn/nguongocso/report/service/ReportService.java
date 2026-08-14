package vn.nguongocso.report.service;

import java.time.LocalDate;

import vn.nguongocso.report.dto.response.IndustryReportResponse;

/**
 * Service để quản lý các chức năng liên quan đến báo cáo.
 */
public interface ReportService {
        /**
         * Lấy báo cáo tổng hợp theo địa bàn và khoảng thời gian.
         */
        IndustryReportResponse getIndustrySummary(
                        String region,
                        LocalDate fromDate,
                        LocalDate toDate);

        /**
         * Xuất báo cáo tổng hợp dạng PDF.
         */
        byte[] exportIndustrySummary(
                        String region,
                        LocalDate fromDate,
                        LocalDate toDate);

        /**
         * Xuất báo cáo tổng hợp theo định dạng yêu cầu (PDF / EXCEL).
         *
         * @return mảng byte nội dung file đã tạo
         */
        byte[] exportIndustrySummary(
                        String region,
                        LocalDate fromDate,
                        LocalDate toDate,
                        String format);
}