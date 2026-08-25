package vn.nguongocso.trace.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.trace.dto.request.ExportLabelsRequest;
import vn.nguongocso.trace.dto.response.LabelExportResponse;
import vn.nguongocso.trace.entity.LabelExportHistory;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.LabelExportHistoryRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.LabelExportService;

/**
 * Service xuất tem QR cho lô hàng (NCL-04-CN-005).
 *
 * <p>
 * Sinh file PDF nhiều trang (khổ giấy A4) chứa lưới tem QR theo khổ tem đã
 * chọn. Mỗi tem gồm ảnh QR (mã hóa URL tra cứu công khai), mã truy xuất và các
 * trường tùy chọn. Mọi lượt xuất đều được ghi vào {@code label_export_history}
 * theo QTN-23.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelExportServiceImpl implements LabelExportService {

    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final LabelExportHistoryRepository labelExportHistoryRepository;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Vai trò được phép xuất tem. */
    private static final String ORG_MANAGER_ROLE = "VT-02";

    /** Các khổ tem hỗ trợ (mm). */
    private static final Set<String> SUPPORTED_LABEL_SIZES = Set.of("40x30", "50x40", "70x50");

    private static final float PT_PER_MM = 72f / 25.4f;
    private static final float PAGE_MARGIN_MM = 5f;
    private static final float LABEL_GAP_MM = 2f;
    private static final float LABEL_PADDING_MM = 1.5f;
    private static final int QR_PIXEL_SIZE = 300;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LabelExportResponse exportLabels(UUID shipmentId, ExportLabelsRequest request) {
        CustomUserDetails currentUser = getCurrentUser();

        // 1. Lô hàng phải tồn tại
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng."));

        // 2. Chỉ VT-02 được xuất tem
        if (!ORG_MANAGER_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý hợp tác xã (VT-02) mới được xuất tem.");
        }

        // 3. Cô lập dữ liệu theo tổ chức (QTN-01)
        if (!currentUser.getOrganizationId().equals(shipment.getOrganization().getOrganizationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xuất tem lô hàng của tổ chức khác.");
        }

        // 4. Lô hàng đã thu hồi thì không được xuất tem
        if (shipment.getStatus() == ShipmentStatus.RECALLED) {
            throw new BusinessException("Lô hàng đã bị thu hồi, không thể xuất tem.");
        }

        // 5. Khổ tem phải nằm trong danh sách đã cấu hình
        String labelSize = request.getLabelSize() == null ? "" : request.getLabelSize().trim().toLowerCase();
        if (!SUPPORTED_LABEL_SIZES.contains(labelSize)) {
            throw new BusinessException("Khổ tem không hợp lệ. Các khổ hỗ trợ: 40x30, 50x40, 70x50.");
        }

        // 6. Lô hàng phải đã sinh mã truy xuất
        List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipmentId);
        if (traceCodes.isEmpty()) {
            throw new BusinessException("Lô hàng chưa có mã truy xuất nào để xuất tem.");
        }
        traceCodes.sort(Comparator.comparing(TraceCode::getCodeValue));

        // 7. Tổng số tem xuất không vượt số mã đã sinh cho lô hàng (QTN-23)
        int totalCodes = traceCodes.size();
        int startIndex = request.getStartIndex();
        int count = request.getCount();
        if (startIndex >= totalCodes || startIndex + count > totalCodes) {
            throw new BusinessException(String.format(
                    "Số lượng tem xuất vượt quá số mã đã sinh cho lô hàng (yêu cầu %d mã từ vị trí %d, lô hàng chỉ có %d mã).",
                    count, startIndex, totalCodes));
        }

        List<TraceCode> selectedCodes = traceCodes.subList(startIndex, startIndex + count);

        // 8. Sinh PDF
        byte[] pdfBytes = generatePdf(shipment, selectedCodes, labelSize);

        // 9. Ghi lịch sử xuất (QTN-23)
        LabelExportHistory history = LabelExportHistory.builder()
                .shipment(shipment)
                .exportedBy(currentUser.getUser())
                .organization(shipment.getOrganization())
                .exportedAt(LocalDateTime.now())
                .startIndex(startIndex)
                .endIndex(startIndex + count - 1)
                .quantity(count)
                .labelSize(labelSize)
                .build();
        labelExportHistoryRepository.save(history);

        String fileName = String.format("Tem_QR_%s_%s.pdf",
                shipmentId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        log.info("Xuất {} tem QR cho lô hàng {} ({} -> {}) khổ {} bởi user {}",
                count, shipmentId, startIndex, startIndex + count - 1, labelSize, currentUser.getUserId());

        return LabelExportResponse.builder()
                .pdfBytes(pdfBytes)
                .fileName(fileName)
                .quantity(count)
                .labelSize(labelSize)
                .startIndex(startIndex)
                .endIndex(startIndex + count - 1)
                .build();
    }

    // ==================== Sinh PDF ====================

    /**
     * Sinh file PDF nhiều trang với lưới tem QR trên giấy A4.
     */
    private byte[] generatePdf(Shipment shipment, List<TraceCode> codes, String labelSize)
            throws com.lowagie.text.DocumentException {
        String[] dims = labelSize.split("x");
        float labelWidth = Float.parseFloat(dims[0]) * PT_PER_MM;
        float labelHeight = Float.parseFloat(dims[1]) * PT_PER_MM;

        float margin = PAGE_MARGIN_MM * PT_PER_MM;
        float gap = LABEL_GAP_MM * PT_PER_MM;
        float padding = LABEL_PADDING_MM * PT_PER_MM;

        float pageWidth = PageSize.A4.getWidth();
        float pageHeight = PageSize.A4.getHeight();

        int cols = Math.max(1, (int) ((pageWidth - 2 * margin + gap) / (labelWidth + gap)));
        int rows = Math.max(1, (int) ((pageHeight - 2 * margin + gap) / (labelHeight + gap)));
        int perPage = cols * rows;

        ExportLabelsRequest.IncludeFields fields = ExportLabelsRequest.IncludeFields.builder()
                .productName(true)
                .cooperativeName(true)
                .lotCode(true)
                .packagingDate(true)
                .build();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document =
                    new com.lowagie.text.Document(PageSize.A4, margin, margin, margin, margin);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte cb = writer.getDirectContent();

            float qrSide = Math.min(labelWidth, labelHeight) - 2 * padding;
            float fontSize = clamp(labelHeight * 0.10f, 5f, 7f);
            float lineHeight = fontSize * 1.4f;
            Font codeFont = new Font(Font.HELVETICA, fontSize, Font.BOLD);
            Font infoFont = new Font(Font.HELVETICA, fontSize, Font.NORMAL);
            float textXOffset = padding * 2 + qrSide;
            float textMaxWidth = labelWidth - textXOffset - padding;

            for (int i = 0; i < codes.size(); i++) {
                int pageIndex = i / perPage;
                if (i > 0 && i % perPage == 0) {
                    document.newPage();
                }

                int slot = i % perPage;
                int col = slot % cols;
                int row = slot / cols;
                float x = margin + col * (labelWidth + gap);
                float yTop = pageHeight - margin - row * (labelHeight + gap);
                float yBottom = yTop - labelHeight;

                // Đường viền cắt tem (màu nhạt)
                cb.setColorStroke(Color.GRAY);
                cb.setLineWidth(0.3f);
                cb.rectangle(x, yBottom, labelWidth, labelHeight);
                cb.stroke();

                drawLabel(cb, codes.get(i), shipment, fields,
                        x, yBottom, labelWidth, labelHeight,
                        qrSide, padding, textXOffset, textMaxWidth,
                        lineHeight, codeFont, infoFont);
            }

            document.close();
            log.debug("Đã sinh PDF {} tem, khổ {}", codes.size(), labelSize);
            return out.toByteArray();
        } catch (WriterException | java.io.IOException e) {
            log.error("Lỗi sinh PDF tem QR: {}", e.getMessage());
            throw new RuntimeException("Không thể sinh file PDF tem QR.", e);
        }
    }

    /**
     * Vẽ một tem: ảnh QR bên trái, thông tin chữ bên phải.
     */
    private void drawLabel(PdfContentByte cb, TraceCode traceCode, Shipment shipment,
            ExportLabelsRequest.IncludeFields fields,
            float x, float yBottom, float labelWidth, float labelHeight,
            float qrSide, float padding, float textXOffset, float textMaxWidth,
            float lineHeight, Font codeFont, Font infoFont) throws WriterException, java.io.IOException {
        // Ảnh QR mã hóa URL tra cứu công khai
        String qrUrl = buildTraceUrl(traceCode.getCodeValue());
        byte[] qrBytes = createQrPng(qrUrl, Math.max(QR_PIXEL_SIZE, (int) qrSide));
        Image qrImage = Image.getInstance(qrBytes);
        qrImage.scaleAbsolute(qrSide, qrSide);
        qrImage.setAbsolutePosition(x + padding, yBottom + labelHeight - padding - qrSide);
        cb.addImage(qrImage);

        // Các dòng chữ bên phải ảnh QR
        List<String[]> lines = new ArrayList<>();
        lines.add(new String[] { traceCode.getCodeValue(), "bold" });
        var productionLot = shipment.getProductionLot();
        if (fields.isProductName() && productionLot != null && productionLot.getProductCategory() != null) {
            lines.add(new String[] { ascii(productionLot.getProductCategory().getName()), "normal" });
        }
        if (fields.isCooperativeName() && shipment.getOrganization() != null) {
            lines.add(new String[] { ascii(shipment.getOrganization().getName()), "normal" });
        }
        if (fields.isLotCode() && productionLot != null) {
            lines.add(new String[] { "Lot: " + ascii(productionLot.getName()), "normal" });
        }
        if (fields.isPackagingDate() && shipment.getCreatedAt() != null) {
            lines.add(new String[] { "PKG: " + DATE_FORMATTER.format(shipment.getCreatedAt()), "normal" });
        }

        float yText = yBottom + labelHeight - padding - lineHeight;
        for (String[] line : lines) {
            if (yText < yBottom + padding) {
                break; // Hết chỗ trong tem
            }
            boolean bold = "bold".equals(line[1]);
            Font font = bold ? codeFont : infoFont;
            String text = truncate(line[0], font, textMaxWidth);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(text, font),
                    x + textXOffset, yText, 0);
            yText -= lineHeight;
        }
    }

    /**
     * Xây dựng URL tra cứu công khai từ FRONTEND_URL.
     */
    private String buildTraceUrl(String codeValue) {
        String base = frontendUrl == null ? "" : frontendUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isBlank()) {
            return codeValue;
        }
        return base + "/public/trace/" + codeValue;
    }

    /**
     * Sinh ảnh QR (PNG bytes) bằng ZXing.
     */
    private byte[] createQrPng(String content, int size) throws WriterException, java.io.IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Cắt bớt văn bản nếu vượt quá chiều rộng cho phép.
     */
    private String truncate(String text, Font font, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.getCalculatedBaseFont(true).getWidthPoint(text, font.getSize()) <= maxWidth) {
            return text;
        }
        String result = text;
        while (result.length() > 1
                && font.getCalculatedBaseFont(true).getWidthPoint(result + "...", font.getSize()) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    /**
     * Chuẩn hóa văn bản tiếng Việt về ASCII để hiển thị an toàn với font mặc định của PDF.
     */
    private String ascii(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.replaceAll("\\p{M}+", "");
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Lấy thông tin người dùng hiện tại từ SecurityContext.
     */
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "Chưa đăng nhập.");
    }
}
