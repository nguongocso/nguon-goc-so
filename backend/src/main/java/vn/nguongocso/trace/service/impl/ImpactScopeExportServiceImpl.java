package vn.nguongocso.trace.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.dto.response.*;
import vn.nguongocso.trace.service.ImpactScopeExportService;
import vn.nguongocso.trace.service.ImpactScopeTraceService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImpactScopeExportServiceImpl implements ImpactScopeExportService {

    private final ImpactScopeTraceService impactScopeTraceService;

    @Override
    public byte[] exportImpactScopeReport(String code, String format, CustomUserDetails currentUser) {
        ImpactScopeTraceResponse traceData = impactScopeTraceService.getImpactScopeTrace(code, currentUser);

        if ("PDF".equalsIgnoreCase(format)) {
            return generatePdfReport(traceData);
        }

        return generateExcelReport(traceData);
    }

    private byte[] generateExcelReport(ImpactScopeTraceResponse traceData) {
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
            createLabelValueRow(sheet, rowIdx++, "Trạng thái Lô sản xuất:", lot != null && lot.getStatus() != null ? formatStatus(lot.getStatus().name()) : "N/A");
            createLabelValueRow(sheet, rowIdx++, "Sản lượng dự kiến:", lot != null ? (lot.getExpectedQuantity() + " " + (lot.getExpectedQuantityUnit() != null ? lot.getExpectedQuantityUnit() : "")) : "N/A");
            createLabelValueRow(sheet, rowIdx++, "Vùng trồng gốc:", farm != null ? (farm.getName() + " (" + farm.getCode() + ")" + (formatLocation(farm.getLocation()).isEmpty() ? "" : " - " + formatLocation(farm.getLocation()))) : "Chưa gắn vùng trồng");

            rowIdx++; // blank line

            // Section 2: Danh sách Lô hàng, Sự kiện vận chuyển/thu mua và Tổ chức nhận
            Row sec2Header = sheet.createRow(rowIdx++);
            Cell sec2Cell = sec2Header.createCell(0);
            sec2Cell.setCellValue("2. DANH SÁCH LÔ HÀNG, SỰ KIỆN VẬN CHUYỂN / THU MUA VÀ TỔ CHỨC NHẬN");
            sec2Cell.setCellStyle(headerStyle);

            // Table headers
            Row tblHeader = sheet.createRow(rowIdx++);
            String[] cols = {"STT", "Tên Lô hàng", "Trạng thái Lô", "Số lượng", "Tem kích hoạt", "Lượt quét", "Sự kiện diễn ra", "Tổ chức đã nhận"};
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
                    dataRow.createCell(2).setCellValue(ship.getStatus() != null ? formatStatus(ship.getStatus().name()) : "");
                    dataRow.createCell(3).setCellValue(ship.getTotalQuantity() != null ? ship.getTotalQuantity() : 0);
                    dataRow.createCell(4).setCellValue(ship.getActivatedStampsCount());
                    dataRow.createCell(5).setCellValue(ship.getScanStats() != null ? ship.getScanStats().getTotalScans() : 0);

                    // Sự kiện diễn ra (Vận chuyển, Thu mua, Nhập kho...)
                    StringBuilder eventsStr = new StringBuilder();
                    if (ship.getEvents() != null && !ship.getEvents().isEmpty()) {
                        for (ChainEventTraceDto ev : ship.getEvents()) {
                            if (eventsStr.length() > 0) eventsStr.append("; ");
                            String eventTypeName = ev.getEventTypeName() != null && !ev.getEventTypeName().trim().isEmpty()
                                    ? formatEventType(ev.getEventTypeName())
                                    : (ev.getEventType() != null ? formatEventType(ev.getEventType().name()) : "");
                            eventsStr.append(eventTypeName)
                                     .append(" (")
                                     .append(ev.getRecordedAt() != null ? ev.getRecordedAt().toString() : "")
                                     .append(")");
                        }
                    } else {
                        eventsStr.append("Chưa phát sinh sự kiện");
                    }
                    dataRow.createCell(6).setCellValue(eventsStr.toString());

                    // Tổ chức đã nhận (Đáp ứng QTN-01 & TC-04)
                    StringBuilder orgsStr = new StringBuilder();
                    if (ship.getReceivingOrganizations() != null && !ship.getReceivingOrganizations().isEmpty()) {
                        for (ReceivingOrganizationTraceDto r : ship.getReceivingOrganizations()) {
                            if (orgsStr.length() > 0) orgsStr.append("; ");
                            orgsStr.append(r.getOrganizationName())
                                   .append(" (Nhận ngày: ")
                                   .append(r.getReceivedAt() != null ? r.getReceivedAt().toString() : "N/A");
                            if (r.getReceivedQuantity() != null) {
                                orgsStr.append(", Thực nhận: ").append(r.getReceivedQuantity());
                            }
                            orgsStr.append(")");
                        }
                    } else {
                        orgsStr.append("Chưa giao cho đối tác");
                    }
                    dataRow.createCell(7).setCellValue(orgsStr.toString());
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

            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("Lỗi khi xuất tệp báo cáo Excel.");
        }
    }

    private byte[] generatePdfReport(ImpactScopeTraceResponse traceData) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.addTitle("Báo cáo truy vết phạm vi ảnh hưởng");
            document.addAuthor("Hệ thống Nguồn Gốc Số");

            com.lowagie.text.Font titleFont = loadPdfFont("fonts/Roboto-Bold.ttf", 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = loadPdfFont("fonts/Roboto-Bold.ttf", 11, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = loadPdfFont("fonts/Roboto-Regular.ttf", 10, com.lowagie.text.Font.NORMAL);

            Paragraph title = new Paragraph("BÁO CÁO TRUY VẾT PHẠM VI ẢNH HƯỞNG CỦA LÔ", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Section 1: Lot & Farm
            document.add(new Paragraph("1. THÔNG TIN LÔ SẢN XUẤT & VÙNG TRỒNG GỐC", headerFont));
            ProductionLotTraceDto lot = traceData.getProductionLot();
            FarmAreaTraceDto farm = traceData.getFarmArea();
            document.add(new Paragraph("Mã / Tên Lô sản xuất: " + (lot != null ? lot.getName() : "N/A"), normalFont));
            document.add(new Paragraph("Trạng thái Lô sản xuất: " + (lot != null && lot.getStatus() != null ? formatStatus(lot.getStatus().name()) : "N/A"), normalFont));
            document.add(new Paragraph("Sản lượng dự kiến: " + (lot != null ? (lot.getExpectedQuantity() + " " + (lot.getExpectedQuantityUnit() != null ? lot.getExpectedQuantityUnit() : "")) : "N/A"), normalFont));
            document.add(new Paragraph("Vùng trồng gốc: " + (farm != null ? (farm.getName() + " (" + farm.getCode() + ")" + (formatLocation(farm.getLocation()).isEmpty() ? "" : " - " + formatLocation(farm.getLocation()))) : "Chưa gắn vùng trồng"), normalFont));
            document.add(new Paragraph(" "));

            // Section 2: Table of Shipments
            document.add(new Paragraph("2. DANH SÁCH LÔ HÀNG VÀ TỔ CHỨC NHẬN", headerFont));
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {1f, 3f, 2f, 2f, 2f, 3f});
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            addPdfHeaderCell(table, "STT", headerFont);
            addPdfHeaderCell(table, "Tên Lô hàng", headerFont);
            addPdfHeaderCell(table, "Trạng thái", headerFont);
            addPdfHeaderCell(table, "Số lượng", headerFont);
            addPdfHeaderCell(table, "Tem kích hoạt", headerFont);
            addPdfHeaderCell(table, "Tổ chức đã nhận", headerFont);

            if (traceData.getShipments() == null || traceData.getShipments().isEmpty()) {
                PdfPCell cell = new PdfPCell(new Phrase("Lô chưa phát sinh lô hàng nào", normalFont));
                cell.setColspan(6);
                table.addCell(cell);
            } else {
                int stt = 1;
                for (ShipmentTraceDto s : traceData.getShipments()) {
                    table.addCell(new Phrase(String.valueOf(stt++), normalFont));
                    table.addCell(new Phrase(s.getName(), normalFont));
                    table.addCell(new Phrase(s.getStatus() != null ? formatStatus(s.getStatus().name()) : "", normalFont));
                    table.addCell(new Phrase(String.valueOf(s.getTotalQuantity()), normalFont));
                    table.addCell(new Phrase(String.valueOf(s.getActivatedStampsCount()), normalFont));

                    StringBuilder orgs = new StringBuilder();
                    if (s.getReceivingOrganizations() != null && !s.getReceivingOrganizations().isEmpty()) {
                        for (ReceivingOrganizationTraceDto r : s.getReceivingOrganizations()) {
                            if (orgs.length() > 0) orgs.append("; ");
                            orgs.append(r.getOrganizationName());
                            if (r.getReceivedQuantity() != null) {
                                orgs.append(" (Thực nhận: ").append(r.getReceivedQuantity()).append(")");
                            }
                        }
                    } else {
                        orgs.append("Chưa giao cho đối tác");
                    }
                    table.addCell(new Phrase(orgs.toString(), normalFont));
                }
            }
            document.add(table);
            document.add(new Paragraph(" "));

            // Section 3: Summary
            document.add(new Paragraph("3. TỔNG HỢP PHẠM VI ẢNH HƯỞNG", headerFont));
            ImpactScopeSummaryDto summary = traceData.getSummary();
            document.add(new Paragraph("Tổng số lô hàng sinh ra: " + (summary != null ? summary.getTotalShipments() : 0), normalFont));
            document.add(new Paragraph("Tổng số tem đã kích hoạt: " + (summary != null ? summary.getTotalActivatedStamps() : 0), normalFont));
            document.add(new Paragraph("Số tổ chức nhận bị ảnh hưởng: " + (summary != null ? summary.getTotalReceivingOrganizations() : 0), normalFont));
            document.add(new Paragraph("Số lô hàng đã thu hồi: " + (summary != null ? summary.getTotalRecalledShipments() : 0), normalFont));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage(), e);
            throw new BusinessException("Lỗi khi tạo tệp báo cáo PDF: " + e.getMessage());
        }
    }

    private void addPdfHeaderCell(PdfPTable table, String title, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setGrayFill(0.9f);
        table.addCell(cell);
    }

    private com.lowagie.text.Font loadPdfFont(String resource, float size, int style) {
        String resourcePath = resource.startsWith("/") ? resource : "/" + resource;
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            byte[] fontBytes;
            if (inputStream != null) {
                fontBytes = inputStream.readAllBytes();
            } else {
                try (InputStream cpStream = new ClassPathResource(resource).getInputStream()) {
                    fontBytes = cpStream.readAllBytes();
                }
            }
            BaseFont baseFont = BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, fontBytes, null);
            return new com.lowagie.text.Font(baseFont, size, style);
        } catch (Exception ex) {
            log.warn("Load custom font failed: {}, falling back to standard font: {}", resource, ex.getMessage());
            return new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, size, style);
        }
    }

    private void createLabelValueRow(Sheet sheet, int rowIdx, String label, String value) {
        Row r = sheet.createRow(rowIdx);
        r.createCell(0).setCellValue(label);
        r.createCell(1).setCellValue(value);
    }

    private String formatStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "N/A";
        switch (status.toUpperCase()) {
            case "ACTIVATED": return "Đã kích hoạt";
            case "DRAFT": return "Dự thảo";
            case "RECALLED": return "Đã thu hồi";
            case "CODE_PRINTED": return "Đã in mã";
            case "APPROVED": return "Đã phê duyệt";
            case "PACKAGED": return "Đã đóng gói";
            case "CANCELLED": return "Đã hủy";
            case "PENDING": return "Chờ xử lý";
            case "COMPLETED": return "Đã hoàn thành";
            default: return status;
        }
    }

    private String formatLocation(String location) {
        if (location == null || location.trim().isEmpty()) return "";
        String loc = location.trim();
        if (loc.toUpperCase().startsWith("POINT (") || loc.toUpperCase().startsWith("POINT(")) {
            String coords = loc.replaceAll("(?i)POINT\\s*\\(", "").replace(")", "").trim();
            String[] parts = coords.split("\\s+");
            if (parts.length >= 2) {
                return "Tọa độ: " + parts[0] + ", " + parts[1];
            }
        }
        return loc;
    }

    private String formatEventType(String type) {
        if (type == null || type.trim().isEmpty()) return "";
        switch (type.toUpperCase()) {
            case "TRANSPORT": return "Vận chuyển";
            case "PROCUREMENT": return "Thu mua";
            case "WAREHOUSE_RECEIPT": return "Nhập kho";
            case "PACKAGING": return "Đóng gói";
            case "PREPROCESSING": return "Sơ chế";
            case "HARVEST": return "Thu hoạch";
            case "STORAGE_CONDITION": return "Bảo quản";
            case "CORRECTION": return "Đính chính";
            case "WAREHOUSE_ENTRY": return "Nhập kho HTX";
            case "WAREHOUSE_EXIT": return "Xuất kho HTX";
            default: return type;
        }
    }
}
