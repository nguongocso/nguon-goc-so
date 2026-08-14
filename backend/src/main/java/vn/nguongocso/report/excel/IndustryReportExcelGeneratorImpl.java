package vn.nguongocso.report.excel;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
import vn.nguongocso.report.dto.response.IndustryReportResponse;
import vn.nguongocso.report.dto.response.ProductBreakdownItem;

/**
 * Sinh file Excel (.xlsx) cho báo cáo tổng hợp ngành bằng Apache POI.
 */
@Slf4j
@Component
public class IndustryReportExcelGeneratorImpl
                implements IndustryReportExcelGenerator {
        private static final String EXPORT_ERROR = "Không thể xuất báo cáo Excel.";

        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private static final String[] HEADERS = {
                        "Loại nông sản",
                        "Số lô hàng",
                        "Tổng sản lượng (kg)"
        };

        /**
         * Tạo nội dung file Excel dạng byte[].
         *
         * @param report dữ liệu báo cáo đã tính toán
         * @return nội dung file .xlsx
         */
        @Override
        public byte[] generate(IndustryReportResponse report) {
                try (Workbook workbook = new XSSFWorkbook();
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet("Báo cáo tổng hợp ngành");
                        sheet.setColumnWidth(0, 30 * 256);
                        sheet.setColumnWidth(1, 18 * 256);
                        sheet.setColumnWidth(2, 22 * 256);

                        Font titleFont = workbook.createFont();
                        titleFont.setBold(true);
                        titleFont.setFontHeightInPoints((short) 16);

                        Font headerFont = workbook.createFont();
                        headerFont.setBold(true);
                        headerFont.setFontHeightInPoints((short) 12);
                        headerFont.setColor(IndexedColors.WHITE.getIndex());

                        Font normalFont = workbook.createFont();
                        normalFont.setFontHeightInPoints((short) 11);

                        CellStyle titleStyle = workbook.createCellStyle();
                        titleStyle.setFont(titleFont);
                        titleStyle.setAlignment(HorizontalAlignment.CENTER);
                        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                        CellStyle headerStyle = workbook.createCellStyle();
                        headerStyle.setFont(headerFont);
                        headerStyle.setAlignment(HorizontalAlignment.CENTER);
                        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                        headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
                        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        headerStyle.setBorderBottom(BorderStyle.THIN);
                        headerStyle.setBorderTop(BorderStyle.THIN);
                        headerStyle.setBorderLeft(BorderStyle.THIN);
                        headerStyle.setBorderRight(BorderStyle.THIN);

                        CellStyle normalStyle = workbook.createCellStyle();
                        normalStyle.setFont(normalFont);
                        normalStyle.setBorderBottom(BorderStyle.THIN);
                        normalStyle.setBorderTop(BorderStyle.THIN);
                        normalStyle.setBorderLeft(BorderStyle.THIN);
                        normalStyle.setBorderRight(BorderStyle.THIN);

                        CellStyle centerStyle = workbook.createCellStyle();
                        centerStyle.cloneStyleFrom(normalStyle);
                        centerStyle.setAlignment(HorizontalAlignment.CENTER);

                        CellStyle rightStyle = workbook.createCellStyle();
                        rightStyle.cloneStyleFrom(normalStyle);
                        rightStyle.setAlignment(HorizontalAlignment.RIGHT);

                        CellStyle messageStyle = workbook.createCellStyle();
                        messageStyle.cloneStyleFrom(normalStyle);
                        messageStyle.setAlignment(HorizontalAlignment.CENTER);

                        // Dòng tiêu đề
                        Row titleRow = sheet.createRow(0);
                        titleRow.setHeightInPoints(28F);
                        Cell titleCell = titleRow.createCell(0);
                        titleCell.setCellValue("BÁO CÁO TỔNG HỢP NGÀNH");
                        titleCell.setCellStyle(titleStyle);
                        sheet.addMergedRegion(
                                        new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));

                        // Dòng thông tin chung
                        Row infoRow = sheet.createRow(2);
                        Cell infoCell = infoRow.createCell(0);
                        infoCell.setCellValue(
                                        "Địa bàn: " + safeString(report.getRegion()));
                        infoCell.setCellStyle(normalStyle);
                        sheet.addMergedRegion(
                                        new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 2));

                        Row dateRow = sheet.createRow(3);
                        Cell dateCell = dateRow.createCell(0);
                        String fromDate = report.getFromDate() == null
                                        ? "-"
                                        : report.getFromDate().format(DATE_FORMAT);
                        String toDate = report.getToDate() == null
                                        ? "-"
                                        : report.getToDate().format(DATE_FORMAT);
                        dateCell.setCellValue("Thời gian: " + fromDate + " - " + toDate);
                        dateCell.setCellStyle(normalStyle);
                        sheet.addMergedRegion(
                                        new org.apache.poi.ss.util.CellRangeAddress(3, 3, 0, 2));

                        Row orgRow = sheet.createRow(4);
                        cellWithValue(orgRow, 0, "Tổng tổ chức: " + report.getTotalOrganizations(), normalStyle);
                        sheet.addMergedRegion(
                                        new org.apache.poi.ss.util.CellRangeAddress(4, 4, 0, 2));

                        Row shipmentRow = sheet.createRow(5);
                        cellWithValue(shipmentRow, 0, "Tổng lô hàng: " + report.getTotalShipments(), normalStyle);
                        sheet.addMergedRegion(
                                        new org.apache.poi.ss.util.CellRangeAddress(5, 5, 0, 2));

                        Row quantityRow = sheet.createRow(6);
                        cellWithValue(
                                        quantityRow,
                                        0,
                                        "Tổng sản lượng: " + new java.text.DecimalFormat("#,##0")
                                                        .format(report.getTotalQuantity()) + " kg",
                                        normalStyle);
                        sheet.addMergedRegion(
                                        new org.apache.poi.ss.util.CellRangeAddress(6, 6, 0, 2));

                        // Dòng tiêu đề bảng
                        Row headerRow = sheet.createRow(8);
                        for (int i = 0; i < HEADERS.length; i++) {
                                Cell header = headerRow.createCell(i);
                                header.setCellValue(HEADERS[i]);
                                header.setCellStyle(headerStyle);
                        }

                        // Dữ liệu
                        List<ProductBreakdownItem> items = report.getProductBreakdown();
                        int rowIndex = 9;

                        if (items == null || items.isEmpty()) {
                                Row emptyRow = sheet.createRow(rowIndex);
                                Cell emptyCell = emptyRow.createCell(0);
                                emptyCell.setCellValue("Không có dữ liệu");
                                emptyCell.setCellStyle(messageStyle);
                                sheet.addMergedRegion(
                                                new org.apache.poi.ss.util.CellRangeAddress(
                                                                rowIndex, rowIndex, 0, 2));
                        } else {
                                for (ProductBreakdownItem item : items) {
                                        Row row = sheet.createRow(rowIndex++);
                                        cellWithValue(row, 0, safeString(item.getProductCategoryName()), normalStyle);
                                        cellWithValue(row, 1, item.getShipmentCount(), centerStyle);
                                        cellWithValue(row, 2, item.getTotalQuantity(), rightStyle);
                                }
                        }

                        workbook.write(outputStream);
                        byte[] result = outputStream.toByteArray();

                        log.info("Excel report generated successfully. "
                                        + "region={}, rows={}, bytes={}",
                                        report.getRegion(),
                                        items == null ? 0 : items.size(),
                                        result.length);

                        return result;

                } catch (Exception ex) {
                        log.error("Export Excel failed. region={}, fromDate={}, toDate={}",
                                        report.getRegion(),
                                        report.getFromDate(),
                                        report.getToDate(),
                                        ex);
                        throw new BusinessException(EXPORT_ERROR);
                }
        }

        private void cellWithValue(Row row, int col, String value, CellStyle style) {
                Cell cell = row.createCell(col);
                cell.setCellValue(value);
                cell.setCellStyle(style);
        }

        private void cellWithValue(Row row, int col, int value, CellStyle style) {
                Cell cell = row.createCell(col);
                cell.setCellValue(value);
                cell.setCellStyle(style);
        }

        private void cellWithValue(Row row, int col, double value, CellStyle style) {
                Cell cell = row.createCell(col);
                cell.setCellValue(value);
                cell.setCellStyle(style);
        }

        private String safeString(String value) {
                return value == null ? "-" : value;
        }
}