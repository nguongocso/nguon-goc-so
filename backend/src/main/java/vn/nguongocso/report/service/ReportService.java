package vn.nguongocso.report.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.nguongocso.report.dto.response.IndustryReportResponse;

/**
 * Service để quản lý các chức năng liên quan đến báo cáo.
 */
public interface ReportService {
        /**
         * Lấy báo cáo tổng hợp theo địa bàn và khoảng thời gian.
         *
         * @param region  địa bàn lọc theo địa chỉ tổ chức (tuỳ chọn với VT-05 đã
         *                được gán địa bàn)
         * @param unitIds danh sách ID đơn vị hành chính lọc theo mapping tổ chức
         *                (lặp được trên query string, có thể rỗng)
         */
        IndustryReportResponse getIndustrySummary(
                        String region,
                        List<UUID> unitIds,
                        LocalDate fromDate,
                        LocalDate toDate);

        /**
         * Xuất báo cáo tổng hợp dạng PDF.
         */
        byte[] exportIndustrySummary(
                        String region,
                        List<UUID> unitIds,
                        LocalDate fromDate,
                        LocalDate toDate);

        /**
         * Xuất báo cáo tổng hợp theo định dạng yêu cầu (PDF / EXCEL).
         *
         * @return mảng byte nội dung file đã tạo
         */
        byte[] exportIndustrySummary(
                        String region,
                        List<UUID> unitIds,
                        LocalDate fromDate,
                        LocalDate toDate,
                        String format);
}
