package vn.nguongocso.report.pdf;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.report.dto.response.IndustryReportResponse;
import vn.nguongocso.report.dto.response.ProductBreakdownItem;

/**
 * Sinh file PDF cho báo cáo tổng hợp ngành.
 */
@Slf4j
@Component
public class IndustryReportPdfGeneratorImpl
                implements IndustryReportPdfGenerator {
        private static final String EXPORT_ERROR = "Không thể xuất báo cáo PDF.";

        private static final String FONT_ERROR = "Không thể tải font PDF.";

        private static final String REGULAR_FONT = "fonts/Roboto-Regular.ttf";

        private static final String BOLD_FONT = "fonts/Roboto-Bold.ttf";

        private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0");

        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        /**
         * Tạo báo cáo tổng hợp ngành dưới dạng PDF.
         *
         * @param report Dữ liệu báo cáo tổng hợp ngành
         * @return Mảng byte đại diện cho tệp PDF đã tạo
         */
        @Override
        public byte[] generate(IndustryReportResponse report) {

                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                        Document document = new Document();

                        PdfWriter.getInstance(document, outputStream);

                        document.open();

                        document.addTitle("Báo cáo tổng hợp ngành");
                        document.addAuthor("Hệ thống Nguồn Gốc Số");
                        document.addCreator("Nguồn Gốc Số");
                        document.addSubject("Báo cáo thống kê");

                        Font titleFont = loadFont(BOLD_FONT, 18, Font.BOLD);

                        Font headerFont = loadFont(BOLD_FONT, 12, Font.BOLD);

                        Font normalFont = loadFont(REGULAR_FONT, 11, Font.NORMAL);

                        addGeneralInformation(
                                        document,
                                        report,
                                        titleFont,
                                        normalFont);

                        document.add(
                                        buildProductTable(
                                                        report,
                                                        headerFont,
                                                        normalFont));

                        document.close();

                        return outputStream.toByteArray();

                } catch (Exception ex) {

                        log.error("Export PDF failed.", ex);

                        throw new BusinessException(EXPORT_ERROR);
                }
        }

        /**
         * Thông tin tổng quan báo cáo.
         */
        private void addGeneralInformation(
                        Document document,
                        IndustryReportResponse report,
                        Font titleFont,
                        Font normalFont)
                        throws DocumentException {

                Paragraph title = new Paragraph(
                                "BÁO CÁO TỔNG HỢP NGÀNH",
                                titleFont);

                title.setAlignment(Paragraph.ALIGN_CENTER);

                document.add(title);

                document.add(new Paragraph(" "));

                document.add(new Paragraph(
                                "Địa bàn: "
                                                + report.getRegion(),
                                normalFont));

                document.add(new Paragraph(
                                "Thời gian: "
                                                + report.getFromDate().format(DATE_FORMAT)
                                                + " - "
                                                + report.getToDate().format(DATE_FORMAT),
                                normalFont));

                document.add(new Paragraph(" "));

                document.add(new Paragraph(
                                "Tổng tổ chức: "
                                                + report.getTotalOrganizations(),
                                normalFont));

                document.add(new Paragraph(
                                "Tổng lô hàng: "
                                                + report.getTotalShipments(),
                                normalFont));

                document.add(new Paragraph(
                                "Tổng sản lượng: "
                                                + NUMBER_FORMAT.format(
                                                                report.getTotalQuantity())
                                                + " kg",
                                normalFont));

                document.add(new Paragraph(" "));
        }

        /**
         * Tạo bảng thống kê theo loại nông sản.
         */
        private PdfPTable buildProductTable(
                        IndustryReportResponse report,
                        Font headerFont,
                        Font normalFont)
                        throws DocumentException {

                PdfPTable table = new PdfPTable(3);

                table.setWidthPercentage(100);

                table.setWidths(new float[] {
                                4F,
                                2F,
                                3F
                });

                table.setSpacingBefore(10F);
                table.setSpacingAfter(10F);

                addHeader(
                                table,
                                "Loại nông sản",
                                headerFont);

                addHeader(
                                table,
                                "Số lô hàng",
                                headerFont);

                addHeader(
                                table,
                                "Tổng sản lượng (kg)",
                                headerFont);

                table.setHeaderRows(1);

                List<ProductBreakdownItem> items = report.getProductBreakdown();

                if (items == null || items.isEmpty()) {

                        PdfPCell cell = new PdfPCell(
                                        new Phrase(
                                                        "Không có dữ liệu",
                                                        normalFont));

                        cell.setColspan(3);
                        cell.setPadding(8F);
                        cell.setHorizontalAlignment(
                                        PdfPCell.ALIGN_CENTER);

                        table.addCell(cell);

                        return table;
                }

                for (ProductBreakdownItem item : items) {

                        table.addCell(
                                        new Phrase(
                                                        item.getProductCategoryName(),
                                                        normalFont));

                        table.addCell(
                                        new Phrase(
                                                        String.valueOf(
                                                                        item.getShipmentCount()),
                                                        normalFont));

                        table.addCell(
                                        new Phrase(
                                                        NUMBER_FORMAT.format(
                                                                        item.getTotalQuantity()),
                                                        normalFont));
                }

                return table;
        }

        /**
         * Thêm tiêu đề bảng.
         */
        private void addHeader(
                        PdfPTable table,
                        String title,
                        Font font) {

                PdfPCell cell = new PdfPCell(
                                new Phrase(
                                                title,
                                                font));

                cell.setHorizontalAlignment(
                                PdfPCell.ALIGN_CENTER);

                cell.setVerticalAlignment(
                                PdfPCell.ALIGN_MIDDLE);

                cell.setPadding(8F);

                cell.setGrayFill(0.9F);

                table.addCell(cell);
        }

        /**
         * Tải font Unicode từ resources.
         */
        private Font loadFont(
                        String resource,
                        float size,
                        int style) {

                try (InputStream inputStream = new ClassPathResource(resource)
                                .getInputStream()) {

                        BaseFont baseFont = BaseFont.createFont(
                                        resource,
                                        BaseFont.IDENTITY_H,
                                        BaseFont.EMBEDDED,
                                        false,
                                        inputStream.readAllBytes(),
                                        null);

                        return new Font(
                                        baseFont,
                                        size,
                                        style);

                } catch (Exception ex) {

                        log.error("Load font failed.", ex);

                        throw new BusinessException(FONT_ERROR);
                }
        }
}