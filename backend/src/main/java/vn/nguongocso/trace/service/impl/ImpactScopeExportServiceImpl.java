package vn.nguongocso.trace.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.dto.response.*;
import vn.nguongocso.trace.service.ImpactScopeExportService;
import vn.nguongocso.trace.service.ImpactScopeTraceService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ImpactScopeExportServiceImpl implements ImpactScopeExportService {

    private final ImpactScopeTraceService impactScopeTraceService;

    @Override
    public byte[] exportImpactScopeReport(String code, String format, CustomUserDetails currentUser) {
        ImpactScopeTraceResponse traceData = impactScopeTraceService.getImpactScopeTrace(code, currentUser);

        // Xuất Excel mặc định hoặc khi format là EXCEL
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Truy vết phạm vi ảnh hưởng");

            // Cell Styles
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int rowIdx = 0;

            // Title
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO TRUY VẾT PHẠM VI ẢNH HƯỞNG CỦA LÔ (NCL-08-CN-010)");
            titleCell.setCellStyle(titleStyle);

            rowIdx++; // blank line

            // Section 1: Thông tin Lô sản xuất & Vùng trồng
            Row sec1Header = sheet.createRow(rowIdx++);
            Cell sec1Cell = sec1Header.createCell(0);
            sec1Cell.setCellValue("1. THÔNG TIN LÔ SẢN XUẤT & VÙNG TRỒNG GỐC");
            sec1Cell.setCellStyle(headerStyle);

            ProductionLotTraceDto lot = traceData.getProductionLot();
            FarmAreaTraceDto farm = traceData.getFarmArea();

            createLabelValueRow(sheet, rowIdx++, "Mã / Tên Lô sản xuất:", lot != null ? lot.getName() : "N/A");
            createLabelValueRow(sheet, rowIdx++, "Trạng thái Lô sản xuất:", lot != null && lot.getStatus() != null ? lot.getStatus().name() : "N/A");
            createLabelValueRow(sheet, rowIdx++, "Sản lượng dự kiến:", lot != null ? (lot.getExpectedQuantity() + " " + (lot.getExpectedQuantityUnit() != null ? lot.getExpectedQuantityUnit() : "")) : "N/A");
            createLabelValueRow(sheet, rowIdx++, "Vùng trồng gốc:", farm != null ? (farm.getName() + " (" + farm.getCode() + ") - " + farm.getLocation()) : "Chưa gắn vùng trồng");

            rowIdx++; // blank line

            // Section 2: Danh sách Lô hàng bị ảnh hưởng
            Row sec2Header = sheet.createRow(rowIdx++);
            Cell sec2Cell = sec2Header.createCell(0);
            sec2Cell.setCellValue("2. DANH SÁCH LÔ HÀNG VÀ TỔ CHỨC NHẬN BỊ ẢNH HƯỞNG");
            sec2Cell.setCellStyle(headerStyle);

            // Table headers
            Row tblHeader = sheet.createRow(rowIdx++);
            String[] cols = {"STT", "Tên Lô hàng", "Trạng thái Lô", "Số lượng", "Tem kích hoạt", "Số lượt quét", "Tổ chức đã nhận"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = tblHeader.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            if (traceData.getShipments() == null || traceData.getShipments().isEmpty()) {
                Row emptyRow = sheet.createRow(rowIdx++);
                emptyRow.createCell(0).setCellValue("1");
                emptyRow.createCell(1).setCellValue("(Lô chưa sinh lô hàng nào - Nhánh rỗng)");
            } else {
                int stt = 1;
                for (ShipmentTraceDto ship : traceData.getShipments()) {
                    Row dataRow = sheet.createRow(rowIdx++);
                    dataRow.createCell(0).setCellValue(stt++);
                    dataRow.createCell(1).setCellValue(ship.getName());
                    dataRow.createCell(2).setCellValue(ship.getStatus() != null ? ship.getStatus().name() : "");
                    dataRow.createCell(3).setCellValue(ship.getTotalQuantity() != null ? ship.getTotalQuantity() : 0);
                    dataRow.createCell(4).setCellValue(ship.getActivatedStampsCount());
                    dataRow.createCell(5).setCellValue(ship.getScanStats() != null ? ship.getScanStats().getTotalScans() : 0);

                    StringBuilder orgsStr = new StringBuilder();
                    if (ship.getReceivingOrganizations() != null && !ship.getReceivingOrganizations().isEmpty()) {
                        for (ReceivingOrganizationTraceDto r : ship.getReceivingOrganizations()) {
                            if (orgsStr.length() > 0) orgsStr.append("; ");
                            orgsStr.append(r.getOrganizationName())
                                   .append(" (Nhận ngày: ")
                                   .append(r.getReceivedAt() != null ? r.getReceivedAt().toString() : "N/A")
                                   .append(")");
                        }
                    } else {
                        orgsStr.append("Chưa giao cho đối tác");
                    }
                    dataRow.createCell(6).setCellValue(orgsStr.toString());
                }
            }

            rowIdx++; // blank line

            // Section 3: Summary
            Row sec3Header = sheet.createRow(rowIdx++);
            Cell sec3Cell = sec3Header.createCell(0);
            sec3Cell.setCellValue("3. TỔNG HỢP PHẠM VI ẢNH HƯỞNG");
            sec3Cell.setCellStyle(headerStyle);

            ImpactScopeSummaryDto summary = traceData.getSummary();
            createLabelValueRow(sheet, rowIdx++, "Tổng số lô hàng sinh ra:", summary != null ? String.valueOf(summary.getTotalShipments()) : "0");
            createLabelValueRow(sheet, rowIdx++, "Tổng số tem đã kích hoạt:", summary != null ? String.valueOf(summary.getTotalActivatedStamps()) : "0");
            createLabelValueRow(sheet, rowIdx++, "Số tổ chức nhận bị ảnh hưởng:", summary != null ? String.valueOf(summary.getTotalReceivingOrganizations()) : "0");
            createLabelValueRow(sheet, rowIdx++, "Số lô hàng đã thu hồi:", summary != null ? String.valueOf(summary.getTotalRecalledShipments()) : "0");

            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("Lỗi khi xuất tệp báo cáo phạm vi ảnh hưởng.");
        }
    }

    private void createLabelValueRow(Sheet sheet, int rowIdx, String label, String value) {
        Row r = sheet.createRow(rowIdx);
        r.createCell(0).setCellValue(label);
        r.createCell(1).setCellValue(value);
    }
}
