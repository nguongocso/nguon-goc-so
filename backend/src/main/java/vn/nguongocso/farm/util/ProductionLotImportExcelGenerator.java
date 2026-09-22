package vn.nguongocso.farm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Tạo tệp Excel mẫu nhập lô sản xuất.
*/
@Component
public class ProductionLotImportExcelGenerator {
    private static final String SHEET_NAME = "Nhap_lo_san_xuat";

    private static final String ACTIVITY_SHEET_NAME = "DanhMuc";

    private static final String ACTIVITY_NAME_RANGE = "FarmActivityTypes";

    private static final int DATA_START_ROW = 1;

    private static final int DATA_END_ROW = 999;

    private static final String DATE_FORMAT = "dd/MM/yyyy";

    private static final String[] HEADERS = {
            "ten_lo",
            "ma_loai_nong_san",
            "ma_vung_trong",
            "san_luong_du_kien",
            "san_luong_thuc_thu",
            "ngay_gieo_trong",
            "ngay_thu_hoach",
            "hoat_dong_canh_tac",
            "vat_tu",
            "so_luong",
            "don_vi",
            "ngay_thuc_hien",
            "ghi_chu"
    };

    /** Tạo tệp Excel mẫu nhập lô sản xuất. */
    public byte[] generate(
            UUID productCategoryId,
            UUID farmAreaId) {

        validateInput(
                productCategoryId,
                farmAreaId);

        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()) {

            Sheet sheet =
                    workbook.createSheet(SHEET_NAME);

            CellStyle headerStyle =
                    createHeaderStyle(workbook);

            CellStyle sampleStyle =
                    createSampleStyle(workbook);

            CellStyle dateStyle =
                    createDateStyle(workbook);

            createHeader(
                    sheet,
                    headerStyle);

            createSampleRow(
                    sheet,
                    sampleStyle,
                    dateStyle,
                    productCategoryId,
                    farmAreaId);

            createActivityDropdown(
                    workbook,
                    sheet);

            createNumberValidation(sheet);

            createDateValidation(sheet);

            configureColumnWidths(sheet);

            sheet.createFreezePane(
                    0,
                    1);

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            0,
                            DATA_END_ROW,
                            0,
                            HEADERS.length - 1));

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Không thể tạo file Excel mẫu nhập lô sản xuất.",
                    e);
        }
    }

    /** Kiểm tra mã loại nông sản và mã vùng trồng. */
    private void validateInput(
            UUID productCategoryId,
            UUID farmAreaId) {

        if (productCategoryId == null) {

            throw new IllegalArgumentException(
                    "Mã loại nông sản không được để trống.");
        }

        if (farmAreaId == null) {

            throw new IllegalArgumentException(
                    "Mã vùng trồng không được để trống.");
        }
    }

    /** Tạo dòng tiêu đề cho tệp mẫu nhập lô sản xuất. */
    private void createHeader(
            Sheet sheet,
            CellStyle headerStyle) {

        Row headerRow =
                sheet.createRow(0);

        headerRow.setHeightInPoints(25);

        for (int i = 0; i < HEADERS.length; i++) {

            Cell cell =
                    headerRow.createCell(i);

            cell.setCellValue(
                    HEADERS[i]);

            cell.setCellStyle(
                    headerStyle);
        }
    }

    /** Tạo dòng mẫu mang mã loại nông sản và mã vùng trồng. */
    private void createSampleRow(
            Sheet sheet,
            CellStyle sampleStyle,
            CellStyle dateStyle,
            UUID productCategoryId,
            UUID farmAreaId) {

        Row sampleRow =
                sheet.createRow(1);

        sampleRow.setHeightInPoints(22);

        createTextCell(
                sampleRow,
                0,
                "",
                sampleStyle);

        createTextCell(
                sampleRow,
                1,
                productCategoryId.toString(),
                sampleStyle);

        createTextCell(
                sampleRow,
                2,
                farmAreaId.toString(),
                sampleStyle);

        createTextCell(
                sampleRow,
                3,
                "",
                sampleStyle);

        createTextCell(
                sampleRow,
                4,
                "",
                sampleStyle);

        createDateCell(
                sampleRow,
                5,
                dateStyle);

        createDateCell(
                sampleRow,
                6,
                dateStyle);

        Cell activityCell =
                sampleRow.createCell(7);

        activityCell.setCellValue(""
                );

        activityCell.setCellStyle(
                sampleStyle);

        createTextCell(
                sampleRow,
                8,
                "",
                sampleStyle);

        createTextCell(
                sampleRow,
                9,
                "",
                sampleStyle);

        createTextCell(
                sampleRow,
                10,
                "",
                sampleStyle);

        createDateCell(
                sampleRow,
                11,
                dateStyle);

        createTextCell(
                sampleRow,
                12,
                "",
                sampleStyle);
    }

    /** Định dạng chữ trắng nền xanh cho dòng tiêu đề. */
    private CellStyle createHeaderStyle(
            XSSFWorkbook workbook) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);

        font.setColor(
                IndexedColors.WHITE.getIndex());

        style.setFont(font);

        style.setFillForegroundColor(
                IndexedColors.DARK_GREEN.getIndex());

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(
                HorizontalAlignment.CENTER);

        style.setVerticalAlignment(
                VerticalAlignment.CENTER);

        applyBorder(style);

        return style;
    }

    /** Định dạng căn giữa có viền cho dòng dữ liệu. */
    private CellStyle createSampleStyle(
            XSSFWorkbook workbook) {

        CellStyle style =
                workbook.createCellStyle();

        style.setVerticalAlignment(
                VerticalAlignment.CENTER);

        applyBorder(style);

        return style;
    }

    /** Định dạng ô ngày tháng theo dd/MM/yyyy. */
    private CellStyle createDateStyle(
            XSSFWorkbook workbook) {

        CellStyle style =
                workbook.createCellStyle();

        style.setDataFormat(
                workbook
                        .createDataFormat()
                        .getFormat(DATE_FORMAT));

        style.setVerticalAlignment(
                VerticalAlignment.CENTER);

        applyBorder(style);

        return style;
    }

    /** Kẻ viền mỏng cho ô Excel. */
    private void applyBorder(
            CellStyle style) {

        style.setBorderTop(
                BorderStyle.THIN);

        style.setBorderBottom(
                BorderStyle.THIN);

        style.setBorderLeft(
                BorderStyle.THIN);

        style.setBorderRight(
                BorderStyle.THIN);
    }

    /** Tạo ô Excel dạng văn bản. */
    private void createTextCell(
            Row row,
            int columnIndex,
            String value,
            CellStyle style) {

        Cell cell =
                row.createCell(columnIndex);

        cell.setCellValue(
                value == null
                        ? ""
                        : value);

        cell.setCellStyle(style);
    }

    /** Tạo ô Excel dạng ngày tháng. */
    private void createDateCell(
            Row row,
            int columnIndex,
            CellStyle style) {

        Cell cell = row.createCell(columnIndex);

        cell.setCellValue("");
        cell.setCellStyle(style);
    }

    /** Tạo danh sách chọn hoạt động canh tác cho cột H. */
    private void createActivityDropdown(
            XSSFWorkbook workbook,
            Sheet sheet) {

        Sheet activitySheet =
                workbook.createSheet(
                        ACTIVITY_SHEET_NAME);

        FarmActivityType[] types =
                FarmActivityType.values();

        for (int i = 0; i < types.length; i++) {

            Row row =
                    activitySheet.createRow(i);

            Cell cell =
                    row.createCell(0);

            cell.setCellValue(
                    types[i].name());
        }

        XSSFName namedRange =
                workbook.createName();

        namedRange.setNameName(
                ACTIVITY_NAME_RANGE);

        namedRange.setRefersToFormula(
                ACTIVITY_SHEET_NAME
                        + "!$A$1:$A$"
                        + types.length);

        DataValidationHelper helper =
                sheet.getDataValidationHelper();

        DataValidationConstraint constraint =
                helper.createFormulaListConstraint(
                        ACTIVITY_NAME_RANGE);

        CellRangeAddressList addressList =
                new CellRangeAddressList(
                        DATA_START_ROW,
                        DATA_END_ROW,
                        7,
                        7);

        DataValidation validation =
                helper.createValidation(
                        constraint,
                        addressList);

        validation.setShowErrorBox(
                true);

        validation.setShowPromptBox(
                true);

        validation.createErrorBox(
                "Giá trị không hợp lệ",
                "Vui lòng chọn hoạt động từ danh sách.");

        validation.createPromptBox(
                "Hoạt động canh tác",
                "Vui lòng chọn một hoạt động trong danh sách.");

        sheet.addValidationData(
                validation);
    }

    /** Giới hạn các cột sản lượng và số lượng không âm. */
    private void createNumberValidation(
            Sheet sheet) {

        DataValidationHelper helper =
                sheet.getDataValidationHelper();

        DataValidationConstraint constraint =
                helper.createDecimalConstraint(
                        DataValidationConstraint.OperatorType.GREATER_OR_EQUAL,
                        "0",
                        null);

        addNumberValidation(
                sheet,
                helper,
                constraint,
                3,
                "Sản lượng dự kiến");

        addNumberValidation(
                sheet,
                helper,
                constraint,
                4,
                "Sản lượng thực thu");

        addNumberValidation(
                sheet,
                helper,
                constraint,
                9,
                "Số lượng");
    }

    /** Áp dụng kiểm tra số không âm cho một cột. */
    private void addNumberValidation(
            Sheet sheet,
            DataValidationHelper helper,
            DataValidationConstraint constraint,
            int columnIndex,
            String fieldName) {

        CellRangeAddressList addressList =
                new CellRangeAddressList(
                        DATA_START_ROW,
                        DATA_END_ROW,
                        columnIndex,
                        columnIndex);

        DataValidation validation =
                helper.createValidation(
                        constraint,
                        addressList);

        validation.setShowErrorBox(
                true);

        validation.setShowPromptBox(
                true);

        validation.createErrorBox(
                "Giá trị không hợp lệ",
                fieldName
                + " phải là số lớn hơn hoặc bằng 0.");

        validation.createPromptBox(
                fieldName,
                "Vui lòng nhập số lớn hơn hoặc bằng 0.");

        sheet.addValidationData(
                validation);
    }

    /** Giới hạn các cột ngày trong khoảng năm 1900–9999. */
    private void createDateValidation(
            Sheet sheet) {

        DataValidationHelper helper =
                sheet.getDataValidationHelper();

        DataValidationConstraint constraint =
                helper.createDateConstraint(
                        DataValidationConstraint.OperatorType.BETWEEN,
                        "DATE(1900,1,1)",
                        "DATE(9999,12,31)",
                        DATE_FORMAT);

        addDateValidation(
                sheet,
                helper,
                constraint,
                5,
                "Ngày gieo trồng");

        addDateValidation(
                sheet,
                helper,
                constraint,
                6,
                "Ngày thu hoạch");

        addDateValidation(
                sheet,
                helper,
                constraint,
                11,
                "Ngày thực hiện");
    }

    /** Áp dụng kiểm tra định dạng ngày cho một cột. */
    private void addDateValidation(
            Sheet sheet,
            DataValidationHelper helper,
            DataValidationConstraint constraint,
            int columnIndex,
            String fieldName) {

        CellRangeAddressList addressList =
                new CellRangeAddressList(
                        DATA_START_ROW,
                        DATA_END_ROW,
                        columnIndex,
                        columnIndex);

        DataValidation validation =
                helper.createValidation(
                        constraint,
                        addressList);

        validation.setShowErrorBox(
                true);

        validation.setShowPromptBox(
                true);

        validation.createErrorBox(
                "Ngày không hợp lệ",
                fieldName
                        + " phải có định dạng dd/MM/yyyy.");

        validation.createPromptBox(
                fieldName,
                "Vui lòng nhập ngày theo định dạng dd/MM/yyyy.");

        sheet.addValidationData(
                validation);
    }
    /** Chỉnh độ rộng 13 cột theo nội dung. */
    private void configureColumnWidths(
            Sheet sheet) {
        int[] widths = {25,42,42,22,22,18,18,25,25,15,15,18,35};

        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i,widths[i] * 256);
        }
    }
}