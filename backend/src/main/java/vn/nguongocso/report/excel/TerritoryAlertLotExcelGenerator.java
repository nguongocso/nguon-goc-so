package vn.nguongocso.report.excel;

import java.time.LocalDate;
import java.util.List;

import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;

/** Interface sinh file Excel danh sách lô có cảnh báo cho Cán bộ quản lý ngành. */
public interface TerritoryAlertLotExcelGenerator {
    /** Sinh nội dung file Excel dưới dạng byte array. */
    byte[] generate(List<AlertLotSummaryResponse> alertLots, String officerName, LocalDate fromDate, LocalDate toDate);

    /** Sinh nội dung file Excel tiện ích chỉ với danh sách lô. */
    default byte[] generateExcel(List<AlertLotSummaryResponse> alertLots) {
        return generate(alertLots, null, null, null);
    }
}
