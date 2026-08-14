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

@Component
public class ProductionLotImportExcelGenerator {

    private static final String SHEET_NAME = "Nhap_lo_san_xuat";

    /**
     * Sheet chứa danh mục dùng làm nguồn cho dropdown.
     */
    private static final String ACTIVITY_SHEET_NAME = "DanhMuc";

    /**
     * Named Range dùng cho dropdown hoạt động canh tác.
     */
    private static final String ACTIVITY_NAME_RANGE = "FarmActivityTypes";

    /**
     * Dòng dữ liệu bắt đầu.
     *
     * Excel:
     * dòng 1 = header
     * dòng 2 = dữ liệu đầu tiên
     *
     * POI sử dụng index 0-based:
     * dòng 2 Excel = index 1
     */
    private static final int DATA_START_ROW = 1;

    /**
     * Dòng dữ liệu cuối cùng.
     *
     * Excel dòng 1000 = POI index 999.
     */
    private static final int DATA_END_ROW = 999;

    /**
     * Format ngày tháng sử dụng trong Excel.
     */
    private static final String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Header của file Excel.
     */
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

    /**
     * Tạo file Excel mẫu nhập hàng loạt lô sản xuất.
     *
     * @param productCategoryId mã loại nông sản
     * @param farmAreaId        mã vùng trồng
     * @return nội dung file Excel
     */
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

            // =====================================================
            // 1. TẠO SHEET CHÍNH
            // =====================================================

            Sheet sheet =
                    workbook.createSheet(SHEET_NAME);

            // =====================================================
            // 2. TẠO STYLE
            // =====================================================

            CellStyle headerStyle =
                    createHeaderStyle(workbook);

            CellStyle sampleStyle =
                    createSampleStyle(workbook);

            CellStyle dateStyle =
                    createDateStyle(workbook);

            // =====================================================
            // 3. TẠO HEADER
            // =====================================================

            createHeader(
                    sheet,
                    headerStyle);

            // =====================================================
            // 4. TẠO DÒNG DỮ LIỆU MẪU
            // =====================================================

            createSampleRow(
                    sheet,
                    sampleStyle,
                    dateStyle,
                    productCategoryId,
                    farmAreaId);

            // =====================================================
            // 5. DROPDOWN HOẠT ĐỘNG CANH TÁC
            // =====================================================

            createActivityDropdown(
                    workbook,
                    sheet);

            // =====================================================
            // 6. VALIDATION CHO CÁC CỘT SỐ
            // =====================================================

            createNumberValidation(sheet);

            // =====================================================
            // 7. VALIDATION CHO CÁC CỘT NGÀY
            // =====================================================

            createDateValidation(sheet);

            // =====================================================
            // 8. FORMAT CỘT
            // =====================================================

            configureColumnWidths(sheet);

            // =====================================================
            // 9. FREEZE HEADER
            // =====================================================

            sheet.createFreezePane(
                    0,
                    1);

            // =====================================================
            // 10. AUTO FILTER
            // =====================================================

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            0,
                            DATA_END_ROW,
                            0,
                            HEADERS.length - 1));

            // =====================================================
            // 11. GHI FILE
            // =====================================================

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Không thể tạo file Excel mẫu nhập lô sản xuất.",
                    e);
        }
    }

    // =============================================================
    // VALIDATE INPUT
    // =============================================================

    /**
     * Kiểm tra tham số đầu vào.
     */
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

    // =============================================================
    // HEADER
    // =============================================================

    /**
     * Tạo dòng header.
     */
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

    // =============================================================
    // SAMPLE ROW
    // =============================================================

    /**
     * Tạo dòng dữ liệu mẫu.
     */
    private void createSampleRow(
            Sheet sheet,
            CellStyle sampleStyle,
            CellStyle dateStyle,
            UUID productCategoryId,
            UUID farmAreaId) {

        Row sampleRow =
                sheet.createRow(1);

        sampleRow.setHeightInPoints(22);

        // =====================================================
        // A - ten_lo
        // =====================================================

        createTextCell(
                sampleRow,
                0,
                "",
                sampleStyle);
        

        // =====================================================
        // B - ma_loai_nong_san
        // =====================================================

        createTextCell(
                sampleRow,
                1,
                productCategoryId.toString(),
                sampleStyle);

        // =====================================================
        // C - ma_vung_trong
        // =====================================================

        createTextCell(
                sampleRow,
                2,
                farmAreaId.toString(),
                sampleStyle);

        // =====================================================
        // D - san_luong_du_kien
        // =====================================================

        createTextCell(
                sampleRow,
                3,
                "",
                sampleStyle);

        // =====================================================
        // E - san_luong_thuc_thu
        // =====================================================

        createTextCell(
                sampleRow,
                4,
                "",
                sampleStyle);

        // =====================================================
        // F - ngay_gieo_trong
        // =====================================================

        createDateCell(
                sampleRow,
                5,
                dateStyle);

        // =====================================================
        // G - ngay_thu_hoach
        // =====================================================

        createDateCell(
                sampleRow,
                6,
                dateStyle);

        // =====================================================
        // H - hoat_dong_canh_tac
        // =====================================================

        Cell activityCell =
                sampleRow.createCell(7);

        activityCell.setCellValue(""
                );

        activityCell.setCellStyle(
                sampleStyle);

        // =====================================================
        // I - vat_tu
        // =====================================================

        createTextCell(
                sampleRow,
                8,
                "",
                sampleStyle);

        // =====================================================
        // J - so_luong
        // =====================================================

        createTextCell(
                sampleRow,
                9,
                "",
                sampleStyle);

        // =====================================================
        // K - don_vi
        // =====================================================

        createTextCell(
                sampleRow,
                10,
                "",
                sampleStyle);

        // =====================================================
        // L - ngay_thuc_hien
        // =====================================================

        createDateCell(
                sampleRow,
                11,
                dateStyle);

        // =====================================================
        // M - ghi_chu
        // =====================================================

        createTextCell(
                sampleRow,
                12,
                "",
                sampleStyle);
    }

    // =============================================================
    // STYLE
    // =============================================================

    /**
     * Style cho header.
     */
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

    /**
     * Style cho dữ liệu.
     */
    private CellStyle createSampleStyle(
            XSSFWorkbook workbook) {

        CellStyle style =
                workbook.createCellStyle();

        style.setVerticalAlignment(
                VerticalAlignment.CENTER);

        applyBorder(style);

        return style;
    }

    /**
     * Style cho ngày tháng.
     *
     * Hiển thị:
     *
     * dd/MM/yyyy
     */
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

    /**
     * Thiết lập border cho cell.
     */
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

    // =============================================================
    // CELL
    // =============================================================

    /**
     * Tạo cell dạng text.
     */
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

    /**
     * Tạo cell ngày tháng.
     */
    private void createDateCell(
            Row row,
            int columnIndex,
            CellStyle style) {

        Cell cell =
                row.createCell(columnIndex);

        /*
         * Để trống nhưng vẫn áp dụng format
         * dd/MM/yyyy.
         */
        cell.setCellValue("");

        cell.setCellStyle(style);
    }

    // =============================================================
    // ACTIVITY DROPDOWN
    // =============================================================

    /**
     * Tạo dropdown cho:
     *
     * H2:H1000
     *
     * Nguồn dữ liệu lấy từ FarmActivityType.
     */
    private void createActivityDropdown(
            XSSFWorkbook workbook,
            Sheet sheet) {

        // =====================================================
        // 1. TẠO SHEET DANH MỤC
        // =====================================================

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

        // =====================================================
        // 2. TẠO NAMED RANGE
        // =====================================================

        XSSFName namedRange =
                workbook.createName();

        namedRange.setNameName(
                ACTIVITY_NAME_RANGE);

        namedRange.setRefersToFormula(
                ACTIVITY_SHEET_NAME
                        + "!$A$1:$A$"
                        + types.length);

        // =====================================================
        // 3. VALIDATION HELPER
        // =====================================================

        DataValidationHelper helper =
                sheet.getDataValidationHelper();

        // =====================================================
        // 4. FORMULA LIST CONSTRAINT
        // =====================================================

        DataValidationConstraint constraint =
                helper.createFormulaListConstraint(
                        ACTIVITY_NAME_RANGE);

        // =====================================================
        // 5. ÁP DỤNG H2:H1000
        // =====================================================

        CellRangeAddressList addressList =
                new CellRangeAddressList(
                        DATA_START_ROW,
                        DATA_END_ROW,
                        7,
                        7);

        // =====================================================
        // 6. TẠO VALIDATION
        // =====================================================

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

        // =====================================================
        // 7. THÊM VALIDATION
        // =====================================================

        sheet.addValidationData(
                validation);
    }

    // =============================================================
    // NUMBER VALIDATION
    // =============================================================

    /**
     * Thiết lập validation số cho:
     *
     * D2:D1000 - san_luong_du_kien
     * E2:E1000 - san_luong_thuc_thu
     * J2:J1000 - so_luong
     *
     * Cho phép:
     *
     * 0
     * 10
     * 10.5
     * 100.25
     *
     * Không cho phép:
     *
     * -1
     * -10.5
     * abc
     *
     * Các ô vẫn có thể để trống.
     */
    private void createNumberValidation(
            Sheet sheet) {

        DataValidationHelper helper =
                sheet.getDataValidationHelper();

        /*
         * Apache POI 5.4.1:
         *
         * createDecimalConstraint() có dạng:
         *
         * createDecimalConstraint(
         *     operatorType,
         *     formula1,
         *     formula2
         * )
         *
         * Không truyền ValidationType.DECIMAL.
         */
        DataValidationConstraint constraint =
                helper.createDecimalConstraint(
                        DataValidationConstraint.OperatorType.GREATER_OR_EQUAL,
                        "0",
                        null);

        // =====================================================
        // D - san_luong_du_kien
        // =====================================================

        addNumberValidation(
                sheet,
                helper,
                constraint,
                3,
                "Sản lượng dự kiến");

        // =====================================================
        // E - san_luong_thuc_thu
        // =====================================================

        addNumberValidation(
                sheet,
                helper,
                constraint,
                4,
                "Sản lượng thực thu");

        // =====================================================
        // J - so_luong
        // =====================================================

        addNumberValidation(
                sheet,
                helper,
                constraint,
                9,
                "Số lượng");
    }

    /**
     * Áp dụng validation số cho một cột.
     */
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

    // =============================================================
    // DATE VALIDATION
    // =============================================================

    /**
     * Thiết lập validation ngày tháng cho:
     *
     * F2:F1000 - ngay_gieo_trong
     * G2:G1000 - ngay_thu_hoach
     * L2:L1000 - ngay_thuc_hien
     *
     * Định dạng hiển thị:
     *
     * dd/MM/yyyy
     *
     * Ví dụ:
     *
     * 08/08/2026
     * 15/09/2026
     *
     * Lưu ý:
     *
     * Excel Desktop không tự hiển thị popup lịch
     * chỉ bằng Data Validation của Apache POI.
     *
     * Validation này đảm bảo dữ liệu nhập vào
     * phải là ngày hợp lệ.
     */
    private void createDateValidation(
            Sheet sheet) {

        DataValidationHelper helper =
                sheet.getDataValidationHelper();

        /*
         * Apache POI 5.4.1:
         *
         * createDateConstraint() có 4 tham số:
         *
         * 1. operatorType
         * 2. formula1
         * 3. formula2
         * 4. dateFormat
         *
         * Do đó KHÔNG được gọi:
         *
         * createDateConstraint(
         *     operator,
         *     formula1,
         *     formula2
         * )
         *
         * vì sẽ gây lỗi compile.
         */

        DataValidationConstraint constraint =
                helper.createDateConstraint(
                        DataValidationConstraint.OperatorType.BETWEEN,
                        "DATE(1900,1,1)",
                        "DATE(9999,12,31)",
                        DATE_FORMAT);

        // =====================================================
        // F - ngay_gieo_trong
        // =====================================================

        addDateValidation(
                sheet,
                helper,
                constraint,
                5,
                "Ngày gieo trồng");

        // =====================================================
        // G - ngay_thu_hoach
        // =====================================================

        addDateValidation(
                sheet,
                helper,
                constraint,
                6,
                "Ngày thu hoạch");

        // =====================================================
        // L - ngay_thuc_hien
        // =====================================================

        addDateValidation(
                sheet,
                helper,
                constraint,
                11,
                "Ngày thực hiện");
    }

    /**
     * Áp dụng validation ngày cho một cột.
     */
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

    // =============================================================
    // COLUMN WIDTH
    // =============================================================

    /**
     * Thiết lập độ rộng các cột.
     */
    private void configureColumnWidths(
            Sheet sheet) {

        int[] widths = {
                25, // A - ten_lo
                42, // B - ma_loai_nong_san
                42, // C - ma_vung_trong
                22, // D - san_luong_du_kien
                22, // E - san_luong_thuc_thu
                18, // F - ngay_gieo_trong
                18, // G - ngay_thu_hoach
                25, // H - hoat_dong_canh_tac
                25, // I - vat_tu
                15, // J - so_luong
                15, // K - don_vi
                18, // L - ngay_thuc_hien
                35  // M - ghi_chu
        };

        for (int i = 0; i < widths.length; i++) {

            sheet.setColumnWidth(
                    i,
                    widths[i] * 256);
        }
    }
}