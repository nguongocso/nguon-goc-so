package vn.nguongocso.report.excel;

import java.time.LocalDate;
import java.util.List;

import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;

/**
 * Interface sinh file Excel danh sách lô có cảnh báo cho Cán bộ quản lý ngành.
 */
public interface TerritoryAlertLotExcelGenerator {
    /**
     * Sinh nội dung file Excel dưới dạng byte array.
     *
     * @param alertLots   danh sách các lô có cảnh báo
     * @param officerName tên cán bộ xuất báo cáo
     * @param fromDate    ngày bắt đầu lọc (nếu có)
     * @param toDate      ngày kết thúc lọc (nếu có)
     * @return mảng byte của file .xlsx
     */
    byte[] generate(List<AlertLotSummaryResponse> alertLots, String officerName, LocalDate fromDate, LocalDate toDate);

    /**
     * Sinh nội dung file Excel tiện ích chỉ với danh sách lô.
     */
    default byte[] generateExcel(List<AlertLotSummaryResponse> alertLots) {
        return generate(alertLots, null, null, null);
    }
}

