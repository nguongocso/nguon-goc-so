package vn.nguongocso.farm.util;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Implementation đọc file Excel nhập lô sản xuất.
 *
 * <p>
 * File Excel được tạo bởi:
 * {@link ProductionLotImportExcelGenerator}
 *
 * <p>
 * Cấu trúc cột:
 *
 * <pre>
 * A - ten_lo
 * B - ma_loai_nong_san
 * C - ma_vung_trong
 * D - san_luong_du_kien
 * E - san_luong_thuc_thu
 * F - ngay_gieo_trong
 * G - ngay_thu_hoach
 * H - hoat_dong_canh_tac
 * I - vat_tu
 * J - so_luong
 * K - don_vi
 * L - ngay_thuc_hien
 * M - ghi_chu
 * </pre>
 *
 * <p>
 * Kiểu dữ liệu:
 *
 * <pre>
 * D - Double
 * E - Double
 * J - Double
 * F - LocalDate
 * G - LocalDate
 * L - LocalDate
 * H - FarmActivityType
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class ProductionLotImportFileParserImpl
        implements ProductionLotImportFileParser {

    /**
     * Tên sheet Excel chính.
     */
    private static final String SHEET_NAME =
            "Nhap_lo_san_xuat";

    /**
     * Định dạng ngày chuẩn.
     *
     * <p>
     * Người dùng nhập:
     *
     * <pre>
     * 28/09/2026
     * </pre>
     *
     * Không sử dụng:
     *
     * <pre>
     * 9/28/2026
     * </pre>
     */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter
                    .ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    /**
     * DataFormatter của Apache POI.
     *
     * <p>
     * Dùng để đọc nội dung cell theo giá trị hiển thị của Excel.
     */
    private final DataFormatter dataFormatter =
            new DataFormatter(Locale.US);

    // =========================================================
    // PARSE FILE
    // =========================================================

    /**
     * Parse file Excel.
     *
     * @param file file Excel .xlsx
     * @return danh sách dòng dữ liệu
     */
    @Override
    public List<ProductionLotImportRow> parse(
            MultipartFile file) {

        validateFile(file);

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || !originalFileName
                        .toLowerCase(Locale.ROOT)
                        .endsWith(".xlsx")) {

            throw new BusinessException(
                    "Chỉ hỗ trợ file Excel định dạng .xlsx.");
        }

        try (
                InputStream inputStream =
                        file.getInputStream();

                Workbook workbook =
                        new XSSFWorkbook(inputStream)
        ) {

            // =================================================
            // LẤY SHEET
            // =================================================

            Sheet sheet =
                    workbook.getSheet(SHEET_NAME);

            if (sheet == null) {

                throw new BusinessException(
                        "File Excel không có sheet '"
                                + SHEET_NAME
                                + "'.");
            }

            // =================================================
            // VALIDATE HEADER
            // =================================================

            validateHeader(sheet);

            // =================================================
            // PARSE DATA
            // =================================================

            return parseRows(sheet);

        } catch (IOException e) {

            throw new BusinessException(
                    "Không thể đọc file Excel nhập lô sản xuất.");
        }
    }

    // =========================================================
    // VALIDATE FILE
    // =========================================================

    /**
     * Kiểm tra file đầu vào.
     */
    private void validateFile(
            MultipartFile file) {

        if (file == null
                || file.isEmpty()) {

            throw new BusinessException(
                    "File nhập lô sản xuất không được để trống.");
        }
    }

    // =========================================================
    // HEADER
    // =========================================================

    /**
     * Kiểm tra header của file Excel.
     *
     * <p>
     * Header phải đúng thứ tự với file mẫu.
     */
    private void validateHeader(
            Sheet sheet) {

        Row headerRow =
                sheet.getRow(0);

        if (headerRow == null) {

            throw new BusinessException(
                    "File Excel không có dòng header.");
        }

        String[] expectedHeaders = {
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

        for (int i = 0;
             i < expectedHeaders.length;
             i++) {

            Cell cell =
                    headerRow.getCell(i);

            String actual =
                    getCellString(cell);

            if (!expectedHeaders[i]
                    .equals(actual)) {

                throw new BusinessException(
                        "Header cột "
                                + getExcelColumnName(i)
                                + " không hợp lệ. "
                                + "Yêu cầu: "
                                + expectedHeaders[i]
                                + ".");
            }
        }
    }

    // =========================================================
    // PARSE ROWS
    // =========================================================

    /**
     * Đọc toàn bộ các dòng dữ liệu.
     *
     * <p>
     * Dòng 1 Excel là header.
     *
     * <p>
     * POI:
     *
     * <pre>
     * index 0 = Excel row 1
     * index 1 = Excel row 2
     * </pre>
     */
    private List<ProductionLotImportRow> parseRows(
            Sheet sheet) {

        List<ProductionLotImportRow> rows =
                new ArrayList<>();

        for (int rowIndex = 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row excelRow =
                    sheet.getRow(rowIndex);

            // Bỏ qua dòng hoàn toàn rỗng
            if (isEmptyRow(excelRow)) {
                continue;
            }

            /*
             * rowNumber là số dòng Excel thực tế.
             *
             * POI index 1
             * -> Excel row 2
             */
            int rowNumber =
                    rowIndex + 1;

            ProductionLotImportRow row =
                    parseRow(
                            excelRow,
                            rowNumber);

            rows.add(row);
        }

        return rows;
    }

    // =========================================================
    // PARSE SINGLE ROW
    // =========================================================

    /**
     * Parse một dòng Excel.
     */
    private ProductionLotImportRow parseRow(
            Row row,
            int rowNumber) {

        return ProductionLotImportRow.builder()

                // =================================================
                // A - ten_lo
                // =================================================

                .rowNumber(rowNumber)

                .lotName(
                        getCellString(
                                row.getCell(0)))

                // =================================================
                // B - ma_loai_nong_san
                // =================================================

                .productCategoryId(
                        getCellString(
                                row.getCell(1)))

                // =================================================
                // C - ma_vung_trong
                // =================================================

                .farmAreaId(
                        getCellString(
                                row.getCell(2)))

                // =================================================
                // D - san_luong_du_kien
                // Double
                // =================================================

                .expectedQuantity(
                        getDouble(
                                row.getCell(3),
                                "san_luong_du_kien",
                                rowNumber))

                // =================================================
                // E - san_luong_thuc_thu
                // Double
                // =================================================

                .actualQuantity(
                        getDouble(
                                row.getCell(4),
                                "san_luong_thuc_thu",
                                rowNumber))

                // =================================================
                // F - ngay_gieo_trong
                // LocalDate
                // =================================================

                .plantingDate(
                        getDate(
                                row.getCell(5),
                                "ngay_gieo_trong",
                                rowNumber))

                // =================================================
                // G - ngay_thu_hoach
                // LocalDate
                // =================================================

                .harvestDate(
                        getDate(
                                row.getCell(6),
                                "ngay_thu_hoach",
                                rowNumber))

                // =================================================
                // H - hoat_dong_canh_tac
                // FarmActivityType
                // =================================================

                .activityType(
                        getActivityType(
                                row.getCell(7),
                                rowNumber))

                // =================================================
                // I - vat_tu
                // =================================================

                .material(
                        getCellString(
                                row.getCell(8)))

                // =================================================
                // J - so_luong
                // Double
                // =================================================

                .quantity(
                        getDouble(
                                row.getCell(9),
                                "so_luong",
                                rowNumber))

                // =================================================
                // K - don_vi
                // =================================================

                .unit(
                        getCellString(
                                row.getCell(10)))

                // =================================================
                // L - ngay_thuc_hien
                // LocalDate
                // =================================================

                .executedDate(
                        getDate(
                                row.getCell(11),
                                "ngay_thuc_hien",
                                rowNumber))

                // =================================================
                // M - ghi_chu
                // =================================================

                .note(
                        getCellString(
                                row.getCell(12)))

                .build();
    }

    // =========================================================
    // STRING
    // =========================================================

    /**
     * Đọc cell dạng String.
     *
     * @param cell cell Excel
     * @return String hoặc null nếu rỗng
     */
    private String getCellString(
            Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType()
                == CellType.BLANK) {

            return null;
        }

        String value =
                dataFormatter.formatCellValue(cell);

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    // =========================================================
    // DOUBLE
    // =========================================================

    /**
     * Đọc số dạng Double từ Excel.
     *
     * <p>
     * Hỗ trợ:
     *
     * <pre>
     * 10
     * 10.5
     * 100.25
     * </pre>
     *
     * <p>
     * Không hỗ trợ:
     *
     * <pre>
     * abc
     * 10abc
     * </pre>
     *
     * <p>
     * Nếu Excel lưu cell là NUMERIC thì lấy trực tiếp
     * {@code getNumericCellValue()}.
     *
     * <p>
     * Nếu Excel lưu cell là STRING thì parse bằng
     * {@link Double#parseDouble(String)}.
     */
    private Double getDouble(
            Cell cell,
            String fieldName,
            int rowNumber) {

        if (cell == null
                || cell.getCellType()
                        == CellType.BLANK) {

            return null;
        }

        try {

            // =================================================
            // EXCEL NUMERIC
            // =================================================

            if (cell.getCellType()
                    == CellType.NUMERIC) {

                double value =
                        cell.getNumericCellValue();

                if (Double.isNaN(value)
                        || Double.isInfinite(value)) {

                    throw new NumberFormatException();
                }

                return value;
            }

            // =================================================
            // STRING
            // =================================================

            String value =
                    getCellString(cell);

            if (value == null
                    || value.isBlank()) {

                return null;
            }

            /*
             * Không tự động thay "," thành "."
             *
             * Ví dụ:
             *
             * 10.5 -> hợp lệ
             * 100.25 -> hợp lệ
             *
             * 10,5 -> không tự động chuyển.
             */
            double result =
                    Double.parseDouble(
                            value.trim());

            if (Double.isNaN(result)
                    || Double.isInfinite(result)) {

                throw new NumberFormatException();
            }

            return result;

        } catch (NumberFormatException e) {

            throw new BusinessException(
                    "Dòng "
                            + rowNumber
                            + ": "
                            + fieldName
                            + " phải là số.");
        }
    }

    // =========================================================
    // DATE
    // =========================================================

    /**
     * Đọc ngày từ Excel.
     *
     * <p>
     * Hỗ trợ hai trường hợp:
     *
     * <ol>
     *     <li>
     *         Excel lưu ngày dưới dạng numeric date.
     *     </li>
     *     <li>
     *         Người dùng nhập text:
     *         {@code dd/MM/yyyy}.
     *     </li>
     * </ol>
     *
     * <p>
     * Ví dụ hợp lệ:
     *
     * <pre>
     * 28/09/2026
     * </pre>
     *
     * <p>
     * Không yêu cầu:
     *
     * <pre>
     * 9/28/2026
     * </pre>
     */
    private LocalDate getDate(
            Cell cell,
            String fieldName,
            int rowNumber) {

        if (cell == null
                || cell.getCellType()
                        == CellType.BLANK) {

            return null;
        }

        try {

            // =================================================
            // TRƯỜNG HỢP EXCEL DATE / NUMERIC
            // =================================================

            if (cell.getCellType()
                    == CellType.NUMERIC) {

                double numericValue =
                        cell.getNumericCellValue();

                /*
                 * Các cột F/G/L là cột ngày.
                 *
                 * Excel thường lưu ngày dưới dạng:
                 *
                 * 463... -> serial date
                 *
                 * Không phụ thuộc việc cell có đang được
                 * format Date hay không.
                 */
                if (DateUtil.isValidExcelDate(
                        numericValue)) {

                    return DateUtil
                            .getLocalDateTime(
                                    numericValue)
                            .toLocalDate();
                }

                throw new DateTimeParseException(
                        "Invalid Excel date",
                        String.valueOf(numericValue),
                        0);
            }

            // =================================================
            // TRƯỜNG HỢP STRING
            // =================================================

            String value =
                    getCellString(cell);

            if (value == null
                    || value.isBlank()) {

                return null;
            }

            /*
             * Chỉ chấp nhận đúng:
             *
             * dd/MM/yyyy
             *
             * Ví dụ:
             *
             * 28/09/2026
             *
             * Không chấp nhận:
             *
             * 9/28/2026
             * 28-09-2026
             * 2026/09/28
             */
            return LocalDate.parse(
                    value.trim(),
                    DATE_FORMATTER);

        } catch (DateTimeParseException e) {

            throw new BusinessException(
                    "Dòng "
                            + rowNumber
                            + ": "
                            + fieldName
                            + " phải có định dạng dd/MM/yyyy.");
        }
    }

    // =========================================================
    // ACTIVITY TYPE
    // =========================================================

    /**
     * Đọc hoạt động canh tác.
     *
     * <p>
     * Giá trị phải tồn tại trong FarmActivityType.
     */
    private FarmActivityType getActivityType(
            Cell cell,
            int rowNumber) {

        String value =
                getCellString(cell);

        /*
         * Không nhập hoạt động thì cho phép null.
         */
        if (value == null
                || value.isBlank()) {

            return null;
        }

        try {

            return FarmActivityType.valueOf(
                    value.trim()
                            .toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException e) {

            throw new BusinessException(
                    "Dòng "
                            + rowNumber
                            + ": hoạt động canh tác '"
                            + value
                            + "' không hợp lệ.");
        }
    }

    // =========================================================
    // EMPTY ROW
    // =========================================================

    /**
     * Kiểm tra một dòng có hoàn toàn rỗng hay không.
     *
     * <p>
     * File mẫu có thể có nhiều dòng trống phía dưới,
     * vì vậy chỉ xử lý những dòng có dữ liệu.
     */
    private boolean isEmptyRow(
            Row row) {

        if (row == null) {
            return true;
        }

        /*
         * File có 13 cột:
         *
         * A -> M
         */
        for (int i = 0; i < 13; i++) {

            Cell cell =
                    row.getCell(i);

            if (cell == null) {
                continue;
            }

            if (cell.getCellType()
                    == CellType.BLANK) {

                continue;
            }

            String value =
                    getCellString(cell);

            if (value != null
                    && !value.isBlank()) {

                return false;
            }
        }

        return true;
    }

    // =========================================================
    // EXCEL COLUMN
    // =========================================================

    /**
     * Chuyển column index thành tên cột Excel.
     *
     * <pre>
     * 0  -> A
     * 1  -> B
     * 2  -> C
     * 25 -> Z
     * 26 -> AA
     * </pre>
     *
     * @param columnIndex index 0-based
     * @return tên cột Excel
     */
    private String getExcelColumnName(
            int columnIndex) {

        StringBuilder result =
                new StringBuilder();

        int index =
                columnIndex + 1;

        while (index > 0) {

            int remainder =
                    (index - 1) % 26;

            result.insert(
                    0,
                    (char) ('A' + remainder));

            index =
                    (index - 1) / 26;
        }

        return result.toString();
    }
}