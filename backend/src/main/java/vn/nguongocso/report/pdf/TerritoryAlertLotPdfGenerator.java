package vn.nguongocso.report.pdf;

import java.time.LocalDate;
import java.util.List;

import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;

/** Giao diện định nghĩa phương thức tạo báo cáo danh sách lô có cảnh báo dưới dạng PDF. */
public interface TerritoryAlertLotPdfGenerator {

    /** Tạo file PDF danh sách lô có cảnh báo theo địa bàn. */
    byte[] generate(
            List<AlertLotSummaryResponse> alertLots,
            String officerName,
            LocalDate fromDate,
            LocalDate toDate
    );
}
