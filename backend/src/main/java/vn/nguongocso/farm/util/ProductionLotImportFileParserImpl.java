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

import lombok.RequiredArgsConstructor;

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

import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Đọc tệp Excel nhập lô sản xuất.
*/
@Component
@RequiredArgsConstructor
public class ProductionLotImportFileParserImpl
        implements ProductionLotImportFileParser {
    private static final String SHEET_NAME =
            "Nhap_lo_san_xuat";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter
                    .ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final DataFormatter dataFormatter =
            new DataFormatter(Locale.US);

    /** Kiểm tra và phân tích tệp Excel nhập lô sản xuất. */
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
            Sheet sheet =
                    workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new BusinessException(
                        "File Excel không có sheet '"
                                + SHEET_NAME
                                + "'.");
            }
            validateHeader(sheet);

            return parseRows(sheet);
        } catch (IOException e) {
            throw new BusinessException(
                    "Không thể đọc file Excel nhập lô sản xuất.");
        }
    }
    /** Kiểm tra tệp nhập lô sản xuất không rỗng. */
    private void validateFile(
            MultipartFile file) {
        if (file == null
                || file.isEmpty()) {
            throw new BusinessException(
                    "File nhập lô sản xuất không được để trống.");
        }
    }
    /** Kiểm tra thứ tự 13 cột tiêu đề theo tệp mẫu. */
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
    /** Đọc các dòng nhập lô sản xuất, bỏ qua dòng trống. */
    private List<ProductionLotImportRow> parseRows(
            Sheet sheet) {
        List<ProductionLotImportRow> rows =
                new ArrayList<>();

        for (int rowIndex = 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {
            Row excelRow =
                    sheet.getRow(rowIndex);
            if (isEmptyRow(excelRow)) {
                continue;
            }
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
    /** Chuyển một dòng Excel thành dòng nhập lô sản xuất. */
    private ProductionLotImportRow parseRow(
            Row row,
            int rowNumber) {
        return ProductionLotImportRow.builder()

                .rowNumber(rowNumber)

                .lotName(
                        getCellString(
                                row.getCell(0)))

                .productCategoryId(
                        getCellString(
                                row.getCell(1)))

                .farmAreaId(
                        getCellString(
                                row.getCell(2)))

                .expectedQuantity(
                        getDouble(
                                row.getCell(3),
                                "san_luong_du_kien",
                                rowNumber))

                .actualQuantity(
                        getDouble(
                                row.getCell(4),
                                "san_luong_thuc_thu",
                                rowNumber))

                .plantingDate(
                        getDate(
                                row.getCell(5),
                                "ngay_gieo_trong",
                                rowNumber))

                .harvestDate(
                        getDate(
                                row.getCell(6),
                                "ngay_thu_hoach",
                                rowNumber))

                .activityType(
                        getActivityType(
                                row.getCell(7),
                                rowNumber))

                .material(
                        getCellString(
                                row.getCell(8)))

                .quantity(
                        getDouble(
                                row.getCell(9),
                                "so_luong",
                                rowNumber))

                .unit(
                        getCellString(
                                row.getCell(10)))

                .executedDate(
                        getDate(
                                row.getCell(11),
                                "ngay_thuc_hien",
                                rowNumber))

                .note(
                        getCellString(
                                row.getCell(12)))

                .build();
    }
    /** Đọc ô Excel theo giá trị hiển thị, rỗng trả về null. */
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
    /** Chuyển ô Excel thành số, sai ghi rõ số dòng. */
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
            String value =
                    getCellString(cell);

            if (value == null
                    || value.isBlank()) {
                return null;
            }
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
    /** Chuyển ô Excel thành ngày, sai ghi rõ số dòng. */
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
            if (cell.getCellType()
                    == CellType.NUMERIC) {
                double numericValue =
                        cell.getNumericCellValue();

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
            String value =
                    getCellString(cell);

            if (value == null
                    || value.isBlank()) {
                return null;
            }
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
    /** Chuyển ô Excel thành loại hoạt động canh tác. */
    private FarmActivityType getActivityType(
            Cell cell,
            int rowNumber) {
        String value =
                getCellString(cell);

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
    /** Kiểm tra dòng Excel rỗng trên cả 13 cột. */
    private boolean isEmptyRow(
            Row row) {
        if (row == null) {
            return true;
        }
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
    /** Chuyển chỉ số cột thành tên cột Excel. */
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