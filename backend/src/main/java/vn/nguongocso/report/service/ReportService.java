package vn.nguongocso.report.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.nguongocso.report.dto.response.IndustryReportResponse;

/** Service quản lý các báo cáo ngành. */
public interface ReportService {

    /** Lấy báo cáo tổng hợp theo địa bàn và khoảng thời gian. */
    IndustryReportResponse getIndustrySummary(
            String region,
            List<UUID> unitIds,
            LocalDate fromDate,
            LocalDate toDate);

    /** Xuất báo cáo tổng hợp dạng PDF. */
    byte[] exportIndustrySummary(
            String region,
            List<UUID> unitIds,
            LocalDate fromDate,
            LocalDate toDate);

    /** Xuất báo cáo tổng hợp theo định dạng yêu cầu. */
    byte[] exportIndustrySummary(
            String region,
            List<UUID> unitIds,
            LocalDate fromDate,
            LocalDate toDate,
            String format);
}
