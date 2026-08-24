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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;
import vn.nguongocso.report.dto.response.Gs1Event;
import vn.nguongocso.report.dto.response.Gs1EventLocation;
import vn.nguongocso.report.dto.response.Gs1ShipmentInfo;
import vn.nguongocso.report.dto.response.Gs1Warning;
import vn.nguongocso.report.entity.DossierExportHistory;
import vn.nguongocso.report.repository.DossierExportHistoryRepository;
import vn.nguongocso.report.service.DossierService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final TraceCodeRepository traceCodeRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
        if (lot == null) {
            missingDocs.add("Lô hàng chưa gắn với Lô sản xuất nào");
        } else {
            if (lot.getStatus() == null || (lot.getStatus() != ProductionLotStatus.CLOSED && lot.getStatus() != ProductionLotStatus.PACKAGED)) {
                missingDocs.add("Lô sản xuất tương ứng chưa hoàn tất (Trạng thái yêu cầu: CLOSED hoặc PACKAGED)");
            }

            List<FarmLog> logs = lot.getId() != null
                    ? farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId())
                    : Collections.emptyList();

            boolean hasPlanting = false;
            boolean hasFertilizing = false;
            boolean hasPesticide = false;
            boolean hasHarvesting = false;

            if (logs != null) {
                for (FarmLog logItem : logs) {
                    if (logItem == null) continue;
                    List<FarmLogAttachment> attachments = logItem.getId() != null
                            ? farmLogAttachmentRepository.findByFarmLogId(logItem.getId())
                            : Collections.emptyList();
                    if (attachments != null && !attachments.isEmpty() && logItem.getActivityType() != null) {
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
            }

            if (!hasPlanting)
                missingDocs.add("Thiếu chứng từ gieo giống/xuống giống (PLANTING)");
            if (!hasFertilizing)
                missingDocs.add("Thiếu chứng từ bón phân (FERTILIZING)");
            if (!hasPesticide)
                missingDocs.add("Thiếu chứng từ phun thuốc/phòng trừ sâu bệnh (PESTICIDE)");
            if (!hasHarvesting)
                missingDocs.add("Thiếu chứng từ thu hoạch (HARVESTING)");
        }

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

            // Cấu hình Font hỗ trợ hiển thị Tiếng Việt an toàn khi chạy trong JAR/Docker
            Font titleFont;
            Font headerFont;
            Font boldFont;
            Font normalFont;

            try (InputStream boldStream = getClass().getResourceAsStream("/fonts/Roboto-Bold.ttf");
                 InputStream regularStream = getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf")) {
                if (boldStream != null && regularStream != null) {
                    byte[] boldBytes = boldStream.readAllBytes();
                    byte[] regularBytes = regularStream.readAllBytes();
                    BaseFont boldBf = BaseFont.createFont("Roboto-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, boldBytes, null);
                    BaseFont regularBf = BaseFont.createFont("Roboto-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, regularBytes, null);
                    titleFont = new Font(boldBf, 16, Font.BOLD, Color.BLACK);
                    headerFont = new Font(boldBf, 12, Font.BOLD, Color.BLACK);
                    boldFont = new Font(boldBf, 10, Font.BOLD, Color.BLACK);
                    normalFont = new Font(regularBf, 10, Font.NORMAL, Color.BLACK);
                } else {
                    titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
                    headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
                    boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
                    normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
                }
            } catch (Exception ex) {
                log.warn("Không thể tải font Roboto tùy chỉnh, dùng font mặc định: {}", ex.getMessage());
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

            Paragraph subtitle = new Paragraph("Mã lô hàng: " + (shipment.getId() != null ? shipment.getId().toString() : "N/A"), normalFont);
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

            ProductionLot lot = shipment.getProductionLot();
            addTableCell(lotTable, "Tên lô sản xuất:", boldFont);
            addTableCell(lotTable, lot != null && lot.getName() != null ? lot.getName() : "N/A", normalFont);
            addTableCell(lotTable, "Danh mục sản phẩm:", boldFont);
            addTableCell(lotTable, (lot != null && lot.getProductCategory() != null && lot.getProductCategory().getName() != null)
                    ? lot.getProductCategory().getName() : "N/A", normalFont);
            addTableCell(lotTable, "Đơn vị sản xuất (HTX):", boldFont);
            addTableCell(lotTable, (lot != null && lot.getOrganization() != null && lot.getOrganization().getName() != null)
                    ? lot.getOrganization().getName() : "N/A", normalFont);
            addTableCell(lotTable, "Ngày xuống giống:", boldFont);
            addTableCell(lotTable,
                    (lot != null && lot.getPlantingDate() != null)
                            ? lot.getPlantingDate().toString()
                            : "N/A",
                    normalFont);
            addTableCell(lotTable, "Ngày thu hoạch:", boldFont);
            addTableCell(lotTable,
                    (lot != null && lot.getHarvestDate() != null)
                            ? lot.getHarvestDate().toString()
                            : "N/A",
                    normalFont);
            addTableCell(lotTable, "Sản lượng dự kiến:", boldFont);
            addTableCell(lotTable, (lot != null && lot.getExpectedQuantity() != null)
                    ? lot.getExpectedQuantity() + " " + (lot.getExpectedQuantityUnit() != null ? lot.getExpectedQuantityUnit() : "kg")
                    : "N/A", normalFont);
            addTableCell(lotTable, "Sản lượng thực tế:", boldFont);
            addTableCell(lotTable,
                    (lot != null && lot.getActualQuantity() != null)
                            ? lot.getActualQuantity() + " kg"
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
            addTableCell(shipmentTable, shipment.getName() != null ? shipment.getName() : "N/A", normalFont);
            addTableCell(shipmentTable, "Số lượng lô hàng:", boldFont);
            addTableCell(shipmentTable, shipment.getTotalQuantity() + " sản phẩm", normalFont);
            addTableCell(shipmentTable, "Thông tin đóng gói:", boldFont);
            addTableCell(shipmentTable, shipment.getPackagingInfo() != null ? shipment.getPackagingInfo() : "N/A",
                    normalFont);
            addTableCell(shipmentTable, "Trạng thái vận hành:", boldFont);
            addTableCell(shipmentTable, shipment.getStatus() != null ? shipment.getStatus().name() : "N/A", normalFont);

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

            List<FarmLog> logs = (lot != null && lot.getId() != null)
                    ? farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId())
                    : Collections.emptyList();
            if (logs != null) {
                for (FarmLog logItem : logs) {
                    if (logItem == null) continue;
                    addTableCell(logTable, logItem.getExecutedDate() != null ? logItem.getExecutedDate().toString() : "N/A", normalFont);
                    addTableCell(logTable, logItem.getActivityType() != null ? logItem.getActivityType().name() : "N/A", normalFont);
                    String materialInfo = (logItem.getMaterial() != null ? logItem.getMaterial() : "") +
                            (logItem.getQuantity() != null ? " (" + logItem.getQuantity() + " " + (logItem.getUnit() != null ? logItem.getUnit() : "") + ")"
                                    : "");
                    addTableCell(logTable, materialInfo.trim().isEmpty() ? "Không có" : materialInfo.trim(), normalFont);
                    addTableCell(logTable, logItem.getNotes() != null ? logItem.getNotes() : "", normalFont);

                    // Lấy chứng từ đính kèm
                    List<FarmLogAttachment> attachments = logItem.getId() != null
                            ? farmLogAttachmentRepository.findByFarmLogId(logItem.getId())
                            : Collections.emptyList();
                    StringBuilder filesStr = new StringBuilder();
                    if (attachments != null) {
                        for (FarmLogAttachment att : attachments) {
                            if (att != null && att.getFileName() != null) {
                                if (filesStr.length() > 0)
                                    filesStr.append("\n");
                                filesStr.append(att.getFileName());
                            }
                        }
                    }
                    addTableCell(logTable, filesStr.toString().isEmpty() ? "Không có" : filesStr.toString(), normalFont);
                }
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
            if (events != null) {
                for (ChainEvent ev : events) {
                    if (ev == null) continue;
                    addTableCell(eventTable, ev.getRecordedAt() != null ? ev.getRecordedAt().format(formatter) : "N/A", normalFont);
                    String eventTypeStr = ev.getEventType() != null ? ev.getEventType().name() : "N/A";
                    addTableCell(eventTable, eventTypeStr + (ev.isCorrection() ? " (Đã điều chỉnh)" : ""),
                            normalFont);
                    addTableCell(eventTable, ev.getEventData() != null ? ev.getEventData() : "", normalFont);
                    String recordedByName = "Hệ thống";
                    if (ev.getRecordedBy() != null) {
                        recordedByName = ev.getRecordedBy().getFullName() != null
                                ? ev.getRecordedBy().getFullName()
                                : (ev.getRecordedBy().getUserName() != null ? ev.getRecordedBy().getUserName() : "Hệ thống");
                    }
                    addTableCell(eventTable, recordedByName, normalFont);
                }
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
                    "Xuất hồ sơ truy xuất cho lô hàng " + (shipment.getName() != null ? shipment.getName() : ""),
                    "Shipment",
                    shipment.getId() != null ? shipment.getId().toString() : "");

            return pdfData;
        } catch (DossierValidationException dve) {
            throw dve;
        } catch (Exception e) {
            log.error("Lỗi xuất file PDF cho shipmentId = {}: {}", shipmentId, e.getMessage(), e);
            throw new BusinessException("Lỗi hệ thống khi sinh file PDF hồ sơ truy xuất: " + e.getMessage());
        }
    }

    /**
     * Xuất hồ sơ truy xuất theo lược đồ GS1 mô phỏng.
     *
     * <p>
     * Chỉ dành cho VT-02 (Quản lý HTX) và VT-04 (Doanh nghiệp thu mua). Hồ sơ
     * được ánh xạ theo bốn chiều {@code who / when / where / why}. Quy trình:
     * xác thực → kiểm tra QTN-11 → kiểm tra sự kiện không rỗng → ánh xạ sự
     * kiện → ghi ActivityLog. Không thay đổi bất kỳ dữ liệu nghiệp vụ nào.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Gs1DossierExportResponse exportGs1Dossier(UUID shipmentId,
                                                     String format,
                                                     boolean includeMapping,
                                                     CustomUserDetails currentUser,
                                                     String ipAddress) {
        // 1. Kiểm tra định dạng hợp lệ
        if (format != null && !format.isBlank()
                && !"json".equalsIgnoreCase(format) && !"xml".equalsIgnoreCase(format)) {
            throw new BusinessException(
                    "Định dạng xuất không được hỗ trợ. Chỉ hỗ trợ json hoặc xml.");
        }

        // 2. Tìm lô hàng
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        // 3. Kiểm tra sự kiện thu mua (bắt buộc với hồ sơ GS1)
        boolean hasProcurement = chainEventRepository.existsByShipmentIdAndEventType(
                shipmentId, ChainEventType.PROCUREMENT);
        if (!hasProcurement) {
            throw new BusinessException(
                    "Lô hàng chưa có sự kiện thu mua. Vui lòng ghi nhận sự kiện thu mua trước khi xuất hồ sơ GS1.");
        }

        // 4. Kiểm tra quyền truy cập (VT-02 / VT-04 được xử lý bởi @PreAuthorize,
        // tại đây kiểm tra phạm vi tổ chức)
        validateDossierAccess(shipment, currentUser);

        // 5. Kiểm tra QTN-11
        DossierCheckResponse checkResult = checkEligibility(shipmentId, currentUser);
        if (!checkResult.isEligible()) {
            throw new DossierValidationException("Không đủ điều kiện xuất hồ sơ truy xuất.",
                    checkResult.getMissingDocuments());
        }

        // 6. Kiểm tra sự kiện không rỗng (không tạo hồ sơ trống)
        List<ChainEvent> events = chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId);
        if (events == null || events.isEmpty()) {
            throw new BusinessException("Lô chưa có sự kiện nào để xuất hồ sơ.");
        }

        // 7. Xây dựng hồ sơ GS1 mô phỏng
        List<Gs1Warning> warnings = new ArrayList<>();
        Gs1DossierExportResponse response = Gs1DossierExportResponse.builder()
                .shipment(buildGs1ShipmentInfo(shipment))
                .events(events.stream()
                        .map(e -> mapToGs1Event(e, warnings))
                        .collect(Collectors.toList()))
                .mapping(includeMapping ? buildMappingTable() : null)
                .warnings(warnings)
                .exportedAt(LocalDateTime.now())
                .exportedBy(currentUser.getFullName())
                .schemaVersion("1.0.0")
                .schemaDescription(
                        "Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ GS1")
                .build();

        // 7. Ghi ActivityLog (audit)
        publishActivityLog(
                currentUser,
                "GS1_DOSSIER_EXPORT",
                "Xuất hồ sơ GS1 cho lô hàng " + shipment.getName(),
                "Shipment",
                shipment.getId().toString());

        return response;
    }

    private Gs1ShipmentInfo buildGs1ShipmentInfo(Shipment shipment) {
        ProductionLot lot = shipment.getProductionLot();

        // Best effort: danh sách mã truy xuất (TraceCode.codeValue)
        List<String> codeValues = Collections.emptyList();
        try {
            List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(shipment.getId());
            if (traceCodes != null && !traceCodes.isEmpty()) {
                codeValues = traceCodes.stream()
                        .map(TraceCode::getCodeValue)
                        .filter(v -> v != null && !v.isBlank())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Không thể lấy mã truy xuất cho shipment {}: {}", shipment.getId(), e.getMessage());
        }

        return Gs1ShipmentInfo.builder()
                .id(shipment.getId())
                .name(shipment.getName())
                .codeValues(codeValues.isEmpty() ? null : codeValues)
                .productCategory(lot != null && lot.getProductCategory() != null
                        ? lot.getProductCategory().getName()
                        : null)
                .totalQuantity(shipment.getTotalQuantity())
                .unit(lot != null ? lot.getExpectedQuantityUnit() : null)
                .status(shipment.getStatus().name())
                .organization(Gs1ShipmentInfo.OrganizationInfo.builder()
                        .id(shipment.getOrganization().getOrganizationId())
                        .name(shipment.getOrganization().getName())
                        .code(shipment.getOrganization().getCode())
                        .build())
                .build();
    }

    private Gs1Event mapToGs1Event(ChainEvent event, List<Gs1Warning> warnings) {
        Gs1EventLocation location = null;

        // where: toạ độ từ ChainEvent.location (JTS Point)
        if (event.getLocation() != null) {
            location = Gs1EventLocation.builder()
                    .latitude(event.getLocation().getY())
                    .longitude(event.getLocation().getX())
                    // address không tồn tại trong domain → null
                    .address(null)
                    .build();
        } else {
            warnings.add(Gs1Warning.builder()
                    .eventId(event.getId())
                    .field("location")
                    .message("Sự kiện thiếu thông tin vị trí")
                    .build());
        }

        // why/what: loại sự kiện + dữ liệu chi tiết
        Map<String, Object> details = parseEventData(event.getEventData());

        return Gs1Event.builder()
                .eventId(event.getId())
                .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                .eventTypeLabel(getEventTypeLabel(event.getEventType()))
                .recordedAt(event.getRecordedAt())
                .recordedBy(event.getRecordedBy() != null ? event.getRecordedBy().getFullName() : null)
                .location(location)
                .details(details.isEmpty() ? null : details)
                .build();
    }

    private String getEventTypeLabel(ChainEventType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case HARVEST -> "Thu hoạch";
            case PREPROCESSING -> "Sơ chế và phân loại";
            case PACKAGING -> "Đóng gói";
            case TRANSPORT -> "Vận chuyển";
            case PROCUREMENT -> "Thu mua";
            case CORRECTION -> "Điều chỉnh";
            case WAREHOUSE_RECEIPT -> "Nhập kho";
            case STORAGE_CONDITION -> "Theo dõi bảo quản";
            default -> type.name();
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEventData(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Collections.singletonMap("raw", json);
        }
    }

    private Map<String, String> buildMappingTable() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("ChainEvent.id", "eventIdentifier");
        mapping.put("ChainEvent.eventType", "eventTypeCode");
        mapping.put("ChainEvent.recordedAt", "eventDateTime");
        mapping.put("ChainEvent.recordedBy.fullName", "actorName");
        mapping.put("ChainEvent.location.latitude", "eventLocation.latitude");
        mapping.put("ChainEvent.location.longitude", "eventLocation.longitude");
        mapping.put("ChainEvent.location.address", "eventLocation.address");
        mapping.put("ChainEvent.eventData", "details");
        mapping.put("Shipment.name", "shipmentName");
        mapping.put("Shipment.totalQuantity", "declaredQuantity");
        mapping.put("Shipment.status", "shipmentStatus");
        mapping.put("TraceCode.codeValue", "codeValues");
        return mapping;
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
