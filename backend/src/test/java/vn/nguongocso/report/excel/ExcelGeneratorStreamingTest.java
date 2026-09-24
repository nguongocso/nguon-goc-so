package vn.nguongocso.report.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.nguongocso.report.enums.LotAlertType;
import vn.nguongocso.report.dto.response.AlertBadgeSummary;
import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;
import vn.nguongocso.report.dto.response.IndustryReportResponse;
import vn.nguongocso.report.dto.response.ProductBreakdownItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelGeneratorStreamingTest {

    private final IndustryReportExcelGeneratorImpl industryReportExcelGenerator = new IndustryReportExcelGeneratorImpl();
    private final TerritoryAlertLotExcelGeneratorImpl territoryAlertLotExcelGenerator = new TerritoryAlertLotExcelGeneratorImpl();

    @Test
    @DisplayName("IndustryReport: SXSSFWorkbook sinh file Excel hợp lệ, đọc lại không bị lỗi")
    void testIndustryReportExcelGeneration() throws IOException {
        IndustryReportResponse report = IndustryReportResponse.builder()
                .region("Thái Nguyên")
                .fromDate(LocalDate.of(2026, 1, 1))
                .toDate(LocalDate.of(2026, 9, 24))
                .totalOrganizations(5)
                .totalShipments(20)
                .totalQuantity(5000.0)
                .productBreakdown(List.of(
                        ProductBreakdownItem.builder()
                                .productCategoryName("Chè Tân Cương")
                                .shipmentCount(12L)
                                .totalQuantity(3000L)
                                .build(),
                        ProductBreakdownItem.builder()
                                .productCategoryName("Chè Bát Tiên")
                                .shipmentCount(8L)
                                .totalQuantity(2000L)
                                .build()
                ))
                .build();

        byte[] bytes = industryReportExcelGenerator.generate(report);
        assertThat(bytes).isNotNull().isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Báo cáo tổng hợp ngành");
            assertThat(sheet).isNotNull();

            Row titleRow = sheet.getRow(0);
            assertThat(titleRow.getCell(0).getStringCellValue()).isEqualTo("BÁO CÁO TỔNG HỢP NGÀNH");

            Row item1Row = sheet.getRow(9);
            assertThat(item1Row.getCell(0).getStringCellValue()).isEqualTo("Chè Tân Cương");
            assertThat(item1Row.getCell(1).getNumericCellValue()).isEqualTo(12.0);
            assertThat(item1Row.getCell(2).getNumericCellValue()).isEqualTo(3000.0);
        }
    }

    @Test
    @DisplayName("TerritoryAlertLot: SXSSFWorkbook sinh file danh sách cảnh báo hợp lệ, đọc lại không bị lỗi")
    void testTerritoryAlertLotExcelGeneration() throws IOException {
        AlertLotSummaryResponse lot = AlertLotSummaryResponse.builder()
                .lotId(UUID.randomUUID())
                .lotCode("LOT-2026-001")
                .lotName("Lô Chè Tân Cương Thượng Hạng")
                .organizationName("HTX Chè Thái Nguyên")
                .productCategoryName("Chè Xanh")
                .farmAreaName("Vùng Trồng A")
                .communeName("Tân Cương")
                .provinceName("Thái Nguyên")
                .lotStatus("PACKAGED")
                .primaryAlertType(LotAlertType.LOCKED_LABEL)
                .latestAlertTriggeredAt(LocalDateTime.of(2026, 9, 24, 8, 30))
                .alertSummaries(List.of(
                        AlertBadgeSummary.builder()
                                .alertType(LotAlertType.LOCKED_LABEL)
                                .alertName("Tem bị khóa")
                                .severity("HIGH")
                                .briefNote("Nghi vấn trùng tem")
                                .triggeredAt(LocalDateTime.of(2026, 9, 24, 8, 30))
                                .build()
                ))
                .build();

        byte[] bytes = territoryAlertLotExcelGenerator.generate(
                List.of(lot),
                "Nguyễn Văn A",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 24)
        );

        assertThat(bytes).isNotNull().isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Lô có cảnh báo");
            assertThat(sheet).isNotNull();

            Row headerRow = sheet.getRow(4);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("STT");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Mã lô");

            Row dataRow = sheet.getRow(5);
            assertThat(dataRow.getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("LOT-2026-001");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("Lô Chè Tân Cương Thượng Hạng");
            assertThat(dataRow.getCell(9).getStringCellValue()).contains("Tem bị khóa");
        }
    }
}
