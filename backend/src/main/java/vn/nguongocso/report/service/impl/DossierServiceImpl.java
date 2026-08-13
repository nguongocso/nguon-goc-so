package vn.nguongocso.report.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.report.exception.DossierValidationException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.dto.response.EventLocation;
import vn.nguongocso.report.dto.response.GS1DossierExportResponse;
import vn.nguongocso.report.dto.response.GS1Event;
import vn.nguongocso.report.dto.response.Warning;
import vn.nguongocso.report.entity.DossierExportHistory;
import vn.nguongocso.report.repository.DossierExportHistoryRepository;
import vn.nguongocso.report.service.DossierService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ hồ sơ.
 *
 * @author Triệu Văn Đại
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DossierServiceImpl implements DossierService {
    private final ShipmentRepository shipmentRepository;
    private final FarmLogRepository farmLogRepository;
    private final FarmLogAttachmentRepository farmLogAttachmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final DossierExportHistoryRepository exportHistoryRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private static final String GS1_SCHEMA_VERSION = "1.0.0";
    private static final String GS1_SCHEMA_DESCRIPTION =
            "Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ GS1";

    /**
     * Kiểm tra điều kiện xuất hồ sơ truy xuất cho một lô hàng.
     *
     * @param shipmentId  ID của lô hàng
     * @param currentUser Thông tin người dùng hiện tại
     * @return DossierCheckResponse chứa kết quả kiểm tra
     */
    @Override
    @Transactional(readOnly = true)
    public DossierCheckResponse checkEligibility(UUID shipmentId, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        validateDossierAccess(shipment, currentUser);

        List<String> missingDocs = new ArrayList<>();

        ProductionLot lot = shipment.getProductionLot();
        if (lot.getStatus() != ProductionLotStatus.CLOSED && lot.getStatus() != ProductionLotStatus.PACKAGED) {
            missingDocs.add("Lô sản xuất tương ứng chưa hoàn tất (Trạng thái yêu cầu: CLOSED hoặc PACKAGED)");
        }

        List<FarmLog> logs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId());

        boolean hasPlanting = false;
        boolean hasFertilizing = false;
        boolean hasPesticide = false;
        boolean hasHarvesting = false;

        for (FarmLog logItem : logs) {
            List<FarmLogAttachment> attachments = farmLogAttachmentRepository.findByFarmLogId(logItem.getId());
            if (attachments != null && !attachments.isEmpty()) {
                switch (logItem.getActivityType()) {
                    case PLANTING:
                        hasPlanting = true;
                        break;
                    case FERTILIZING:
                        hasFertilizing = true;
                        break;
                    case PESTICIDE:
                        hasPesticide = true;
                        break;
                    case HARVESTING:
                        hasHarvesting = true;
                        break;
                    default:
                        break;
                }
            }
        }

        if (!hasPlanting)
            missingDocs.add("Thiếu chứng từ gieo giống/xuống giống (PLANTING)");
        if (!hasFertilizing)
            missingDocs.add("Thiếu chứng từ bón phân (FERTILIZING)");
        if (!hasPesticide)
            missingDocs.add("Thiếu chứng từ phun thuốc/phòng trừ sâu bệnh (PESTICIDE)");
        if (!hasHarvesting)
            missingDocs.add("Thiếu chứng từ thu hoạch (HARVESTING)");

        if (!missingDocs.isEmpty()) {
            throw new DossierValidationException(
                    "Không đủ điều kiện xuất hồ sơ truy xuất: Lô hàng chưa hoàn tất hoặc thiếu chứng từ bắt buộc.",
                    missingDocs);
        }

        return DossierCheckResponse.builder()
                .shipmentId(shipmentId)
                .eligible(true)
                .missingDocuments(new ArrayList<>())
                .build();
    }

    /**
     * Xuất hồ sơ truy xuất cho một lô hàng dưới dạng PDF.
     *
     * @param shipmentId  ID của lô hàng
     * @param currentUser Thông tin người dùng hiện tại
     * @param ipAddress   Địa chỉ IP của người dùng
     * @return Mảng byte đại diện cho tệp PDF đã tạo
     */
    @Override
    @Transactional
    public byte[] exportDossierPdf(UUID shipmentId, CustomUserDetails currentUser, String ipAddress) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        // Kiểm tra quyền truy cập
        validateDossierAccess(shipment, currentUser);

        // Kiểm tra điều kiện xuất hồ sơ
        DossierCheckResponse checkResult = checkEligibility(shipmentId, currentUser);
        if (!checkResult.isEligible()) {
            // Ghi nhật ký thất bại
            logDossierExport(shipment, currentUser, "FAILED", ipAddress, 0L);
            throw new DossierValidationException("Không đủ điều kiện xuất hồ sơ truy xuất.",
                    checkResult.getMissingDocuments());
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Cấu hình Font hỗ trợ hiển thị Tiếng Việt
            Font titleFont;
            Font headerFont;
            Font boldFont;
            Font normalFont;

            String fontPath = "backend/src/main/resources/fonts/Roboto-Bold.ttf";
            if (new File(fontPath).exists()) {
                BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                titleFont = new Font(bf, 16, Font.BOLD, Color.BLACK);
                headerFont = new Font(bf, 12, Font.BOLD, Color.BLACK);
                boldFont = new Font(bf, 10, Font.BOLD, Color.BLACK);
                normalFont = new Font(bf, 10, Font.NORMAL, Color.BLACK);
            } else {
                // Fallback nếu không có hệ điều hành Windows hoặc không tìm thấy font Arial
                titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
                headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
                boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
                normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
            }

            // 1. Tiêu đề tài liệu
            Paragraph title = new Paragraph("HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);

            Paragraph subtitle = new Paragraph("Mã lô hàng: " + shipment.getId().toString(), normalFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(10);
            document.add(subtitle);

            document.add(new Paragraph(" "));

            // 2. Thông tin chung về Lô sản xuất
            document.add(new Paragraph("I. THÔNG TIN LÔ SẢN XUẤT", headerFont));
            document.add(new Paragraph(" "));
            PdfPTable lotTable = new PdfPTable(2);
            lotTable.setWidthPercentage(100);
            lotTable.setSpacingAfter(15);

            addTableCell(lotTable, "Tên lô sản xuất:", boldFont);
            addTableCell(lotTable, shipment.getProductionLot().getName(), normalFont);
            addTableCell(lotTable, "Danh mục sản phẩm:", boldFont);
            addTableCell(lotTable, shipment.getProductionLot().getProductCategory().getName(), normalFont);
            addTableCell(lotTable, "Đơn vị sản xuất (HTX):", boldFont);
            addTableCell(lotTable, shipment.getProductionLot().getOrganization().getName(), normalFont);
            addTableCell(lotTable, "Ngày xuống giống:", boldFont);
            addTableCell(lotTable,
                    shipment.getProductionLot().getPlantingDate() != null
                            ? shipment.getProductionLot().getPlantingDate().toString()
                            : "N/A",
                    normalFont);
            addTableCell(lotTable, "Ngày thu hoạch:", boldFont);
            addTableCell(lotTable,
                    shipment.getProductionLot().getHarvestDate() != null
                            ? shipment.getProductionLot().getHarvestDate().toString()
                            : "N/A",
                    normalFont);
            addTableCell(lotTable, "Sản lượng dự kiến:", boldFont);
            addTableCell(lotTable, shipment.getProductionLot().getExpectedQuantity() + " kg", normalFont);
            addTableCell(lotTable, "Sản lượng thực tế:", boldFont);
            addTableCell(lotTable,
                    shipment.getProductionLot().getActualQuantity() != null
                            ? shipment.getProductionLot().getActualQuantity() + " kg"
                            : "N/A",
                    normalFont);

            document.add(lotTable);

            // 3. Thông tin lô hàng vận chuyển
            document.add(new Paragraph("II. THÔNG TIN LÔ HÀNG", headerFont));
            document.add(new Paragraph(" "));
            PdfPTable shipmentTable = new PdfPTable(2);
            shipmentTable.setWidthPercentage(100);
            shipmentTable.setSpacingAfter(15);

            addTableCell(shipmentTable, "Tên lô hàng vận chuyển:", boldFont);
            addTableCell(shipmentTable, shipment.getName(), normalFont);
            addTableCell(shipmentTable, "Số lượng lô hàng:", boldFont);
            addTableCell(shipmentTable, shipment.getTotalQuantity() + " sản phẩm", normalFont);
            addTableCell(shipmentTable, "Thông tin đóng gói:", boldFont);
            addTableCell(shipmentTable, shipment.getPackagingInfo() != null ? shipment.getPackagingInfo() : "N/A",
                    normalFont);
            addTableCell(shipmentTable, "Trạng thái vận hành:", boldFont);
            addTableCell(shipmentTable, shipment.getStatus().name(), normalFont);

            document.add(shipmentTable);

            // 4. Nhật ký canh tác
            document.add(new Paragraph("III. LỊCH TRÌNH CANH TÁC & CHỨNG TỪ", headerFont));
            document.add(new Paragraph(" "));
            PdfPTable logTable = new PdfPTable(5);
            logTable.setWidthPercentage(100);
            logTable.setWidths(new float[] { 15f, 20f, 15f, 25f, 25f });
            logTable.setSpacingAfter(15);

            // Header cho bảng nhật ký
            addTableHeaderCell(logTable, "Ngày thực hiện", boldFont);
            addTableHeaderCell(logTable, "Hoạt động", boldFont);
            addTableHeaderCell(logTable, "Vật tư / Số lượng", boldFont);
            addTableHeaderCell(logTable, "Ghi chú", boldFont);
            addTableHeaderCell(logTable, "Chứng từ đính kèm", boldFont);

            List<FarmLog> logs = farmLogRepository
                    .findByProductionLotId_IdOrderByExecutedDateAsc(shipment.getProductionLot().getId());
            for (FarmLog logItem : logs) {
                addTableCell(logTable, logItem.getExecutedDate().toString(), normalFont);
                addTableCell(logTable, logItem.getActivityType().name(), normalFont);
                String materialInfo = (logItem.getMaterial() != null ? logItem.getMaterial() : "") +
                        (logItem.getQuantity() != null ? " (" + logItem.getQuantity() + " " + logItem.getUnit() + ")"
                                : "");
                addTableCell(logTable, materialInfo, normalFont);
                addTableCell(logTable, logItem.getNotes() != null ? logItem.getNotes() : "", normalFont);

                // Lấy chứng từ đính kèm
                List<FarmLogAttachment> attachments = farmLogAttachmentRepository.findByFarmLogId(logItem.getId());
                StringBuilder filesStr = new StringBuilder();
                if (attachments != null) {
                    for (FarmLogAttachment att : attachments) {
                        if (filesStr.length() > 0)
                            filesStr.append("\n");
                        filesStr.append(att.getFileName());
                    }
                }
                addTableCell(logTable, filesStr.toString().isEmpty() ? "Không có" : filesStr.toString(), normalFont);
            }
            document.add(logTable);

            // 5. Chuỗi sự kiện luân chuyển
            document.add(new Paragraph("IV. DÒNG SỰ KIỆN CHUỖI CUNG ỨNG (TIMELINE)", headerFont));
            document.add(new Paragraph(" "));
            PdfPTable eventTable = new PdfPTable(4);
            eventTable.setWidthPercentage(100);
            eventTable.setWidths(new float[] { 20f, 20f, 35f, 25f });
            eventTable.setSpacingAfter(15);

            addTableHeaderCell(eventTable, "Thời điểm ghi nhận", boldFont);
            addTableHeaderCell(eventTable, "Loại sự kiện", boldFont);
            addTableHeaderCell(eventTable, "Chi tiết dữ liệu", boldFont);
            addTableHeaderCell(eventTable, "Người ghi nhận", boldFont);

            List<ChainEvent> events = chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (ChainEvent ev : events) {
                addTableCell(eventTable, ev.getRecordedAt().format(formatter), normalFont);
                addTableCell(eventTable, ev.getEventType().name() + (ev.isCorrection() ? " (Đã điều chỉnh)" : ""),
                        normalFont);
                addTableCell(eventTable, ev.getEventData() != null ? ev.getEventData() : "", normalFont);
                addTableCell(eventTable, ev.getRecordedBy().getFullName(), normalFont);
            }
            document.add(eventTable);

            document.close();

            byte[] pdfData = out.toByteArray();
            long fileSize = pdfData.length;

            // Ghi nhận nhật ký thành công
            logDossierExport(shipment, currentUser, "SUCCESS", ipAddress, fileSize);

            publishActivityLog(
                    currentUser,
                    "EXPORT",
                    "Xuất hồ sơ truy xuất cho lô hàng " + shipment.getName(),
                    "Shipment",
                    shipment.getId().toString());

            return pdfData;
        } catch (Exception e) {
            log.error("Lỗi xuất file PDF cho shipmentId = {}: {}", shipmentId, e.getMessage());
            throw new BusinessException("Lỗi hệ thống khi sinh file PDF hồ sơ truy xuất.");
        }
    }

    /**
     * Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1 (dạng JSON).
     *
     * @param shipmentId     ID của lô hàng
     * @param currentUser    Người dùng hiện tại (VT-02 / VT-04)
     * @param includeMapping Có bao gồm bảng ánh xạ schema hay không
     * @return DTO hồ sơ GS1 mô phỏng
     */
    @Override
    @Transactional(readOnly = true)
    public GS1DossierExportResponse exportGs1Dossier(UUID shipmentId, CustomUserDetails currentUser,
            boolean includeMapping) {
        GS1DossierExportResponse result = buildGs1ExportDossier(shipmentId, currentUser, includeMapping, "json");
        return result;
    }

    private GS1DossierExportResponse buildGs1ExportDossier(UUID shipmentId, CustomUserDetails currentUser,
            boolean includeMapping, String format) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng."));

        // Kiểm tra quyền truy cập Shipment/Organization
        validateDossierAccess(shipment, currentUser);

        // Kiểm tra điều kiện QTN-11
        checkEligibility(shipmentId, currentUser);

        // Lấy danh sách sự kiện, sắp xếp recordedAt ASC (thứ tự ổn định theo id)
        List<ChainEvent> events = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId).stream()
                .sorted(Comparator.comparing(ChainEvent::getRecordedAt)
                        .thenComparing(e -> e.getId() != null ? e.getId().toString() : ""))
                .toList();

        // Shipment không có sự kiện nào -> 400
        if (events.isEmpty()) {
            throw new BusinessException("Lô chưa có sự kiện nào để xuất hồ sơ.");
        }

        return buildGs1Dossier(shipment, events, currentUser, includeMapping, format);
    }

    /**
     * Xuất hồ sơ theo lược đồ mô phỏng chuẩn GS1 (dạng XML).
     *
     * <p>Biểu diễn cùng tập dữ liệu và business semantics với JSON export.</p>
     *
     * @param shipmentId     ID của lô hàng
     * @param currentUser    Người dùng hiện tại (VT-02 / VT-04)
     * @param includeMapping Có bao gồm bảng ánh xạ schema hay không
     * @return Chuỗi XML
     */
    @Override
    @Transactional(readOnly = true)
    public String exportGs1DossierXml(UUID shipmentId, CustomUserDetails currentUser,
            boolean includeMapping) {
        GS1DossierExportResponse dossier = buildGs1ExportDossier(shipmentId, currentUser, includeMapping, "xml");
        return generateGs1Xml(dossier);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private GS1DossierExportResponse buildGs1Dossier(Shipment shipment, List<ChainEvent> events,
            CustomUserDetails currentUser, boolean includeMapping, String format) {

        List<GS1Event> gs1Events = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();

        for (ChainEvent event : events) {
            Map<String, Object> details = parseEventData(event.getEventData());

            EventLocation location = null;
            if (event.getLocation() != null) {
                location = EventLocation.builder()
                        .latitude(event.getLocation().getY())
                        .longitude(event.getLocation().getX())
                        // ChainEvent không có address -> để null
                        .address(null)
                        .build();
            } else {
                // Thiếu location: vẫn export, thêm warning
                warnings.add(Warning.builder()
                        .eventId(event.getId())
                        .field("location")
                        .message("Sự kiện thiếu thông tin vị trí")
                        .build());
            }

            gs1Events.add(GS1Event.builder()
                    .eventId(event.getId())
                    .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                    .eventTypeLabel(event.getEventType() != null ? eventTypeLabel(event.getEventType()) : null)
                    .recordedAt(event.getRecordedAt())
                    .recordedBy(event.getRecordedBy() != null ? event.getRecordedBy().getFullName() : null)
                    .location(location)
                    .details(details)
                    .build());
        }

        publishActivityLog(currentUser,
                "GS1_DOSSIER_EXPORT",
                "Xuat ho so GS1 dinh dang " + format + " cho lo hang " + shipment.getName(),
                "Shipment",
                shipment.getId().toString());

        return GS1DossierExportResponse.builder()
                .shipment(buildShipmentInfo(shipment))
                .events(gs1Events)
                .mapping(includeMapping ? buildGs1Mapping() : null)
                .warnings(warnings)
                .exportedAt(LocalDateTime.now())
                .exportedBy(currentUser != null ? currentUser.getFullName() : null)
                .schemaVersion(GS1_SCHEMA_VERSION)
                .schemaDescription(GS1_SCHEMA_DESCRIPTION)
                .build();
    }

    private GS1DossierExportResponse.ShipmentInfo buildShipmentInfo(Shipment shipment) {
        ProductionLot lot = shipment.getProductionLot();
        String productCategory = lot != null && lot.getProductCategory() != null
                ? lot.getProductCategory().getName()
                : null;
        String unit = lot != null ? lot.getExpectedQuantityUnit() : null;

        GS1DossierExportResponse.OrganizationInfo orgInfo = null;
        if (shipment.getOrganization() != null) {
            orgInfo = GS1DossierExportResponse.OrganizationInfo.builder()
                    .id(shipment.getOrganization().getOrganizationId())
                    .name(shipment.getOrganization().getName())
                    .code(shipment.getOrganization().getCode())
                    .build();
        }

        return GS1DossierExportResponse.ShipmentInfo.builder()
                .id(shipment.getId())
                .name(shipment.getName())
                .codeValue(null) // Shipment khong co field codeValue
                .productCategory(productCategory)
                .totalQuantity(shipment.getTotalQuantity())
                .unit(unit)
                .status(shipment.getStatus() != null ? shipment.getStatus().name() : null)
                .organization(orgInfo)
                .build();
    }

    private Map<String, String> buildGs1Mapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("ChainEvent.id", "eventIdentifier");
        mapping.put("ChainEvent.eventType", "eventTypeCode");
        mapping.put("ChainEvent.recordedAt", "eventDateTime");
        mapping.put("ChainEvent.recordedBy.fullName", "actorName");
        mapping.put("ChainEvent.location.latitude", "eventLocation.latitude");
        mapping.put("ChainEvent.location.longitude", "eventLocation.longitude");
        mapping.put("ChainEvent.location.address", "eventLocation.address");
        mapping.put("ChainEvent.eventData", "eventData");
        mapping.put("Shipment.name", "shipmentName");
        mapping.put("Shipment.totalQuantity", "declaredQuantity");
        mapping.put("Shipment.status", "shipmentStatus");
        return mapping;
    }

    private String eventTypeLabel(ChainEventType type) {
        return switch (type) {
            case HARVEST -> "Thu hoạch";
            case PACKAGING -> "Đóng gói";
            case TRANSPORT -> "Vận chuyển";
            case PROCUREMENT -> "Thu mua";
            case CORRECTION -> "Sửa lỗi";
            case WAREHOUSE_RECEIPT -> "Nhập kho";
            case STORAGE_CONDITION -> "Theo dõi bảo quản";
        };
    }

    private Map<String, Object> parseEventData(String eventDataJson) {
        if (eventDataJson == null || eventDataJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(eventDataJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Không thể parse eventData: {}", eventDataJson);
            return new HashMap<>();
        }
    }

    private String generateGs1Xml(GS1DossierExportResponse dossier) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gs1Dossier>\n");

        GS1DossierExportResponse.ShipmentInfo shipment = dossier.getShipment();
        if (shipment != null) {
            sb.append("  <shipment>\n");
            sb.append("    <id>").append(shipment.getId()).append("</id>\n");
            sb.append("    <name>").append(escapeXml(shipment.getName())).append("</name>\n");
            if (shipment.getCodeValue() != null) {
                sb.append("    <codeValue>").append(escapeXml(shipment.getCodeValue())).append("</codeValue>\n");
            }
            if (shipment.getProductCategory() != null) {
                sb.append("    <productCategory>").append(escapeXml(shipment.getProductCategory()))
                        .append("</productCategory>\n");
            }
            if (shipment.getTotalQuantity() != null) {
                sb.append("    <totalQuantity>").append(shipment.getTotalQuantity()).append("</totalQuantity>\n");
            }
            if (shipment.getUnit() != null) {
                sb.append("    <unit>").append(escapeXml(shipment.getUnit())).append("</unit>\n");
            }
            if (shipment.getStatus() != null) {
                sb.append("    <status>").append(escapeXml(shipment.getStatus())).append("</status>\n");
            }
            if (shipment.getOrganization() != null) {
                sb.append("    <organization>\n");
                sb.append("      <id>").append(shipment.getOrganization().getId()).append("</id>\n");
                sb.append("      <name>").append(escapeXml(shipment.getOrganization().getName())).append("</name>\n");
                sb.append("      <code>").append(escapeXml(shipment.getOrganization().getCode())).append("</code>\n");
                sb.append("    </organization>\n");
            }
            sb.append("  </shipment>\n");
        }

        sb.append("  <events>\n");
        if (dossier.getEvents() != null) {
            for (GS1Event event : dossier.getEvents()) {
                sb.append("    <event>\n");
                sb.append("      <eventId>").append(event.getEventId()).append("</eventId>\n");
                sb.append("      <eventType>").append(escapeXml(event.getEventType())).append("</eventType>\n");
                if (event.getEventTypeLabel() != null) {
                    sb.append("      <eventTypeLabel>").append(escapeXml(event.getEventTypeLabel()))
                            .append("</eventTypeLabel>\n");
                }
                if (event.getRecordedAt() != null) {
                    sb.append("      <recordedAt>").append(event.getRecordedAt()).append("</recordedAt>\n");
                }
                if (event.getRecordedBy() != null) {
                    sb.append("      <recordedBy>").append(escapeXml(event.getRecordedBy())).append("</recordedBy>\n");
                }
                if (event.getLocation() != null) {
                    sb.append("      <location>\n");
                    sb.append("        <latitude>").append(event.getLocation().getLatitude()).append("</latitude>\n");
                    sb.append("        <longitude>").append(event.getLocation().getLongitude()).append("</longitude>\n");
                    if (event.getLocation().getAddress() != null) {
                        sb.append("        <address>").append(escapeXml(event.getLocation().getAddress()))
                                .append("</address>\n");
                    }
                    sb.append("      </location>\n");
                }
                if (event.getDetails() != null && !event.getDetails().isEmpty()) {
                    sb.append("      <details>").append(toJsonForXml(event.getDetails())).append("</details>\n");
                }
                sb.append("    </event>\n");
            }
        }
        sb.append("  </events>\n");

        if (dossier.getMapping() != null) {
            sb.append("  <mapping>\n");
            for (Map.Entry<String, String> entry : dossier.getMapping().entrySet()) {
                sb.append("    <entry>\n");
                sb.append("      <systemField>").append(escapeXml(entry.getKey())).append("</systemField>\n");
                sb.append("      <gs1Field>").append(escapeXml(entry.getValue())).append("</gs1Field>\n");
                sb.append("    </entry>\n");
            }
            sb.append("  </mapping>\n");
        }

        sb.append("  <warnings>\n");
        if (dossier.getWarnings() != null) {
            for (Warning warning : dossier.getWarnings()) {
                sb.append("    <warning>\n");
                sb.append("      <eventId>").append(warning.getEventId()).append("</eventId>\n");
                sb.append("      <field>").append(escapeXml(warning.getField())).append("</field>\n");
                sb.append("      <message>").append(escapeXml(warning.getMessage())).append("</message>\n");
                sb.append("    </warning>\n");
            }
        }
        sb.append("  </warnings>\n");

        if (dossier.getExportedAt() != null) {
            sb.append("  <exportedAt>").append(dossier.getExportedAt()).append("</exportedAt>\n");
        }
        if (dossier.getExportedBy() != null) {
            sb.append("  <exportedBy>").append(escapeXml(dossier.getExportedBy())).append("</exportedBy>\n");
        }
        if (dossier.getSchemaVersion() != null) {
            sb.append("  <schemaVersion>").append(escapeXml(dossier.getSchemaVersion())).append("</schemaVersion>\n");
        }
        if (dossier.getSchemaDescription() != null) {
            sb.append("  <schemaDescription>").append(escapeXml(dossier.getSchemaDescription()))
                    .append("</schemaDescription>\n");
        }

        sb.append("</gs1Dossier>");
        return sb.toString();
    }

    private String toJsonForXml(Map<String, Object> details) {
        try {
            return escapeXml(objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.warn("Không thể serialize eventData cho XML: {}", e.getMessage());
            return "";
        }
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        String amp = "&" + "amp;";
        String lt = "&" + "lt;";
        String gt = "&" + "gt;";
        String quot = "&" + "quot;";
        String apos = "&" + "apos;";
        return value.replace("&", amp)
                .replace("<", lt)
                .replace(">", gt)
                .replace("\"", quot)
                .replace("'", apos);
    }

    private void validateDossierAccess(Shipment shipment, CustomUserDetails currentUser) {
        String role = currentUser.getRoleCode();

        // 1. Quyền Admin (VT-01): Được phép truy cập mọi lô
        if ("VT-01".equals(role)) {
            return;
        }

        // 2. Quyền Quản lý HTX (VT-02): Lô hàng phải thuộc HTX của mình
        if ("VT-02".equals(role)) {
            UUID userOrgId = currentUser.getOrganizationId();
            UUID shipmentOrgId = shipment.getOrganization().getOrganizationId();
            if (!shipmentOrgId.equals(userOrgId)) {
                throw new AccessDeniedException("Từ chối thao tác: Bạn không có quyền truy cập lô hàng này.");
            }
            return;
        }

        // 3. Quyền Doanh nghiệp thu mua (VT-04): Lô hàng phải được thu mua bởi doanh
        // nghiệp của mình
        if ("VT-04".equals(role)) {
            boolean isAssociated = false;
            List<ChainEvent> events = chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipment.getId());
            for (ChainEvent event : events) {
                if (event.getEventType() == ChainEventType.PROCUREMENT && !event.isCorrection()) {
                    UUID recorderId = event.getRecordedBy().getUserId();
                    boolean belongsToSameOrg = organizationUserRepository
                            .findByOrganization_OrganizationIdAndUser_UserId(
                                    currentUser.getOrganizationId(), recorderId)
                            .isPresent();
                    if (belongsToSameOrg) {
                        isAssociated = true;
                        break;
                    }
                }
            }

            if (!isAssociated) {
                throw new AccessDeniedException(
                        "Từ chối thao tác: Lô hàng này không thuộc sở hữu thu mua của doanh nghiệp bạn.");
            }
            return;
        }

        // Các role khác không được phép truy cập
        throw new AccessDeniedException("Từ chối thao tác: Bạn không có quyền xem hoặc xuất hồ sơ cho lô hàng này.");
    }

    private void logDossierExport(Shipment shipment, CustomUserDetails currentUser, String status, String ipAddress,
            Long fileSize) {
        try {
            User user = userRepository.findById(currentUser.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người xuất."));

            Organization org = Organization.builder()
                    .organizationId(currentUser.getOrganizationId())
                    .build();

            String fileName = "Ho_so_truy_xuat_" + shipment.getName().replaceAll("\\s+", "_") + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            DossierExportHistory history = DossierExportHistory.builder()
                    .shipment(shipment)
                    .exporter(user)
                    .organization(org)
                    .exportedAt(LocalDateTime.now())
                    .fileName(fileName)
                    .fileSize(fileSize)
                    .status(status)
                    .ipAddress(ipAddress)
                    .build();

            exportHistoryRepository.save(history);
            log.info("Ghi log xuất hồ sơ thành công cho user: {}, status: {}", currentUser.getUsername(), status);
        } catch (Exception e) {
            log.error("Lỗi khi lưu lịch sử xuất hồ sơ truy xuất: {}", e.getMessage());
        }
    }

    private void publishActivityLog(CustomUserDetails currentUser, String action, String description, String entityType,
            String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
