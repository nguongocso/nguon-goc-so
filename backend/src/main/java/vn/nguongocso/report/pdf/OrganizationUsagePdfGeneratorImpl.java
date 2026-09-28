package vn.nguongocso.report.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
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
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse.MetricComparison;
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse.OrganizationUsageItem;

/** Triển khai sinh file PDF báo cáo mức độ sử dụng nền tảng theo tổ chức (NCL-07-CN-008). */
@Slf4j
@Component
public class OrganizationUsagePdfGeneratorImpl implements OrganizationUsagePdfGenerator {
    private static final String EXPORT_ERROR = "Không thể xuất báo cáo PDF mức độ sử dụng nền tảng.";
    private static final String REGULAR_FONT = "fonts/Roboto-Regular.ttf";
    private static final String BOLD_FONT = "fonts/Roboto-Bold.ttf";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color HEADER_BG_COLOR = new Color(46, 125, 50);
    private static final Color ALT_ROW_BG_COLOR = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    /** Tiêu đề 12 cột của bảng báo cáo. */
    private static final String[] TABLE_HEADERS = {
            "STT", "Mã tổ chức", "Tên tổ chức", "Loại", "Trạng thái",
            "Lô sản xuất", "Nhật ký", "Sự kiện chuỗi", "Tem kích hoạt",
            "Tra cứu công khai", "Người dùng HT", "Hoạt động gần nhất"
    };

    /** Độ rộng tương đối của từng cột. */
    private static final float[] TABLE_WIDTHS = {
            0.6F, 1.3F, 2.6F, 1.4F, 1.4F,
            1.6F, 1.4F, 1.6F, 1.6F,
            1.8F, 1.4F, 1.9F
    };

    @Override
    public byte[] generate(OrganizationUsageDashboardResponse dashboard) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24F, 24F, 24F, 24F);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            document.addTitle("Báo cáo mức độ sử dụng nền tảng theo tổ chức");
            document.addAuthor("Hệ thống Nguồn Gốc Số");
            document.addCreator("Nguồn Gốc Số");
            document.addSubject("Báo cáo mức độ sử dụng nền tảng");

            Font titleFont = loadFont(BOLD_FONT, 16F, Font.BOLD, new Color(33, 37, 41));
            Font subTitleFont = loadFont(REGULAR_FONT, 9.5F, Font.NORMAL, new Color(100, 116, 139));
            Font headerFont = loadFont(BOLD_FONT, 9F, Font.BOLD, Color.WHITE);
            Font dataFont = loadFont(REGULAR_FONT, 8.5F, Font.NORMAL, new Color(30, 41, 59));

            addHeaderInformation(document, dashboard, titleFont, subTitleFont);
            document.add(buildUsageTable(dashboard, headerFont, dataFont));

            document.close();
            return outputStream.toByteArray();

        } catch (Exception ex) {
            log.error("Không thể sinh PDF báo cáo mức độ sử dụng nền tảng", ex);
            throw new BusinessException(EXPORT_ERROR);
        }
    }

    /** Thêm phần tiêu đề và thông tin kỳ báo cáo. */
    private void addHeaderInformation(
            Document document,
            OrganizationUsageDashboardResponse dashboard,
            Font titleFont,
            Font subTitleFont
    ) {
        Paragraph title = new Paragraph("BÁO CÁO MỨC ĐỘ SỬ DỤNG NỀN TẢNG", titleFont);
        title.setSpacingAfter(4F);
        document.add(title);

        Paragraph subtitle = new Paragraph(
                "Kỳ hiện tại: " + dashboard.getStartDate().format(DATE_FORMAT)
                        + " - " + dashboard.getEndDate().format(DATE_FORMAT)
                        + "   |   Kỳ trước: " + dashboard.getPreviousStartDate().format(DATE_FORMAT)
                        + " - " + dashboard.getPreviousEndDate().format(DATE_FORMAT),
                subTitleFont
        );
        subtitle.setSpacingAfter(2F);
        document.add(subtitle);

        Paragraph meta = new Paragraph(
                "Tổng số tổ chức: " + dashboard.getTotalOrganizations()
                        + "   |   Thời điểm xuất: " + LocalDateTime.now().format(DATETIME_FORMAT),
                subTitleFont
        );
        meta.setSpacingAfter(10F);
        document.add(meta);
    }

    /** Xây dựng bảng dữ liệu mức độ sử dụng theo từng tổ chức. */
    private PdfPTable buildUsageTable(OrganizationUsageDashboardResponse dashboard, Font headerFont, Font dataFont) {
        PdfPTable table = new PdfPTable(TABLE_WIDTHS);
        table.setWidthPercentage(100F);
        table.setSpacingAfter(12F);

        for (String header : TABLE_HEADERS) {
            addCell(table, header, headerFont, HEADER_BG_COLOR, Color.WHITE, Element.ALIGN_CENTER);
        }

        List<OrganizationUsageItem> items = dashboard.getItems() != null ? dashboard.getItems() : List.of();
        int stt = 1;
        for (OrganizationUsageItem item : items) {
            Color bgColor = stt % 2 == 0 ? ALT_ROW_BG_COLOR : Color.WHITE;
            addCell(table, String.valueOf(stt), dataFont, bgColor, null, Element.ALIGN_CENTER);
            addCell(table, item.getOrganizationCode(), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, item.getOrganizationName(), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, typeLabel(item.getOrganizationType()), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, usageStatusLabel(item), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, formatMetric(item.getProductionLots(), item.isHasData()), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, formatMetric(item.getFarmLogs(), item.isHasData()), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, formatMetric(item.getChainEvents(), item.isHasData()), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, formatMetric(item.getActivatedLabels(), item.isHasData()), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, formatMetric(item.getPublicLookups(), item.isHasData()), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, formatMetric(item.getActiveUsers(), item.isHasData()), dataFont, bgColor, null, Element.ALIGN_LEFT);
            addCell(table, formatLastActivity(item.getLastActivityAt()), dataFont, bgColor, null, Element.ALIGN_LEFT);
            stt++;
        }

        return table;
    }

    /** Thêm một ô dữ liệu vào bảng với nền, căn lề và viền thống nhất. */
    private void addCell(PdfPTable table, String text, Font font, Color bgColor, Color fontColor, int horizontalAlignment) {
        Font effectiveFont = fontColor != null
                ? new Font(font.getBaseFont(), font.getSize(), font.getStyle(), fontColor)
                : font;
        PdfPCell cell = new PdfPCell(
                new Phrase(text != null && !text.isBlank() ? text : "—", effectiveFont)
        );
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(horizontalAlignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5F);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    /** Định dạng một chỉ số kèm % thay đổi so với kỳ trước. */
    private String formatMetric(MetricComparison metric, boolean hasData) {
        if (!hasData || metric == null) {
            return "—";
        }
        double percent = metric.getChangePercent() != null
                ? metric.getChangePercent()
                : (metric.getChange() > 0 ? 100.0 : 0.0);
        return String.format("%d (%+.1f%%)", metric.getCurrent(), percent);
    }

    /** Định dạng thời điểm hoạt động gần nhất. */
    private String formatLastActivity(LocalDateTime lastActivityAt) {
        return lastActivityAt != null ? lastActivityAt.format(DATETIME_FORMAT) : "—";
    }

    /** Dịch loại tổ chức sang nhãn tiếng Việt. */
    private String typeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "—";
        }
        switch (type) {
            case "COOPERATIVE":
                return "Hợp tác xã";
            case "ENTERPRISE":
                return "Doanh nghiệp";
            case "GOVERNMENT":
                return "Cơ quan quản lý";
            case "SYSTEM":
                return "Tổ chức hệ thống";
            default:
                return type;
        }
    }

    /** Nhãn trạng thái sử dụng thống nhất với giao diện. */
    private String usageStatusLabel(OrganizationUsageItem item) {
        if (item == null) {
            return "—";
        }
        if (item.isNeedsSupport() && item.getLastActivityAt() != null) {
            return "Cần liên hệ hỗ trợ";
        }
        if (item.isHasData()) {
            return "Đang hoạt động";
        }
        return "Chưa có dữ liệu";
    }

    /** Tải phông chữ Roboto Unicode hỗ trợ đầy đủ tiếng Việt có dấu. */
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
