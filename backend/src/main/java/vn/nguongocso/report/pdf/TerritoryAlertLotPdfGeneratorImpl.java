package vn.nguongocso.report.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.report.dto.response.AlertBadgeSummary;
import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;
import vn.nguongocso.report.enums.LotAlertType;

/**
 * Triển khai sinh file PDF báo cáo danh sách lô có cảnh báo theo địa bàn (NCL-07-CN-006).
 */
@Slf4j
@Component
public class TerritoryAlertLotPdfGeneratorImpl implements TerritoryAlertLotPdfGenerator {

    private static final String EXPORT_ERROR = "Không thể xuất báo cáo PDF danh sách lô cảnh báo.";
    private static final String REGULAR_FONT = "fonts/Roboto-Regular.ttf";
    private static final String BOLD_FONT = "fonts/Roboto-Bold.ttf";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color HEADER_BG_COLOR = new Color(46, 125, 50); // Màu xanh lá chủ đạo (#2E7D32)
    private static final Color ALT_ROW_BG_COLOR = new Color(248, 250, 252); // Màu nền dòng chẵn (#F8FAFC)
    private static final Color BORDER_COLOR = new Color(226, 232, 240); // Màu viền nhẹ (#E2E8F0)

    @Override
    public byte[] generate(List<AlertLotSummaryResponse> alertLots, String officerName, LocalDate fromDate, LocalDate toDate) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Khổ giấy A4 xoay ngang để hiển thị đầy đủ thông tin các cột
            Document document = new Document(PageSize.A4.rotate(), 24F, 24F, 24F, 24F);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            document.addTitle("Danh sách lô nông sản có cảnh báo theo địa bàn");
            document.addAuthor("Hệ thống Nguồn Gốc Số");
            document.addCreator("Nguồn Gốc Số");
            document.addSubject("Báo cáo theo dõi lô có cảnh báo");

            Font titleFont = loadFont(BOLD_FONT, 16F, Font.BOLD, new Color(33, 37, 41));
            Font subTitleFont = loadFont(REGULAR_FONT, 9.5F, Font.NORMAL, new Color(100, 116, 139));
            Font headerFont = loadFont(BOLD_FONT, 9.5F, Font.BOLD, Color.WHITE);
            Font dataFont = loadFont(REGULAR_FONT, 9F, Font.NORMAL, new Color(30, 41, 59));
            Font footerFont = loadFont(REGULAR_FONT, 8.5F, Font.ITALIC, new Color(148, 163, 184));

            // 1. Tiêu đề và thông tin chung
            addHeaderInformation(document, alertLots.size(), officerName, fromDate, toDate, titleFont, subTitleFont);

            // 2. Bảng dữ liệu lô có cảnh báo
            document.add(buildAlertLotsTable(alertLots, headerFont, dataFont));

            // 3. Chân trang ghi chú
            Paragraph footer = new Paragraph("Báo cáo được trích xuất từ Hệ thống Quản lý và Truy xuất Nguồn gốc Nông sản Nguồn Gốc Số", footerFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(12F);
            document.add(footer);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception ex) {
            log.error("Lỗi khi tạo file PDF danh sách lô có cảnh báo", ex);
            throw new BusinessException(EXPORT_ERROR);
        }
    }

    /**
     * Thêm tiêu đề và siêu dữ liệu báo cáo vào đầu trang.
     */
    private void addHeaderInformation(
            Document document,
            int totalLots,
            String officerName,
            LocalDate fromDate,
            LocalDate toDate,
            Font titleFont,
            Font subTitleFont
    ) throws DocumentException {
        Paragraph title = new Paragraph("DANH SÁCH LÔ SẢN XUẤT CÓ CẢNH BÁO THEO ĐỊA BÀN", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6F);
        document.add(title);

        String dateRangeText = (fromDate != null && toDate != null)
                ? fromDate.format(DATE_FORMAT) + " - " + toDate.format(DATE_FORMAT)
                : "Toàn bộ thời gian";
        String officerText = (officerName != null && !officerName.isBlank()) ? officerName : "Cán bộ quản lý ngành";
        String exportTime = LocalDateTime.now().format(DATETIME_FORMAT);

        Paragraph meta = new Paragraph(
                "Thời gian lọc: " + dateRangeText +
                "  |  Cán bộ phụ trách: " + officerText +
                "  |  Thời điểm xuất: " + exportTime +
                "  |  Tổng số lô: " + totalLots,
                subTitleFont
        );
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(14F);
        document.add(meta);
    }

    /**
     * Khởi tạo và điền dữ liệu bảng danh sách lô có cảnh báo.
     */
    private PdfPTable buildAlertLotsTable(
            List<AlertLotSummaryResponse> alertLots,
            Font headerFont,
            Font dataFont
    ) throws DocumentException {
        // Bảng gồm 8 cột: STT, Lô sản xuất, Vùng trồng, Loại nông sản, Tổ chức HTX, Loại cảnh báo, Thời điểm cảnh báo, Trạng thái
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {
                4F,   // STT
                18F,  // Lô sản xuất
                13F,  // Vùng trồng
                13F,  // Loại nông sản
                18F,  // Tổ chức HTX
                16F,  // Loại cảnh báo
                10F,  // Thời điểm cảnh báo
                8F    // Trạng thái
        });
        table.setSpacingBefore(4F);
        table.setSpacingAfter(8F);

        // Header
        addTableHeaderCell(table, "STT", headerFont, Element.ALIGN_CENTER);
        addTableHeaderCell(table, "Lô sản xuất", headerFont, Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Vùng trồng", headerFont, Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Loại nông sản", headerFont, Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Tổ chức / HTX sở hữu", headerFont, Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Loại cảnh báo", headerFont, Element.ALIGN_LEFT);
        addTableHeaderCell(table, "Thời gian", headerFont, Element.ALIGN_CENTER);
        addTableHeaderCell(table, "Trạng thái", headerFont, Element.ALIGN_CENTER);
        table.setHeaderRows(1);

        if (alertLots == null || alertLots.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("Không có lô cảnh báo nào phù hợp với bộ lọc tìm kiếm.", dataFont));
            emptyCell.setColspan(8);
            emptyCell.setPadding(14F);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setBorderColor(BORDER_COLOR);
            table.addCell(emptyCell);
            return table;
        }

        int stt = 1;
        for (AlertLotSummaryResponse lot : alertLots) {
            Color rowBg = (stt % 2 == 0) ? ALT_ROW_BG_COLOR : Color.WHITE;

            // STT
            addDataCell(table, String.valueOf(stt++), dataFont, Element.ALIGN_CENTER, rowBg);

            // Lô sản xuất
            String lotDisplay = lot.getLotName() != null ? lot.getLotName() : (lot.getLotCode() != null ? lot.getLotCode() : "—");
            addDataCell(table, lotDisplay, dataFont, Element.ALIGN_LEFT, rowBg);

            // Vùng trồng
            String farmArea = lot.getFarmAreaName() != null ? lot.getFarmAreaName() : "—";
            addDataCell(table, farmArea, dataFont, Element.ALIGN_LEFT, rowBg);

            // Loại nông sản
            String productCategory = lot.getProductCategoryName() != null ? lot.getProductCategoryName() : "—";
            addDataCell(table, productCategory, dataFont, Element.ALIGN_LEFT, rowBg);

            // Tổ chức / HTX sở hữu
            String organization = lot.getOrganizationName() != null ? lot.getOrganizationName() : "—";
            addDataCell(table, organization, dataFont, Element.ALIGN_LEFT, rowBg);

            // Loại cảnh báo (Việt hóa)
            String alertTypeText = formatAlertTypes(lot);
            addDataCell(table, alertTypeText, dataFont, Element.ALIGN_LEFT, rowBg);

            // Thời gian cảnh báo
            String triggeredAt = lot.getLatestAlertTriggeredAt() != null
                    ? lot.getLatestAlertTriggeredAt().format(DATETIME_FORMAT)
                    : "—";
            addDataCell(table, triggeredAt, dataFont, Element.ALIGN_CENTER, rowBg);

            // Trạng thái lô
            String status = formatLotStatus(lot.getLotStatus());
            addDataCell(table, status, dataFont, Element.ALIGN_CENTER, rowBg);
        }

        return table;
    }

    /**
     * Tạo ô tiêu đề bảng với phong cách hiện đại.
     */
    private void addTableHeaderCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(8F);
        cell.setPaddingBottom(8F);
        cell.setPaddingLeft(6F);
        cell.setPaddingRight(6F);
        cell.setBackgroundColor(HEADER_BG_COLOR);
        cell.setBorderColor(HEADER_BG_COLOR);
        table.addCell(cell);
    }

    /**
     * Tạo ô dữ liệu trong bảng với màu nền và đường kẻ viền nhẹ.
     */
    private void addDataCell(PdfPTable table, String text, Font font, int alignment, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(6F);
        cell.setPaddingBottom(6F);
        cell.setPaddingLeft(6F);
        cell.setPaddingRight(6F);
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(0.5F);
        table.addCell(cell);
    }

    /**
     * Chuyển đổi danh sách loại cảnh báo sang chuỗi tiếng Việt dễ hiểu.
     */
    private String formatAlertTypes(AlertLotSummaryResponse lot) {
        if (lot.getAlertSummaries() != null && !lot.getAlertSummaries().isEmpty()) {
            return lot.getAlertSummaries().stream()
                    .map(AlertBadgeSummary::getAlertName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.joining(", "));
        }
        if (lot.getPrimaryAlertType() != null) {
            return translateLotAlertType(lot.getPrimaryAlertType());
        }
        return "—";
    }

    /**
     * Dịch mã loại cảnh báo sang tiếng Việt.
     */
    private String translateLotAlertType(LotAlertType alertType) {
        if (alertType == null) {
            return "—";
        }
        switch (alertType) {
            case RECALLING:
                return "Đang thu hồi";
            case LOCKED_LABEL:
                return "Tem bị khóa";
            case INSPECTION_FAILED:
                return "Kiểm nghiệm không đạt";
            case QUARANTINE_OVERWRITTEN:
                return "Ghi đè cách ly PHI";
            case SERIOUS_FEEDBACK_OPEN:
                return "Phản ánh nghiêm trọng";
            case INSPECTION_EXPIRED:
                return "Kiểm nghiệm hết hạn";
            default:
                return alertType.name();
        }
    }

    /**
     * Định dạng trạng thái lô sản xuất sang tiếng Việt.
     */
    private String formatLotStatus(String status) {
        if (status == null || status.isBlank()) {
            return "—";
        }
        switch (status.toUpperCase()) {
            case "ACTIVE":
                return "Đang sản xuất";
            case "PLANTED":
                return "Đang gieo trồng";
            case "GROWING":
                return "Đang phát triển";
            case "HARVESTING":
                return "Đang thu hoạch";
            case "HARVESTED":
                return "Đã thu hoạch";
            case "PROCESSING":
                return "Đang sơ chế";
            case "PACKAGED":
                return "Đã đóng gói";
            case "DISTRIBUTING":
                return "Đang phân phối";
            case "COMPLETED":
                return "Hoàn thành";
            case "CANCELLED":
                return "Đã hủy";
            case "CLOSED":
                return "Đã đóng";
            case "RECALLED":
                return "Đã thu hồi";
            default:
                return status;
        }
    }

    /**
     * Tải phông chữ Roboto Unicode hỗ trợ đầy đủ tiếng Việt có dấu.
     */
    private Font loadFont(String resource, float size, int style, Color color) {
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

            BaseFont baseFont = BaseFont.createFont(
                    resource,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    false,
                    fontBytes,
                    null
            );

            return new Font(baseFont, size, style, color);

        } catch (Exception ex) {
            log.warn("Không thể nạp font tùy biến {}: {}, sử dụng font dự phòng Helvetica", resource, ex.getMessage());
            return new Font(Font.HELVETICA, size, style, color);
        }
    }
}
