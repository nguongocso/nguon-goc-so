package vn.nguongocso.report.excel;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.report.dto.response.AlertBadgeSummary;
import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;

/**
 * Triển khai sinh file Excel danh sách lô có cảnh báo cho Cán bộ quản lý ngành (NCL-07-CN-006).
 */
@Slf4j
@Component
public class TerritoryAlertLotExcelGeneratorImpl implements TerritoryAlertLotExcelGenerator {

    private static final String EXPORT_ERROR = "Không thể xuất file Excel danh sách lô cảnh báo.";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] HEADERS = {
            "STT",
            "Mã lô",
            "Tên lô sản xuất",
            "Tổ chức / HTX sở hữu",
            "Loại nông sản",
            "Vùng trồng",
            "Xã / Phường",
            "Tỉnh / Thành phố",
            "Trạng thái lô",
            "Loại cảnh báo",
            "Thời điểm cảnh báo",
            "Ghi chú chi tiết"
    };

    @Override
    public byte[] generate(List<AlertLotSummaryResponse> alertLots, String officerName, LocalDate fromDate, LocalDate toDate) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Lô có cảnh báo");

            // Đặt độ rộng cột mặc định
            sheet.setColumnWidth(0, 8 * 256);   // STT
            sheet.setColumnWidth(1, 18 * 256);  // Mã lô
            sheet.setColumnWidth(2, 30 * 256);  // Tên lô
            sheet.setColumnWidth(3, 30 * 256);  // Tổ chức
            sheet.setColumnWidth(4, 20 * 256);  // Loại nông sản
            sheet.setColumnWidth(5, 22 * 256);  // Vùng trồng
            sheet.setColumnWidth(6, 18 * 256);  // Xã
            sheet.setColumnWidth(7, 18 * 256);  // Tỉnh
            sheet.setColumnWidth(8, 16 * 256);  // Trạng thái
            sheet.setColumnWidth(9, 28 * 256);  // Loại cảnh báo
            sheet.setColumnWidth(10, 20 * 256); // Thời điểm
            sheet.setColumnWidth(11, 40 * 256); // Ghi chú

            // Fonts
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            Font subTitleFont = workbook.createFont();
            subTitleFont.setItalic(true);
            subTitleFont.setFontHeightInPoints((short) 11);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font normalFont = workbook.createFont();
            normalFont.setFontHeightInPoints((short) 10);

            // Styles
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle subTitleStyle = workbook.createCellStyle();
            subTitleStyle.setFont(subTitleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setFont(normalFont);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(dataStyle);

            CellStyle centerDataStyle = workbook.createCellStyle();
            centerDataStyle.setFont(normalFont);
            centerDataStyle.setAlignment(HorizontalAlignment.CENTER);
            centerDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(centerDataStyle);

            // 1. Dòng tiêu đề
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH LÔ NÔNG SẢN CÓ CẢNH BÁO THEO ĐỊA BÀN");
            titleCell.setCellStyle(titleStyle);

            // 2. Dòng thông tin phụ
            Row metaRow1 = sheet.createRow(1);
            Cell metaCell1 = metaRow1.createCell(0);
            String timeRangeText = (fromDate != null && toDate != null)
                    ? "Từ ngày: " + fromDate.format(DATE_FORMATTER) + " - Đến ngày: " + toDate.format(DATE_FORMATTER)
                    : "Toàn bộ thời gian";
            metaCell1.setCellValue("Thời gian lọc: " + timeRangeText + " | Cán bộ xuất: " + (officerName != null ? officerName : "Cán bộ quản lý ngành"));
            metaCell1.setCellStyle(subTitleStyle);

            Row metaRow2 = sheet.createRow(2);
            Cell metaCell2 = metaRow2.createCell(0);
            metaCell2.setCellValue("Tổng số lô có cảnh báo: " + alertLots.size());
            metaCell2.setCellStyle(subTitleStyle);

            // 3. Dòng Header
            Row headerRow = sheet.createRow(4);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 4. Điền dữ liệu
            int rowIndex = 5;
            int stt = 1;
            for (AlertLotSummaryResponse lot : alertLots) {
                Row row = sheet.createRow(rowIndex++);

                // STT
                Cell c0 = row.createCell(0);
                c0.setCellValue(stt++);
                c0.setCellStyle(centerDataStyle);

                // Mã lô
                Cell c1 = row.createCell(1);
                c1.setCellValue(lot.getLotCode() != null ? lot.getLotCode() : lot.getLotId().toString().substring(0, 8));
                c1.setCellStyle(centerDataStyle);

                // Tên lô
                Cell c2 = row.createCell(2);
                c2.setCellValue(lot.getLotName() != null ? lot.getLotName() : "—");
                c2.setCellStyle(dataStyle);

                // Tổ chức sở hữu
                Cell c3 = row.createCell(3);
                c3.setCellValue(lot.getOrganizationName() != null ? lot.getOrganizationName() : "—");
                c3.setCellStyle(dataStyle);

                // Loại nông sản
                Cell c4 = row.createCell(4);
                c4.setCellValue(lot.getProductCategoryName() != null ? lot.getProductCategoryName() : "—");
                c4.setCellStyle(dataStyle);

                // Vùng trồng
                Cell c5 = row.createCell(5);
                c5.setCellValue(lot.getFarmAreaName() != null ? lot.getFarmAreaName() : "—");
                c5.setCellStyle(dataStyle);

                // Xã
                Cell c6 = row.createCell(6);
                c6.setCellValue(lot.getCommuneName() != null ? lot.getCommuneName() : "—");
                c6.setCellStyle(dataStyle);

                // Tỉnh
                Cell c7 = row.createCell(7);
                c7.setCellValue(lot.getProvinceName() != null ? lot.getProvinceName() : "—");
                c7.setCellStyle(dataStyle);

                // Trạng thái lô
                Cell c8 = row.createCell(8);
                c8.setCellValue(lot.getLotStatus() != null ? lot.getLotStatus() : "—");
                c8.setCellStyle(centerDataStyle);

                // Loại cảnh báo
                String alertNames = (lot.getAlertSummaries() != null && !lot.getAlertSummaries().isEmpty())
                        ? lot.getAlertSummaries().stream().map(AlertBadgeSummary::getAlertName).collect(Collectors.joining(", "))
                        : (lot.getPrimaryAlertType() != null ? lot.getPrimaryAlertType().name() : "—");
                Cell c9 = row.createCell(9);
                c9.setCellValue(alertNames);
                c9.setCellStyle(dataStyle);

                // Thời điểm cảnh báo
                String triggeredAtStr = (lot.getLatestAlertTriggeredAt() != null)
                        ? lot.getLatestAlertTriggeredAt().format(DATETIME_FORMATTER)
                        : "—";
                Cell c10 = row.createCell(10);
                c10.setCellValue(triggeredAtStr);
                c10.setCellStyle(centerDataStyle);

                // Ghi chú
                String notes = (lot.getAlertSummaries() != null && !lot.getAlertSummaries().isEmpty())
                        ? lot.getAlertSummaries().stream().map(AlertBadgeSummary::getBriefNote).filter(n -> n != null && !n.isBlank()).collect(Collectors.joining("; "))
                        : "—";
                Cell c11 = row.createCell(11);
                c11.setCellValue(notes);
                c11.setCellStyle(dataStyle);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception ex) {
            log.error("Lỗi khi sinh file Excel báo cáo lô có cảnh báo", ex);
            throw new BusinessException(EXPORT_ERROR);
        }
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
