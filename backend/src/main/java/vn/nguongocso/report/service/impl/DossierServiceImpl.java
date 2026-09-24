package vn.nguongocso.report.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.export.entity.ProfileTemplateField;
import vn.nguongocso.export.repository.ProfileTemplateRepository;
import vn.nguongocso.export.util.ExportDisplayFormatter;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.report.dto.request.BatchDossierCheckRequest;
import vn.nguongocso.report.dto.request.BatchDossierExportRequest;
import vn.nguongocso.report.dto.response.BatchDossierCheckResponse;
import vn.nguongocso.report.dto.response.BatchDossierHistoryDto;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;
import vn.nguongocso.report.dto.response.Gs1Event;
import vn.nguongocso.report.dto.response.Gs1EventLocation;
import vn.nguongocso.report.dto.response.Gs1Inspection;
import vn.nguongocso.report.dto.response.Gs1InspectionCriterion;
import vn.nguongocso.report.dto.response.Gs1ShipmentInfo;
import vn.nguongocso.report.dto.response.Gs1Warning;
import vn.nguongocso.report.entity.DossierExportHistory;
import vn.nguongocso.report.exception.DossierValidationException;
import vn.nguongocso.report.repository.DossierExportHistoryRepository;
import vn.nguongocso.report.service.DossierService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Triển khai dịch vụ xử lý nghiệp vụ hồ sơ truy xuất. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DossierServiceImpl implements DossierService {
    private final ShipmentRepository shipmentRepository;
    private final ShipmentHandoverRepository shipmentHandoverRepository;
    private final FarmLogRepository farmLogRepository;
    private final FarmLogAttachmentRepository farmLogAttachmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final DossierExportHistoryRepository exportHistoryRepository;
    private final UserRepository userRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final InspectionCriterionResultRepository inspectionCriterionResultRepository;
    private final ProductionLotCertificationRepository productionLotCertificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProfileTemplateRepository profileTemplateRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /** Kiểm tra điều kiện xuất hồ sơ truy xuất cho một lô hàng. */
    @Override
    @Transactional(readOnly = true)
    public DossierCheckResponse checkEligibility(
        UUID shipmentId,
        CustomUserDetails currentUser) {

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
                    if (logItem == null) {
                        continue;
                    }
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

            if (!hasPlanting) {
                missingDocs.add("Thiếu chứng từ gieo giống/xuống giống (PLANTING)");
            }
            if (!hasFertilizing) {
                missingDocs.add("Thiếu chứng từ bón phân (FERTILIZING)");
            }
            if (!hasPesticide) {
                missingDocs.add("Thiếu chứng từ phun thuốc/phòng trừ sâu bệnh (PESTICIDE)");
            }
            if (!hasHarvesting) {
                missingDocs.add("Thiếu chứng từ thu hoạch (HARVESTING)");
            }
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

    /** Xuất hồ sơ truy xuất cho một lô hàng dưới dạng PDF. */
    @Override
    @Transactional
    public byte[] exportDossierPdf(
        UUID shipmentId,
        CustomUserDetails currentUser,
        String ipAddress) {
        return exportDossierPdf(shipmentId, null, currentUser, ipAddress);
    }

    /** Xuất hồ sơ truy xuất dạng PDF áp dụng mẫu cấu hình trường đối tác. */
    @Override
    @Transactional
    public byte[] exportDossierPdf(
        UUID shipmentId,
        UUID templateId,
        CustomUserDetails currentUser,
        String ipAddress) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        validateDossierAccess(shipment, currentUser);

        DossierCheckResponse checkResult = checkEligibility(shipmentId, currentUser);
        if (!checkResult.isEligible()) {
            logDossierExport(shipment, currentUser, "FAILED", ipAddress, 0L);
            throw new DossierValidationException("Không đủ điều kiện xuất hồ sơ truy xuất.",
                checkResult.getMissingDocuments());
        }

        ProfileTemplate template = null;
        UUID userOrgId = currentUser != null ? currentUser.getOrganizationId() : null;
        UUID effectiveOrgId = userOrgId;
        if ("VT-04".equals(currentUser.getRoleCode()) && shipment.getOrganization() != null) {
            effectiveOrgId = shipment.getOrganization().getOrganizationId();
        }
        if (templateId != null && profileTemplateRepository != null) {
            template = profileTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));
            if ("VT-04".equals(currentUser.getRoleCode())) {
                if (effectiveOrgId != null && !template.getOrganization().getOrganizationId().equals(effectiveOrgId)) {
                    throw new AccessDeniedException("Mẫu hồ sơ không thuộc tổ chức của lô hàng này.");
                }
            } else {
                if (userOrgId != null && !template.getOrganization().getOrganizationId().equals(userOrgId)) {
                    throw new AccessDeniedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
                }
            }
        } else if (effectiveOrgId != null && profileTemplateRepository != null) {
            template = profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(effectiveOrgId)
                .orElse(null);
        }

        Set<String> selectedFieldKeys = null;
        if (template != null && template.getFields() != null && !template.getFields().isEmpty()) {
            selectedFieldKeys = template.getFields().stream()
                .map(ProfileTemplateField::getFieldKey)
                .collect(Collectors.toSet());
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = loadFont("fonts/Roboto-Bold.ttf", 16, Font.BOLD);
            Font headerFont = loadFont("fonts/Roboto-Bold.ttf", 12, Font.BOLD);
            Font boldFont = loadFont("fonts/Roboto-Bold.ttf", 10, Font.BOLD);
            Font normalFont = loadFont("fonts/Roboto-Regular.ttf", 10, Font.NORMAL);

            renderShipmentDossierPdf(document, shipment, template, selectedFieldKeys, titleFont, headerFont, boldFont, normalFont);

            document.close();

            byte[] pdfData = out.toByteArray();
            long fileSize = pdfData.length;

            logDossierExport(shipment, currentUser, "SUCCESS", ipAddress, fileSize,
                template != null ? template.getId() : null);

            publishActivityLog(
                currentUser,
                "EXPORT",
                "Xuất hồ sơ truy xuất cho lô hàng " + (shipment.getName() != null ? shipment.getName() : "")
                    + (template != null ? " theo mẫu: " + template.getName() : ""),
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

    /** Xuất hồ sơ truy xuất theo lược đồ GS1 mô phỏng. */
    @Override
    @Transactional(readOnly = true)
    public Gs1DossierExportResponse exportGs1Dossier(
        UUID shipmentId,
        String format,
        boolean includeMapping,
        CustomUserDetails currentUser,
        String ipAddress) {

        if (format != null && !format.isBlank()
            && !"json".equalsIgnoreCase(format) && !"xml".equalsIgnoreCase(format)) {
            throw new BusinessException("Định dạng xuất không được hỗ trợ. Chỉ hỗ trợ json hoặc xml.");
        }

        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        boolean hasProcurement = chainEventRepository.existsByShipmentIdAndEventType(
            shipmentId, ChainEventType.PROCUREMENT);
        if (!hasProcurement) {
            throw new BusinessException("Lô hàng chưa có sự kiện thu mua. Vui lòng ghi nhận sự kiện thu mua trước khi xuất hồ sơ GS1.");
        }

        validateDossierAccess(shipment, currentUser);

        DossierCheckResponse checkResult = checkEligibility(shipmentId, currentUser);
        if (!checkResult.isEligible()) {
            throw new DossierValidationException("Không đủ điều kiện xuất hồ sơ truy xuất.",
                checkResult.getMissingDocuments());
        }

        List<ChainEvent> events = getShipmentEventsWithLineage(shipment);
        if (events == null || events.isEmpty()) {
            throw new BusinessException("Lô chưa có sự kiện nào để xuất hồ sơ.");
        }

        List<Gs1Warning> warnings = new ArrayList<>();
        Gs1DossierExportResponse response = Gs1DossierExportResponse.builder()
            .shipment(buildGs1ShipmentInfo(shipment))
            .events(events.stream()
                .map(e -> mapToGs1Event(e, warnings))
                .collect(Collectors.toList()))
            .inspections(buildGs1Inspections(shipment))
            .mapping(includeMapping ? buildMappingTable() : null)
            .warnings(warnings)
            .exportedAt(LocalDateTime.now())
            .exportedBy(currentUser.getFullName())
            .schemaVersion("1.0.0")
            .schemaDescription("Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ GS1")
            .build();

        publishActivityLog(
            currentUser,
            "GS1_DOSSIER_EXPORT",
            "Xuất hồ sơ GS1 cho lô hàng " + shipment.getName(),
            "Shipment",
            shipment.getId().toString());

        return response;
    }

    /** Xây dựng thông tin lô hàng cho hồ sơ GS1. */
    private Gs1ShipmentInfo buildGs1ShipmentInfo(Shipment shipment) {
        ProductionLot lot = shipment.getProductionLot();

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

    /** Ánh xạ sự kiện chuỗi cung ứng sang đối tượng GS1. */
    private Gs1Event mapToGs1Event(
        ChainEvent event,
        List<Gs1Warning> warnings) {

        Gs1EventLocation location = null;

        if (event.getLocation() != null) {
            location = Gs1EventLocation.builder()
                .latitude(event.getLocation().getY())
                .longitude(event.getLocation().getX())
                .address(null)
                .build();
        } else {
            warnings.add(Gs1Warning.builder()
                .eventId(event.getId())
                .field("location")
                .message("Sự kiện thiếu thông tin vị trí")
                .build());
        }

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

    /** Lấy nhãn hiển thị loại sự kiện. */
    private String getEventTypeLabel(ChainEventType type) {
        return ExportDisplayFormatter.formatChainEventType(type);
    }

    /** Giải mã dữ liệu sự kiện từ chuỗi JSON. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEventData(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.singletonMap("raw", json);
        }
    }

    /** Xây dựng bảng ánh xạ trường GS1. */
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
        mapping.put("InspectionRequest.inspectionUnit", "inspections[].inspectionUnit");
        mapping.put("InspectionRequest.sampleSentDate", "inspections[].sampleSentDate");
        mapping.put("InspectionRequest.status", "inspections[].status");
        mapping.put("InspectionCriterion.criterionCode", "inspections[].criteria[].criterionCode");
        mapping.put("InspectionCriterion.criterionName", "inspections[].criteria[].criterionName");
        mapping.put("InspectionCriterionResult.passed", "inspections[].criteria[].passed");
        mapping.put("InspectionCriterionResult.resultDate", "inspections[].criteria[].resultDate");
        mapping.put("InspectionCriterionResult.expiryDate", "inspections[].criteria[].expiryDate");
        return mapping;
    }

    /** Nạp danh sách yêu cầu kiểm nghiệm của lô sản xuất tương ứng. */
    private List<InspectionRequest> loadInspectionRequests(Shipment shipment) {
        if (shipment == null) {
            return Collections.emptyList();
        }
        ProductionLot lot = shipment.getProductionLot();
        if (lot == null || lot.getId() == null) {
            return Collections.emptyList();
        }
        try {
            List<InspectionRequest> requests = inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lot.getId());
            return requests != null ? requests : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Không thể lấy lịch sử kiểm nghiệm cho shipment {}: {}",
                shipment.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Nạp kết quả kiểm nghiệm của mọi chỉ tiêu thuộc yêu cầu kiểm nghiệm theo ID. */
    private Map<UUID, InspectionCriterionResult> loadResultsByCriterionId(InspectionRequest request) {
        try {
            List<InspectionCriterionResult> results = inspectionCriterionResultRepository
                .findByInspectionCriterion_InspectionRequest_Id(request.getId());
            if (results == null || results.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<UUID, InspectionCriterionResult> resultByCriterionId = new LinkedHashMap<>();
            for (InspectionCriterionResult result : results) {
                if (result != null && result.getInspectionCriterion() != null
                    && result.getInspectionCriterion().getId() != null) {
                    resultByCriterionId.put(result.getInspectionCriterion().getId(), result);
                }
            }
            return resultByCriterionId;
        } catch (Exception e) {
            log.warn("Không thể lấy kết quả kiểm nghiệm cho yêu cầu {}: {}",
                request.getId(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** Xây dựng phần lịch sử kiểm nghiệm cho hồ sơ GS1 mô phỏng. */
    private List<Gs1Inspection> buildGs1Inspections(Shipment shipment) {
        List<InspectionRequest> requests = loadInspectionRequests(shipment);
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<Gs1Inspection> inspections = new ArrayList<>();
        for (InspectionRequest request : requests) {
            if (request == null) {
                continue;
            }
            Map<UUID, InspectionCriterionResult> resultByCriterionId = loadResultsByCriterionId(request);
            List<Gs1InspectionCriterion> criteria = new ArrayList<>();
            if (request.getCriteria() != null) {
                for (InspectionCriterion criterion : request.getCriteria()) {
                    if (criterion == null) {
                        continue;
                    }
                    InspectionCriterionResult result = criterion.getId() != null
                        ? resultByCriterionId.get(criterion.getId())
                        : null;
                    criteria.add(Gs1InspectionCriterion.builder()
                        .criterionCode(criterion.getCriterionCode())
                        .criterionName(criterion.getCriterionName())
                        .standardName(criterion.getStandard() != null
                            ? criterion.getStandard().getName()
                            : null)
                        .passed(result != null ? result.getPassed() : null)
                        .resultDate(result != null ? result.getResultDate() : null)
                        .expiryDate(result != null ? result.getExpiryDate() : null)
                        .build());
                }
            }
            inspections.add(Gs1Inspection.builder()
                .requestId(request.getId())
                .inspectionUnit(request.getInspectionUnit())
                .sampleSentDate(request.getSampleSentDate())
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .criteria(criteria.isEmpty() ? null : criteria)
                .build());
        }
        return inspections;
    }

    /** Chuyển lịch sử kiểm nghiệm thành các dòng dữ liệu cho bảng PDF. */
    private List<String[]> toInspectionPdfRows(List<Gs1Inspection> inspections) {
        List<String[]> rows = new ArrayList<>();
        if (inspections == null || inspections.isEmpty()) {
            return rows;
        }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Gs1Inspection inspection : inspections) {
            String sampleSentDate = inspection.getSampleSentDate() != null
                ? inspection.getSampleSentDate().format(dateFormatter) : "N/A";
            String inspectionUnit = inspection.getInspectionUnit() != null
                ? inspection.getInspectionUnit() : "N/A";
            if (inspection.getCriteria() == null || inspection.getCriteria().isEmpty()) {
                rows.add(new String[] { sampleSentDate, inspectionUnit,
                    "Yêu cầu kiểm nghiệm chưa có chỉ tiêu.", "N/A", "N/A", "N/A" });
                continue;
            }
            for (Gs1InspectionCriterion criterion : inspection.getCriteria()) {
                if (criterion == null) {
                    continue;
                }
                String criterionLabel = criterion.getCriterionName() != null
                    ? criterion.getCriterionName()
                    : (criterion.getCriterionCode() != null ? criterion.getCriterionCode() : "N/A");
                if (criterion.getStandardName() != null) {
                    criterionLabel = criterionLabel + " (" + criterion.getStandardName() + ")";
                }
                String outcome = criterion.getPassed() == null ? "Chưa có kết quả"
                    : (Boolean.TRUE.equals(criterion.getPassed()) ? "Đạt" : "Không đạt");
                rows.add(new String[] {
                    sampleSentDate,
                    inspectionUnit,
                    criterionLabel,
                    outcome,
                    criterion.getResultDate() != null
                        ? criterion.getResultDate().format(dateFormatter) : "N/A",
                    criterion.getExpiryDate() != null
                        ? criterion.getExpiryDate().format(dateFormatter) : "N/A"
                });
            }
        }
        return rows;
    }

    /** Thêm ô dữ liệu vào bảng PDF. */
    private void addTableCell(
        PdfPTable table,
        String text,
        Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    /** Thêm ô tiêu đề vào bảng PDF. */
    private void addTableHeaderCell(
        PdfPTable table,
        String text,
        Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBackgroundColor(new Color(240, 240, 240));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    /** Định dạng trạng thái lô hàng. */
    private String formatShipmentStatus(vn.nguongocso.trace.enums.ShipmentStatus status) {
        String res = ExportDisplayFormatter.formatShipmentStatus(status);
        return res != null ? res : "N/A";
    }

    /** Định dạng loại hoạt động canh tác. */
    private String formatFarmActivityType(vn.nguongocso.farm.enums.FarmActivityType type) {
        String res = ExportDisplayFormatter.formatFarmActivityType(type);
        return res != null ? res : "N/A";
    }

    /** Định dạng dữ liệu sự kiện cho PDF. */
    private String formatEventDataForPdf(String rawJson) {
        String res = ExportDisplayFormatter.formatEventData(rawJson, "\n");
        return (res != null && !res.isBlank()) ? res : "N/A";
    }

    /** Vẽ nội dung PDF hồ sơ truy xuất. */
    private void renderShipmentDossierPdf(
        Document document,
        Shipment shipment,
        Font titleFont,
        Font headerFont,
        Font boldFont,
        Font normalFont) throws Exception {

        renderShipmentDossierPdf(document, shipment, null, null, titleFont, headerFont, boldFont, normalFont);
    }

    /** Vẽ nội dung PDF hồ sơ truy xuất kèm mẫu cấu hình. */
    private void renderShipmentDossierPdf(
        Document document,
        Shipment shipment,
        ProfileTemplate template,
        Set<String> selectedFieldKeys,
        Font titleFont,
        Font headerFont,
        Font boldFont,
        Font normalFont) throws Exception {

        if (shipment == null) {
            shipment = buildMockShipmentForPreview();
        }

        Paragraph title = new Paragraph("HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6);
        document.add(title);

        if (template != null) {
            String tplText = "Mẫu hồ sơ áp dụng: " + template.getName()
                + (template.getPartnerName() != null && !template.getPartnerName().isBlank()
                ? " (Đối tác: " + template.getPartnerName() + ")" : "");
            Paragraph tplPara = new Paragraph(tplText, boldFont);
            tplPara.setAlignment(Element.ALIGN_CENTER);
            tplPara.setSpacingAfter(4);
            document.add(tplPara);
        }

        Paragraph subtitle = new Paragraph("Mã định danh lô hàng: " + (shipment != null && shipment.getId() != null ? shipment.getId().toString() : "SHIP-MOCK-2026-DEMO"), normalFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(10);
        document.add(subtitle);

        document.add(new Paragraph(" "));

        ProductionLot lot = shipment != null ? shipment.getProductionLot() : null;
        Organization org = shipment != null && shipment.getOrganization() != null
            ? shipment.getOrganization()
            : (lot != null ? lot.getOrganization() : null);
        FarmArea farmArea = lot != null ? lot.getFarmArea() : null;

        boolean hasLotInfo = selectedFieldKeys == null || selectedFieldKeys.stream().anyMatch(k -> k.startsWith("productionLot.") || k.startsWith("organization.") || k.startsWith("farmArea."));
        if (hasLotInfo) {
            document.add(new Paragraph("I. THÔNG TIN ĐƠN VỊ & LÔ SẢN XUẤT", headerFont));
            document.add(new Paragraph(" "));
            PdfPTable lotTable = new PdfPTable(2);
            lotTable.setWidthPercentage(100);
            lotTable.setSpacingAfter(15);

            if (selectedFieldKeys == null || selectedFieldKeys.contains("organization.name")) {
                addTableCell(lotTable, "Đơn vị sản xuất (HTX):", boldFont);
                addTableCell(lotTable, (org != null && org.getName() != null) ? org.getName() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("organization.code")) {
                addTableCell(lotTable, "Mã định danh HTX:", boldFont);
                addTableCell(lotTable, (org != null && org.getCode() != null) ? org.getCode() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("organization.type")) {
                addTableCell(lotTable, "Loại hình tổ chức:", boldFont);
                addTableCell(lotTable, (org != null && org.getType() != null) ? ExportDisplayFormatter.formatOrganizationType(org.getType()) : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("organization.status")) {
                addTableCell(lotTable, "Trạng thái tổ chức:", boldFont);
                addTableCell(lotTable, (org != null && org.getStatus() != null) ? ExportDisplayFormatter.formatOrganizationStatus(org.getStatus()) : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("organization.address")) {
                addTableCell(lotTable, "Địa chỉ trụ sở:", boldFont);
                addTableCell(lotTable, (org != null && org.getAddress() != null) ? org.getAddress() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("organization.province")) {
                addTableCell(lotTable, "Tỉnh / Thành phố:", boldFont);
                addTableCell(lotTable, (org != null && org.getProvince() != null && org.getProvince().getName() != null) ? org.getProvince().getName() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("organization.phone")) {
                addTableCell(lotTable, "Số điện thoại liên hệ:", boldFont);
                addTableCell(lotTable, (org != null && org.getPhone() != null) ? org.getPhone() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("organization.email")) {
                addTableCell(lotTable, "Email liên hệ:", boldFont);
                addTableCell(lotTable, (org != null && org.getEmail() != null) ? org.getEmail() : "N/A", normalFont);
            }

            if (selectedFieldKeys == null || selectedFieldKeys.contains("farmArea.name")) {
                addTableCell(lotTable, "Vùng chuyên canh / Vùng trồng:", boldFont);
                addTableCell(lotTable, (farmArea != null && farmArea.getName() != null) ? farmArea.getName() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("farmArea.location")) {
                addTableCell(lotTable, "Tọa độ địa lý vùng trồng:", boldFont);
                String locStr = (farmArea != null && farmArea.getLocation() != null)
                    ? farmArea.getLocation().getY() + ", " + farmArea.getLocation().getX() : "N/A";
                addTableCell(lotTable, locStr, normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("farmArea.area")) {
                addTableCell(lotTable, "Diện tích canh tác:", boldFont);
                String areaStr = (farmArea != null && farmArea.getArea() != null)
                    ? farmArea.getArea() + " " + (farmArea.getAreaUnit() != null ? ExportDisplayFormatter.formatAreaUnit(farmArea.getAreaUnit()) : "ha")
                    : "N/A";
                addTableCell(lotTable, areaStr, normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("farmArea.cropType")) {
                addTableCell(lotTable, "Chủng loại cây trồng:", boldFont);
                addTableCell(lotTable, (farmArea != null && farmArea.getCropType() != null && farmArea.getCropType().getName() != null)
                    ? farmArea.getCropType().getName() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("farmArea.isActive")) {
                addTableCell(lotTable, "Trạng thái vùng trồng:", boldFont);
                addTableCell(lotTable, farmArea != null ? (Boolean.TRUE.equals(farmArea.getIsActive()) ? "Đang hoạt động" : "Tạm ngưng") : "N/A", normalFont);
            }

            if (selectedFieldKeys == null || selectedFieldKeys.contains("productionLot.name")) {
                addTableCell(lotTable, "Tên lô sản xuất:", boldFont);
                addTableCell(lotTable, lot != null && lot.getName() != null ? lot.getName() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("productionLot.productCategory")) {
                addTableCell(lotTable, "Danh mục sản phẩm:", boldFont);
                addTableCell(lotTable, (lot != null && lot.getProductCategory() != null && lot.getProductCategory().getName() != null)
                    ? lot.getProductCategory().getName() : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("productionLot.plantingDate")) {
                addTableCell(lotTable, "Ngày xuống giống:", boldFont);
                addTableCell(lotTable,
                    (lot != null && lot.getPlantingDate() != null)
                        ? lot.getPlantingDate().toString()
                        : "N/A",
                    normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("productionLot.harvestDate")) {
                addTableCell(lotTable, "Ngày thu hoạch:", boldFont);
                addTableCell(lotTable,
                    (lot != null && lot.getHarvestDate() != null)
                        ? lot.getHarvestDate().toString()
                        : "N/A",
                    normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("productionLot.expectedQuantity")) {
                addTableCell(lotTable, "Sản lượng dự kiến:", boldFont);
                addTableCell(lotTable, (lot != null && lot.getExpectedQuantity() != null)
                    ? lot.getExpectedQuantity() + " " + (lot.getExpectedQuantityUnit() != null ? lot.getExpectedQuantityUnit() : "kg")
                    : "N/A", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("productionLot.expectedQuantityUnit")) {
                addTableCell(lotTable, "Đơn vị tính sản lượng:", boldFont);
                addTableCell(lotTable, (lot != null && lot.getExpectedQuantityUnit() != null) ? lot.getExpectedQuantityUnit() : "kg", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("productionLot.actualQuantity")) {
                addTableCell(lotTable, "Sản lượng thực tế:", boldFont);
                addTableCell(lotTable,
                    (lot != null && lot.getActualQuantity() != null)
                        ? lot.getActualQuantity() + " kg"
                        : "N/A",
                    normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("productionLot.status")) {
                addTableCell(lotTable, "Trạng thái lô sản xuất:", boldFont);
                addTableCell(lotTable, (lot != null && lot.getStatus() != null) ? ExportDisplayFormatter.formatProductionLotStatus(lot.getStatus()) : "N/A", normalFont);
            }

            if (lotTable.getRows().size() > 0) {
                document.add(lotTable);
            }
        }

        boolean hasShipmentInfo = selectedFieldKeys == null || selectedFieldKeys.stream().anyMatch(k -> k.startsWith("shipment."));
        if (hasShipmentInfo) {
            document.add(new Paragraph("II. THÔNG TIN LÔ HÀNG", headerFont));
            document.add(new Paragraph(" "));
            PdfPTable shipmentTable = new PdfPTable(2);
            shipmentTable.setWidthPercentage(100);
            shipmentTable.setSpacingAfter(15);

            if (selectedFieldKeys == null || selectedFieldKeys.contains("shipment.name")) {
                addTableCell(shipmentTable, "Tên lô hàng vận chuyển:", boldFont);
                addTableCell(shipmentTable, (shipment != null && shipment.getName() != null) ? shipment.getName() : "Lô hàng mẫu kiểm thử", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("shipment.totalQuantity")) {
                addTableCell(shipmentTable, "Số lượng lô hàng:", boldFont);
                addTableCell(shipmentTable, (shipment != null ? shipment.getTotalQuantity() : 2000) + " sản phẩm", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("shipment.packagingInfo")) {
                addTableCell(shipmentTable, "Thông tin đóng gói:", boldFont);
                addTableCell(shipmentTable, (shipment != null && shipment.getPackagingInfo() != null) ? shipment.getPackagingInfo() : "Thùng carton 10kg, dán tem QR GS1",
                    normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("shipment.status")) {
                addTableCell(shipmentTable, "Trạng thái vận hành:", boldFont);
                addTableCell(shipmentTable, (shipment != null && shipment.getStatus() != null) ? formatShipmentStatus(shipment.getStatus()) : "Đã kích hoạt", normalFont);
            }
            if (selectedFieldKeys == null || selectedFieldKeys.contains("shipment.createdAt")) {
                addTableCell(shipmentTable, "Thời điểm tạo lô hàng:", boldFont);
                String createdStr = (shipment != null && shipment.getCreatedAt() != null)
                    ? shipment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                addTableCell(shipmentTable, createdStr, normalFont);
            }

            if (shipmentTable.getRows().size() > 0) {
                document.add(shipmentTable);
            }
        }

        boolean hasCertifications = selectedFieldKeys == null || selectedFieldKeys.stream().anyMatch(k -> k.startsWith("certification."));
        if (hasCertifications) {
            document.add(new Paragraph("III. CHỨNG NHẬN TIÊU CHUẨN", headerFont));
            document.add(new Paragraph(" "));

            boolean colName = selectedFieldKeys == null || selectedFieldKeys.contains("certification.name");
            boolean colStd = selectedFieldKeys == null || selectedFieldKeys.contains("certification.standardName");
            boolean colCode = selectedFieldKeys == null || selectedFieldKeys.contains("certification.certificationCode");
            boolean colIssue = selectedFieldKeys == null || selectedFieldKeys.contains("certification.issueDate");
            boolean colExpiry = selectedFieldKeys == null || selectedFieldKeys.contains("certification.expiryDate");
            boolean colCertifier = selectedFieldKeys == null || selectedFieldKeys.contains("certification.certifier");

            List<String> headers = new ArrayList<>();
            List<Float> widths = new ArrayList<>();
            if (colName) {
                headers.add("Tên chứng nhận");
                widths.add(25f);
            }
            if (colStd) {
                headers.add("Tiêu chuẩn");
                widths.add(15f);
            }
            if (colCode) {
                headers.add("Số hiệu");
                widths.add(15f);
            }
            if (colIssue) {
                headers.add("Ngày cấp");
                widths.add(15f);
            }
            if (colExpiry) {
                headers.add("Hạn hiệu lực");
                widths.add(15f);
            }
            if (colCertifier) {
                headers.add("Tổ chức chứng nhận");
                widths.add(15f);
            }

            if (!headers.isEmpty()) {
                PdfPTable certTable = new PdfPTable(headers.size());
                certTable.setWidthPercentage(100);
                float[] widthArr = new float[widths.size()];
                for (int i = 0; i < widths.size(); i++) {
                    widthArr[i] = widths.get(i);
                }
                certTable.setWidths(widthArr);
                certTable.setSpacingAfter(15);

                for (String h : headers) {
                    addTableHeaderCell(certTable, h, boldFont);
                }

                List<ProductionLotCertification> certs = (lot != null && lot.getId() != null)
                    ? productionLotCertificationRepository.findByProductionLotIdIn(List.of(lot.getId()))
                    : Collections.emptyList();

                if (certs != null && !certs.isEmpty()) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    for (ProductionLotCertification plc : certs) {
                        var c = plc.getCertification();
                        if (c == null) {
                            continue;
                        }
                        if (colName) {
                            addTableCell(certTable, c.getName() != null ? c.getName() : "N/A", normalFont);
                        }
                        if (colStd) {
                            addTableCell(certTable, (c.getStandard() != null && c.getStandard().getName() != null) ? c.getStandard().getName() : "N/A", normalFont);
                        }
                        if (colCode) {
                            addTableCell(certTable, c.getCode() != null ? c.getCode() : "N/A", normalFont);
                        }
                        if (colIssue) {
                            addTableCell(certTable, c.getIssueDate() != null ? c.getIssueDate().format(dtf) : "N/A", normalFont);
                        }
                        if (colExpiry) {
                            addTableCell(certTable, c.getExpiryDate() != null ? c.getExpiryDate().format(dtf) : "N/A", normalFont);
                        }
                        if (colCertifier) {
                            addTableCell(certTable, c.getIssuedBy() != null ? c.getIssuedBy() : "N/A", normalFont);
                        }
                    }
                } else if (shipment != null && shipment.getId() == null) {
                    if (colName) addTableCell(certTable, "Chứng nhận tiêu chuẩn VietGAP Trồng trọt", normalFont);
                    if (colStd) addTableCell(certTable, "VietGAP", normalFont);
                    if (colCode) addTableCell(certTable, "VG-2026-LD-0018", normalFont);
                    if (colIssue) addTableCell(certTable, "2026-01-10", normalFont);
                    if (colExpiry) addTableCell(certTable, "2028-01-10", normalFont);
                    if (colCertifier) addTableCell(certTable, "Trung tâm Chứng nhận Phù hợp Quacert", normalFont);

                    if (colName) addTableCell(certTable, "Chứng nhận Chuỗi Cung ứng Thực phẩm An toàn", normalFont);
                    if (colStd) addTableCell(certTable, "Chuỗi ATTP", normalFont);
                    if (colCode) addTableCell(certTable, "ATTP-LD-2026-089", normalFont);
                    if (colIssue) addTableCell(certTable, "2026-02-15", normalFont);
                    if (colExpiry) addTableCell(certTable, "2029-02-15", normalFont);
                    if (colCertifier) addTableCell(certTable, "Chi cục Trồng trọt & BVTV Lâm Đồng", normalFont);
                } else {
                    PdfPCell emptyCell = new PdfPCell(new Phrase("Chưa có chứng nhận tiêu chuẩn cho lô sản xuất này.", normalFont));
                    emptyCell.setColspan(headers.size());
                    emptyCell.setPadding(6);
                    certTable.addCell(emptyCell);
                }
                document.add(certTable);
            }
        }

        boolean hasFarmLogs = selectedFieldKeys == null || selectedFieldKeys.stream().anyMatch(k -> k.startsWith("farmLog."));
        if (hasFarmLogs) {
            document.add(new Paragraph("IV. LỊCH TRÌNH CANH TÁC & CHỨNG TỪ", headerFont));
            document.add(new Paragraph(" "));

            boolean colDate = selectedFieldKeys == null || selectedFieldKeys.contains("farmLog.executedDate");
            boolean colAct = selectedFieldKeys == null || selectedFieldKeys.contains("farmLog.activityType");
            boolean colMat = selectedFieldKeys == null || selectedFieldKeys.contains("farmLog.material") || selectedFieldKeys.contains("farmLog.quantity") || selectedFieldKeys.contains("farmLog.unit");
            boolean colNotes = selectedFieldKeys == null || selectedFieldKeys.contains("farmLog.notes");
            boolean colAtt = selectedFieldKeys == null || selectedFieldKeys.contains("farmLog.attachments");

            List<String> headers = new ArrayList<>();
            List<Float> widths = new ArrayList<>();
            if (colDate) {
                headers.add("Ngày thực hiện");
                widths.add(15f);
            }
            if (colAct) {
                headers.add("Hoạt động");
                widths.add(20f);
            }
            if (colMat) {
                headers.add("Vật tư / Số lượng");
                widths.add(20f);
            }
            if (colNotes) {
                headers.add("Ghi chú");
                widths.add(25f);
            }
            if (colAtt) {
                headers.add("Chứng từ đính kèm");
                widths.add(20f);
            }

            if (!headers.isEmpty()) {
                PdfPTable logTable = new PdfPTable(headers.size());
                logTable.setWidthPercentage(100);
                float[] widthArr = new float[widths.size()];
                for (int i = 0; i < widths.size(); i++) {
                    widthArr[i] = widths.get(i);
                }
                logTable.setWidths(widthArr);
                logTable.setSpacingAfter(15);

                for (String h : headers) {
                    addTableHeaderCell(logTable, h, boldFont);
                }

                List<FarmLog> logs = (lot != null && lot.getId() != null)
                    ? farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lot.getId())
                    : Collections.emptyList();
                if (logs != null && !logs.isEmpty()) {
                    for (FarmLog logItem : logs) {
                        if (logItem == null) {
                            continue;
                        }
                        if (colDate) {
                            addTableCell(logTable, logItem.getExecutedDate() != null ? logItem.getExecutedDate().toString() : "N/A", normalFont);
                        }
                        if (colAct) {
                            addTableCell(logTable, logItem.getActivityType() != null ? formatFarmActivityType(logItem.getActivityType()) : "N/A", normalFont);
                        }
                        if (colMat) {
                            String materialInfo = (logItem.getMaterial() != null ? logItem.getMaterial() : "") +
                                (logItem.getQuantity() != null ? " (" + logItem.getQuantity() + " " + (logItem.getUnit() != null ? logItem.getUnit() : "") + ")"
                                    : "");
                            addTableCell(logTable, materialInfo.trim().isEmpty() ? "Không có" : materialInfo.trim(), normalFont);
                        }
                        if (colNotes) {
                            addTableCell(logTable, logItem.getNotes() != null ? logItem.getNotes() : "", normalFont);
                        }
                        if (colAtt) {
                            List<FarmLogAttachment> attachments = logItem.getId() != null
                                ? farmLogAttachmentRepository.findByFarmLogId(logItem.getId())
                                : Collections.emptyList();
                            StringBuilder filesStr = new StringBuilder();
                            if (attachments != null) {
                                for (FarmLogAttachment att : attachments) {
                                    if (att != null && att.getFileName() != null) {
                                        if (filesStr.length() > 0) {
                                            filesStr.append("\n");
                                        }
                                        filesStr.append(att.getFileName());
                                    }
                                }
                            }
                            addTableCell(logTable, filesStr.toString().isEmpty() ? "Không có" : filesStr.toString(), normalFont);
                        }
                    }
                } else if (shipment != null && shipment.getId() == null) {
                    if (colDate) addTableCell(logTable, "2026-06-15", normalFont);
                    if (colAct) addTableCell(logTable, "Gieo giống / Xuống giống", normalFont);
                    if (colMat) addTableCell(logTable, "Giống cà rốt F1 Kuroda (2.5 gói)", normalFont);
                    if (colNotes) addTableCell(logTable, "Gieo hạt vụ thu đông, độ ẩm đất 75%, xử lý vi sinh Trichoderma", normalFont);
                    if (colAtt) addTableCell(logTable, "BienBan_GieoGiong_2026.pdf", normalFont);

                    if (colDate) addTableCell(logTable, "2026-07-02", normalFont);
                    if (colAct) addTableCell(logTable, "Tưới tiêu", normalFont);
                    if (colMat) addTableCell(logTable, "Nước giếng khoan kiểm nghiệm (50 m3)", normalFont);
                    if (colNotes) addTableCell(logTable, "Tưới phun mưa tự động duy trì độ ẩm 70%, làm sạch cỏ luống", normalFont);
                    if (colAtt) addTableCell(logTable, "KetQua_NuocTuoi_2026.pdf", normalFont);

                    if (colDate) addTableCell(logTable, "2026-07-10", normalFont);
                    if (colAct) addTableCell(logTable, "Bón phân", normalFont);
                    if (colMat) addTableCell(logTable, "Phân trùn quế vi sinh (500 kg)", normalFont);
                    if (colNotes) addTableCell(logTable, "Bón thúc lần 1 giai đoạn cây phát triển thân lá và rễ củ", normalFont);
                    if (colAtt) addTableCell(logTable, "HoaDon_VatTu_TrunQue.pdf", normalFont);

                    if (colDate) addTableCell(logTable, "2026-07-28", normalFont);
                    if (colAct) addTableCell(logTable, "Phòng trừ sâu bệnh", normalFont);
                    if (colMat) addTableCell(logTable, "Chế phẩm Neem Oil thảo mộc (10 lít)", normalFont);
                    if (colNotes) addTableCell(logTable, "Phun phòng ngừa sâu tơ và rệp muội định kỳ theo quy trình VietGAP", normalFont);
                    if (colAtt) addTableCell(logTable, "NhatKy_ChePhamSinhHoc.pdf", normalFont);

                    if (colDate) addTableCell(logTable, "2026-08-15", normalFont);
                    if (colAct) addTableCell(logTable, "Bón phân", normalFont);
                    if (colMat) addTableCell(logTable, "Phân Kali hữu cơ khoáng (300 kg)", normalFont);
                    if (colNotes) addTableCell(logTable, "Bón thúc lần 2 nuôi củ, tăng tích lũy đường và màu sắc tự nhiên", normalFont);
                    if (colAtt) addTableCell(logTable, "PhieuXuatKho_PhanBon.pdf", normalFont);

                    if (colDate) addTableCell(logTable, "2026-08-30", normalFont);
                    if (colAct) addTableCell(logTable, "Chăm sóc", normalFont);
                    if (colMat) addTableCell(logTable, "Không sử dụng vật tư (Cách ly 15 ngày)", normalFont);
                    if (colNotes) addTableCell(logTable, "Kiểm tra độ chín đồng đều và đảm bảo cách ly an toàn trước thu hoạch", normalFont);
                    if (colAtt) addTableCell(logTable, "BienBan_KiemTraCachLy.pdf", normalFont);
                } else {
                    PdfPCell emptyLogCell = new PdfPCell(new Phrase("Chưa có nhật ký canh tác cho lô này.", normalFont));
                    emptyLogCell.setColspan(headers.size());
                    emptyLogCell.setPadding(6);
                    logTable.addCell(emptyLogCell);
                }
                document.add(logTable);
            }
        }

        boolean hasInspections = selectedFieldKeys == null || selectedFieldKeys.stream().anyMatch(k -> k.startsWith("inspection."));
        if (hasInspections) {
            document.add(new Paragraph("V. LỊCH SỬ KIỂM NGHIỆM", headerFont));
            document.add(new Paragraph(" "));

            boolean colDate = selectedFieldKeys == null || selectedFieldKeys.contains("inspection.sampleSentDate");
            boolean colUnit = selectedFieldKeys == null || selectedFieldKeys.contains("inspection.inspectionUnit");
            boolean colCrit = selectedFieldKeys == null || selectedFieldKeys.contains("inspection.criterionName");
            boolean colPass = selectedFieldKeys == null || selectedFieldKeys.contains("inspection.passed");
            boolean colResDate = selectedFieldKeys == null || selectedFieldKeys.contains("inspection.resultDate");
            boolean colExpDate = selectedFieldKeys == null || selectedFieldKeys.contains("inspection.expiryDate");

            List<String> headers = new ArrayList<>();
            List<Float> widths = new ArrayList<>();
            if (colDate) {
                headers.add("Ngày gửi mẫu");
                widths.add(15f);
            }
            if (colUnit) {
                headers.add("Đơn vị kiểm nghiệm");
                widths.add(20f);
            }
            if (colCrit) {
                headers.add("Chỉ tiêu / Tiêu chuẩn");
                widths.add(25f);
            }
            if (colPass) {
                headers.add("Kết quả");
                widths.add(12f);
            }
            if (colResDate) {
                headers.add("Ngày cấp kết quả");
                widths.add(14f);
            }
            if (colExpDate) {
                headers.add("Hạn hiệu lực");
                widths.add(14f);
            }

            if (!headers.isEmpty()) {
                PdfPTable inspectionTable = new PdfPTable(headers.size());
                inspectionTable.setWidthPercentage(100);
                float[] widthArr = new float[widths.size()];
                for (int i = 0; i < widths.size(); i++) {
                    widthArr[i] = widths.get(i);
                }
                inspectionTable.setWidths(widthArr);
                inspectionTable.setSpacingAfter(15);

                for (String h : headers) {
                    addTableHeaderCell(inspectionTable, h, boldFont);
                }

                List<Gs1Inspection> inspections = buildGs1Inspections(shipment);
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                boolean hasAnyData = false;

                if (inspections != null && !inspections.isEmpty()) {
                    for (Gs1Inspection inspection : inspections) {
                        String sampleSentDate = inspection.getSampleSentDate() != null
                            ? inspection.getSampleSentDate().format(dateFormatter) : "N/A";
                        String inspectionUnit = inspection.getInspectionUnit() != null
                            ? inspection.getInspectionUnit() : "N/A";

                        if (inspection.getCriteria() == null || inspection.getCriteria().isEmpty()) {
                            hasAnyData = true;
                            if (colDate) {
                                addTableCell(inspectionTable, sampleSentDate, normalFont);
                            }
                            if (colUnit) {
                                addTableCell(inspectionTable, inspectionUnit, normalFont);
                            }
                            if (colCrit) {
                                addTableCell(inspectionTable, "Yêu cầu kiểm nghiệm chưa có chỉ tiêu.", normalFont);
                            }
                            if (colPass) {
                                addTableCell(inspectionTable, inspection.getStatus() != null ? inspection.getStatus() : "N/A", normalFont);
                            }
                            if (colResDate) {
                                addTableCell(inspectionTable, "N/A", normalFont);
                            }
                            if (colExpDate) {
                                addTableCell(inspectionTable, "N/A", normalFont);
                            }
                        } else {
                            for (Gs1InspectionCriterion criterion : inspection.getCriteria()) {
                                if (criterion == null) {
                                    continue;
                                }
                                hasAnyData = true;
                                String criterionLabel = criterion.getCriterionName() != null
                                    ? criterion.getCriterionName()
                                    : (criterion.getCriterionCode() != null ? criterion.getCriterionCode() : "N/A");
                                if (criterion.getStandardName() != null) {
                                    criterionLabel = criterionLabel + " (" + criterion.getStandardName() + ")";
                                }
                                String outcome = criterion.getPassed() == null ? "Chưa có kết quả"
                                    : (Boolean.TRUE.equals(criterion.getPassed()) ? "Đạt" : "Không đạt");
                                String resultDate = criterion.getResultDate() != null ? criterion.getResultDate().format(dateFormatter) : "N/A";
                                String expiryDate = criterion.getExpiryDate() != null ? criterion.getExpiryDate().format(dateFormatter) : "N/A";

                                if (colDate) {
                                    addTableCell(inspectionTable, sampleSentDate, normalFont);
                                }
                                if (colUnit) {
                                    addTableCell(inspectionTable, inspectionUnit, normalFont);
                                }
                                if (colCrit) {
                                    addTableCell(inspectionTable, criterionLabel, normalFont);
                                }
                                if (colPass) {
                                    addTableCell(inspectionTable, outcome, normalFont);
                                }
                                if (colResDate) {
                                    addTableCell(inspectionTable, resultDate, normalFont);
                                }
                                if (colExpDate) {
                                    addTableCell(inspectionTable, expiryDate, normalFont);
                                }
                            }
                        }
                    }
                }

                if (!hasAnyData) {
                    if (shipment != null && shipment.getId() == null) {
                        if (colDate) addTableCell(inspectionTable, "2026-09-05", normalFont);
                        if (colUnit) addTableCell(inspectionTable, "Trung tâm Phân tích Quatest 3", normalFont);
                        if (colCrit) addTableCell(inspectionTable, "Kim loại nặng trong đất & nước (Pb, Cd)", normalFont);
                        if (colPass) addTableCell(inspectionTable, "Đạt", normalFont);
                        if (colResDate) addTableCell(inspectionTable, "2026-09-06", normalFont);
                        if (colExpDate) addTableCell(inspectionTable, "2027-03-06", normalFont);

                        if (colDate) addTableCell(inspectionTable, "2026-09-08", normalFont);
                        if (colUnit) addTableCell(inspectionTable, "Trung tâm Phân tích Quatest 3", normalFont);
                        if (colCrit) addTableCell(inspectionTable, "Dư lượng Nitrat (NO3-) trên nông sản", normalFont);
                        if (colPass) addTableCell(inspectionTable, "Đạt", normalFont);
                        if (colResDate) addTableCell(inspectionTable, "2026-09-09", normalFont);
                        if (colExpDate) addTableCell(inspectionTable, "2027-03-09", normalFont);

                        if (colDate) addTableCell(inspectionTable, "2026-09-08", normalFont);
                        if (colUnit) addTableCell(inspectionTable, "Trung tâm Phân tích Quatest 3", normalFont);
                        if (colCrit) addTableCell(inspectionTable, "Dư lượng thuốc BVTV (Cypermethrin, Chlorpyrifos)", normalFont);
                        if (colPass) addTableCell(inspectionTable, "Đạt (Không phát hiện)", normalFont);
                        if (colResDate) addTableCell(inspectionTable, "2026-09-09", normalFont);
                        if (colExpDate) addTableCell(inspectionTable, "2027-03-09", normalFont);

                        if (colDate) addTableCell(inspectionTable, "2026-09-08", normalFont);
                        if (colUnit) addTableCell(inspectionTable, "Trung tâm Phân tích Quatest 3", normalFont);
                        if (colCrit) addTableCell(inspectionTable, "Vi sinh vật gây hại (Salmonella, E.coli)", normalFont);
                        if (colPass) addTableCell(inspectionTable, "Đạt", normalFont);
                        if (colResDate) addTableCell(inspectionTable, "2026-09-09", normalFont);
                        if (colExpDate) addTableCell(inspectionTable, "2027-03-09", normalFont);
                    } else {
                        PdfPCell emptyCell = new PdfPCell(new Phrase("Chưa có dữ liệu kiểm nghiệm cho lô sản xuất này.", normalFont));
                        emptyCell.setColspan(headers.size());
                        emptyCell.setPadding(6);
                        inspectionTable.addCell(emptyCell);
                    }
                }
                document.add(inspectionTable);
            }
        }

        boolean hasTimeline = selectedFieldKeys == null || selectedFieldKeys.stream().anyMatch(k -> k.startsWith("chainEvent."));
        if (hasTimeline) {
            document.add(new Paragraph("VI. DÒNG SỰ KIỆN CHUỖI CUNG ỨNG (TIMELINE)", headerFont));
            document.add(new Paragraph(" "));

            boolean colTime = selectedFieldKeys == null || selectedFieldKeys.contains("chainEvent.recordedAt");
            boolean colType = selectedFieldKeys == null || selectedFieldKeys.contains("chainEvent.eventType");
            boolean colLoc = selectedFieldKeys == null || selectedFieldKeys.contains("chainEvent.location");
            boolean colData = selectedFieldKeys == null || selectedFieldKeys.contains("chainEvent.eventData");
            boolean colUser = selectedFieldKeys == null || selectedFieldKeys.contains("chainEvent.recordedBy");

            List<String> headers = new ArrayList<>();
            List<Float> widths = new ArrayList<>();
            if (colTime) {
                headers.add("Thời điểm ghi nhận");
                widths.add(18f);
            }
            if (colType) {
                headers.add("Loại sự kiện");
                widths.add(18f);
            }
            if (colLoc) {
                headers.add("Tọa độ địa điểm");
                widths.add(16f);
            }
            if (colData) {
                headers.add("Chi tiết dữ liệu");
                widths.add(28f);
            }
            if (colUser) {
                headers.add("Người ghi nhận");
                widths.add(20f);
            }

            if (!headers.isEmpty()) {
                PdfPTable eventTable = new PdfPTable(headers.size());
                eventTable.setWidthPercentage(100);
                float[] widthArr = new float[widths.size()];
                for (int i = 0; i < widths.size(); i++) {
                    widthArr[i] = widths.get(i);
                }
                eventTable.setWidths(widthArr);
                eventTable.setSpacingAfter(15);

                for (String h : headers) {
                    addTableHeaderCell(eventTable, h, boldFont);
                }

                List<ChainEvent> events = getShipmentEventsWithLineage(shipment);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                if (events != null && !events.isEmpty()) {
                    for (ChainEvent ev : events) {
                        if (ev == null) {
                            continue;
                        }
                        if (colTime) {
                            addTableCell(eventTable, ev.getRecordedAt() != null ? ev.getRecordedAt().format(formatter) : "N/A", normalFont);
                        }
                        if (colType) {
                            String eventTypeStr = ev.getEventType() != null ? getEventTypeLabel(ev.getEventType()) : "N/A";
                            addTableCell(eventTable, eventTypeStr + (ev.isCorrection() ? " (Đã điều chỉnh)" : ""), normalFont);
                        }
                        if (colLoc) {
                            String locStr = ev.getLocation() != null ? (ev.getLocation().getY() + ", " + ev.getLocation().getX()) : "N/A";
                            addTableCell(eventTable, locStr, normalFont);
                        }
                        if (colData) {
                            addTableCell(eventTable, formatEventDataForPdf(ev.getEventData()), normalFont);
                        }
                        if (colUser) {
                            String recordedByName = "Hệ thống";
                            if (ev.getRecordedBy() != null) {
                                recordedByName = ev.getRecordedBy().getFullName() != null
                                    ? ev.getRecordedBy().getFullName()
                                    : (ev.getRecordedBy().getUserName() != null ? ev.getRecordedBy().getUserName() : "Hệ thống");
                            }
                            addTableCell(eventTable, recordedByName, normalFont);
                        }
                    }
                } else if (shipment != null && shipment.getId() == null) {
                    if (colTime) addTableCell(eventTable, "2026-09-10 07:30:00", normalFont);
                    if (colType) addTableCell(eventTable, "Thu hoạch", normalFont);
                    if (colLoc) addTableCell(eventTable, "11.8345, 108.4567", normalFont);
                    if (colData) addTableCell(eventTable, "Sản lượng: 2500 kg; Thu hoạch thủ công sáng sớm, sơ tuyển củ loại 1", normalFont);
                    if (colUser) addTableCell(eventTable, "Nguyễn Văn Quản Lý", normalFont);

                    if (colTime) addTableCell(eventTable, "2026-09-10 11:00:00", normalFont);
                    if (colType) addTableCell(eventTable, "Vận chuyển nội bộ", normalFont);
                    if (colLoc) addTableCell(eventTable, "11.8348, 108.4569", normalFont);
                    if (colData) addTableCell(eventTable, "Vận chuyển về trạm sơ chế bằng xe chuyên dụng 49C-123.45; Nhiệt độ: 20°C", normalFont);
                    if (colUser) addTableCell(eventTable, "Lê Văn Vận Chuyển", normalFont);

                    if (colTime) addTableCell(eventTable, "2026-09-10 14:30:00", normalFont);
                    if (colType) addTableCell(eventTable, "Sơ chế & Làm sạch", normalFont);
                    if (colLoc) addTableCell(eventTable, "11.8350, 108.4570", normalFont);
                    if (colData) addTableCell(eventTable, "Rửa sạch sục khí Ozone diệt khuẩn, làm ráo tự nhiên; Phân loại củ 16-20cm", normalFont);
                    if (colUser) addTableCell(eventTable, "Phạm Thị Sơ Chế", normalFont);

                    if (colTime) addTableCell(eventTable, "2026-09-11 09:00:00", normalFont);
                    if (colType) addTableCell(eventTable, "Kiểm tra chất lượng (QC)", normalFont);
                    if (colLoc) addTableCell(eventTable, "11.8350, 108.4570", normalFont);
                    if (colData) addTableCell(eventTable, "Kiểm tra cảm quan độ giòn tươi và test nhanh tồn dư nitrate; Đạt 100% chuẩn xuất hàng", normalFont);
                    if (colUser) addTableCell(eventTable, "Đỗ Văn Kiểm Phẩm", normalFont);

                    if (colTime) addTableCell(eventTable, "2026-09-12 10:00:00", normalFont);
                    if (colType) addTableCell(eventTable, "Đóng gói", normalFont);
                    if (colLoc) addTableCell(eventTable, "11.8350, 108.4570", normalFont);
                    if (colData) addTableCell(eventTable, "Số thùng: 200; Quy cách: Thùng carton chuyên dụng 10kg, bọc màng PE thoáng khí, dán tem QR GS1", normalFont);
                    if (colUser) addTableCell(eventTable, "Trần Thị Đóng Gói", normalFont);

                    if (colTime) addTableCell(eventTable, "2026-09-12 14:00:00", normalFont);
                    if (colType) addTableCell(eventTable, "Kích hoạt tem truy xuất", normalFont);
                    if (colLoc) addTableCell(eventTable, "11.8350, 108.4570", normalFont);
                    if (colData) addTableCell(eventTable, "Kích hoạt 200 mã định danh tem QR GS1 công khai trên Cổng thông tin Nguồn Gốc Số", normalFont);
                    if (colUser) addTableCell(eventTable, "Nguyễn Văn Quản Lý", normalFont);

                    if (colTime) addTableCell(eventTable, "2026-09-12 16:30:00", normalFont);
                    if (colType) addTableCell(eventTable, "Xuất kho vận chuyển", normalFont);
                    if (colLoc) addTableCell(eventTable, "11.8352, 108.4572", normalFont);
                    if (colData) addTableCell(eventTable, "Bàn giao xe container lạnh 49A-888.99 giao Đại siêu thị Go! Đà Lạt; Nhiệt độ thùng: 8°C - 12°C", normalFont);
                    if (colUser) addTableCell(eventTable, "Hoàng Văn Kho", normalFont);

                    if (colTime) addTableCell(eventTable, "2026-09-13 06:30:00", normalFont);
                    if (colType) addTableCell(eventTable, "Tiếp nhận & Phân phối", normalFont);
                    if (colLoc) addTableCell(eventTable, "11.9404, 108.4583", normalFont);
                    if (colData) addTableCell(eventTable, "Đại siêu thị Go! Đà Lạt nghiệm thu quét mã QR, xác nhận nhập kho 200 thùng và phân phối lên kệ", normalFont);
                    if (colUser) addTableCell(eventTable, "Quản lý Tiếp nhận Siêu thị", normalFont);
                } else {
                    PdfPCell emptyEventCell = new PdfPCell(new Phrase("Chưa ghi nhận sự kiện luân chuyển nào.", normalFont));
                    emptyEventCell.setColspan(headers.size());
                    emptyEventCell.setPadding(6);
                    eventTable.addCell(emptyEventCell);
                }
                document.add(eventTable);
            }
        }
    }

    /** Kiểm tra quyền truy cập hồ sơ truy xuất. */
    private void validateDossierAccess(
        Shipment shipment,
        CustomUserDetails currentUser) {

        String role = currentUser.getRoleCode();

        if ("VT-01".equals(role)) {
            return;
        }

        if ("VT-02".equals(role)) {
            UUID userOrgId = currentUser.getOrganizationId();
            UUID shipmentOrgId = shipment.getOrganization().getOrganizationId();
            if (!shipmentOrgId.equals(userOrgId)) {
                throw new AccessDeniedException("Từ chối thao tác: Bạn không có quyền truy cập lô hàng này.");
            }
            return;
        }

        if ("VT-04".equals(role)) {
            UUID userOrgId = currentUser.getOrganizationId();
            boolean isRecipient = shipment.getRecipientOrganization() != null
                && userOrgId != null
                && userOrgId.equals(shipment.getRecipientOrganization().getOrganizationId());

            boolean hasAcceptedHandover = userOrgId != null
                && shipmentHandoverRepository.existsByShipmentIdAndToOrganizationOrganizationIdAndStatus(
                    shipment.getId(), userOrgId, ShipmentHandoverStatus.ACCEPTED);

            boolean hasProcurement = userOrgId != null
                && chainEventRepository.existsByShipmentIdAndRecordedOrganizationIdAndEventType(
                    shipment.getId(), userOrgId, ChainEventType.PROCUREMENT);

            if (!isRecipient && !hasAcceptedHandover && !hasProcurement) {
                throw new AccessDeniedException("Từ chối thao tác: Lô hàng này không được giao cho doanh nghiệp của bạn.");
            }
            return;
        }

        throw new AccessDeniedException("Từ chối thao tác: Bạn không có quyền xem hoặc xuất hồ sơ cho lô hàng này.");
    }

    /** Lấy danh sách sự kiện kèm dòng họ lô hàng cha. */
    private List<ChainEvent> getShipmentEventsWithLineage(Shipment shipment) {
        if (shipment == null || shipment.getId() == null) {
            return Collections.emptyList();
        }
        List<ChainEvent> events = new ArrayList<>();
        if (shipment.getParentShipment() != null) {
            LocalDateTime splitAt = shipment.getSplitAt();
            chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(
                    shipment.getParentShipment().getId())
                .stream()
                .filter(event -> splitAt == null || event.getRecordedAt() == null
                    || !event.getRecordedAt().isAfter(splitAt))
                .forEach(events::add);
        }
        List<ChainEvent> shipmentEvents = chainEventRepository
            .findByShipment_IdOrderByRecordedAtAsc(shipment.getId());
        if (shipmentEvents != null) {
            events.addAll(shipmentEvents);
        }
        events.sort(Comparator.comparing(ChainEvent::getRecordedAt,
            Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    /** Ghi nhật ký xuất hồ sơ truy xuất. */
    private void logDossierExport(
        Shipment shipment,
        CustomUserDetails currentUser,
        String status,
        String ipAddress,
        Long fileSize) {

        logDossierExport(shipment, currentUser, status, ipAddress, fileSize, null);
    }

    /** Ghi nhật ký xuất hồ sơ truy xuất kèm thông tin mẫu áp dụng. */
    private void logDossierExport(
        Shipment shipment,
        CustomUserDetails currentUser,
        String status,
        String ipAddress,
        Long fileSize,
        UUID templateId) {

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
                .templateId(templateId)
                .build();

            exportHistoryRepository.save(history);
            log.info("Ghi log xuất hồ sơ thành công cho user: {}, status: {}", currentUser.getUsername(), status);
        } catch (Exception e) {
            log.error("Lỗi khi lưu lịch sử xuất hồ sơ truy xuất: {}", e.getMessage());
        }
    }

    /** Phát sự kiện ghi nhật ký hoạt động. */
    private void publishActivityLog(
        CustomUserDetails currentUser,
        String action,
        String description,
        String entityType,
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

    /** Kiểm tra điều kiện xuất hồ sơ hàng loạt cho danh sách lô hàng. */
    @Override
    @Transactional(readOnly = true)
    public BatchDossierCheckResponse checkBatchEligibility(
        BatchDossierCheckRequest request,
        CustomUserDetails currentUser) {

        if (request == null || request.getShipmentIds() == null || request.getShipmentIds().isEmpty()) {
            throw new BusinessException("Danh sách lô hàng không được để trống.");
        }

        List<BatchDossierCheckResponse.ShipmentEligibilityItem> eligibleList = new ArrayList<>();
        List<BatchDossierCheckResponse.ShipmentEligibilityItem> ineligibleList = new ArrayList<>();

        for (UUID shipmentId : request.getShipmentIds()) {
            if (shipmentId == null) {
                continue;
            }

            Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
            if (shipment == null) {
                ineligibleList.add(BatchDossierCheckResponse.ShipmentEligibilityItem.builder()
                    .shipmentId(shipmentId)
                    .shipmentName("Lô không tồn tại (" + shipmentId + ")")
                    .eligible(false)
                    .missingDocuments(List.of("Không tìm thấy lô hàng trên hệ thống"))
                    .build());
                continue;
            }

            UUID shipmentOrgId = shipment.getOrganization() != null
                ? shipment.getOrganization().getOrganizationId()
                : null;
            String shipmentOrgName = shipment.getOrganization() != null
                ? shipment.getOrganization().getName()
                : null;

            try {
                validateDossierAccess(shipment, currentUser);
            } catch (AccessDeniedException ex) {
                ineligibleList.add(BatchDossierCheckResponse.ShipmentEligibilityItem.builder()
                    .shipmentId(shipmentId)
                    .shipmentName(shipment.getName())
                    .eligible(false)
                    .missingDocuments(List.of("Lô ngoài phạm vi quản lý của tổ chức (QTN-01)"))
                    .organizationId(shipmentOrgId)
                    .organizationName(shipmentOrgName)
                    .build());
                continue;
            }

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
                        if (logItem == null) {
                            continue;
                        }
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

                if (!hasPlanting) {
                    missingDocs.add("Thiếu chứng từ gieo giống/xuống giống (PLANTING)");
                }
                if (!hasFertilizing) {
                    missingDocs.add("Thiếu chứng từ bón phân (FERTILIZING)");
                }
                if (!hasPesticide) {
                    missingDocs.add("Thiếu chứng từ phun thuốc/phòng trừ sâu bệnh (PESTICIDE)");
                }
                if (!hasHarvesting) {
                    missingDocs.add("Thiếu chứng từ thu hoạch (HARVESTING)");
                }
            }

            if (!missingDocs.isEmpty()) {
                ineligibleList.add(BatchDossierCheckResponse.ShipmentEligibilityItem.builder()
                    .shipmentId(shipmentId)
                    .shipmentName(shipment.getName())
                    .eligible(false)
                    .missingDocuments(missingDocs)
                    .organizationId(shipmentOrgId)
                    .organizationName(shipmentOrgName)
                    .build());
            } else {
                eligibleList.add(BatchDossierCheckResponse.ShipmentEligibilityItem.builder()
                    .shipmentId(shipmentId)
                    .shipmentName(shipment.getName())
                    .eligible(true)
                    .missingDocuments(Collections.emptyList())
                    .organizationId(shipmentOrgId)
                    .organizationName(shipmentOrgName)
                    .build());
            }
        }

        return BatchDossierCheckResponse.builder()
            .totalSelected(request.getShipmentIds().size())
            .totalEligible(eligibleList.size())
            .totalIneligible(ineligibleList.size())
            .eligibleShipments(eligibleList)
            .ineligibleShipments(ineligibleList)
            .build();
    }

    /** Xuất bộ hồ sơ truy xuất PDF hợp nhất cho nhiều lô hàng. */
    @Override
    @Transactional
    public byte[] exportBatchDossierPdf(
        BatchDossierExportRequest request,
        CustomUserDetails currentUser,
        String ipAddress) {

        if (request == null || request.getShipmentIds() == null || request.getShipmentIds().isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một lô hàng đủ điều kiện để xuất bộ hồ sơ.");
        }

        List<Shipment> eligibleShipments = new ArrayList<>();
        for (UUID shipmentId : request.getShipmentIds()) {
            Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
            if (shipment != null) {
                try {
                    validateDossierAccess(shipment, currentUser);
                    eligibleShipments.add(shipment);
                } catch (AccessDeniedException ex) {
                    log.warn("Bỏ qua lô {} do vi phạm quyền truy cập QTN-01 khi xuất hàng loạt", shipmentId);
                }
            }
        }

        if (eligibleShipments.isEmpty()) {
            throw new BusinessException("Không có lô hàng nào đủ điều kiện hoặc thuộc quyền truy cập để xuất bộ hồ sơ.");
        }

        ProfileTemplate batchTemplate = null;
        UUID userOrgId = currentUser != null ? currentUser.getOrganizationId() : null;

        Set<UUID> involvedOrgIds = new HashSet<>();
        if ("VT-04".equals(currentUser.getRoleCode())) {
            for (Shipment ship : eligibleShipments) {
                if (ship.getOrganization() != null) {
                    involvedOrgIds.add(ship.getOrganization().getOrganizationId());
                }
            }
        }

        UUID effectiveOrgIdForDefault = userOrgId;
        if ("VT-04".equals(currentUser.getRoleCode()) && !involvedOrgIds.isEmpty()) {
            effectiveOrgIdForDefault = eligibleShipments.get(0).getOrganization() != null
                ? eligibleShipments.get(0).getOrganization().getOrganizationId()
                : null;
        }

        if (request.getTemplateId() != null && profileTemplateRepository != null) {
            batchTemplate = profileTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));

            if ("VT-04".equals(currentUser.getRoleCode())) {
                if (batchTemplate.getOrganization() == null
                    || !involvedOrgIds.contains(batchTemplate.getOrganization().getOrganizationId())) {
                    throw new AccessDeniedException("Mẫu hồ sơ không thuộc tổ chức của bất kỳ lô hàng nào trong danh sách.");
                }
            } else {
                if (userOrgId != null && !batchTemplate.getOrganization().getOrganizationId().equals(userOrgId)) {
                    throw new AccessDeniedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
                }
            }
        } else if (effectiveOrgIdForDefault != null && profileTemplateRepository != null) {
            batchTemplate = profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(effectiveOrgIdForDefault)
                .orElse(null);
        }

        Set<String> batchSelectedFieldKeys = null;
        if (batchTemplate != null && batchTemplate.getFields() != null && !batchTemplate.getFields().isEmpty()) {
            batchSelectedFieldKeys = batchTemplate.getFields().stream()
                .map(ProfileTemplateField::getFieldKey)
                .collect(Collectors.toSet());
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = loadFont("fonts/Roboto-Bold.ttf", 16, Font.BOLD);
            Font headerFont = loadFont("fonts/Roboto-Bold.ttf", 12, Font.BOLD);
            Font boldFont = loadFont("fonts/Roboto-Bold.ttf", 10, Font.BOLD);
            Font normalFont = loadFont("fonts/Roboto-Regular.ttf", 10, Font.NORMAL);

            String batchTitle = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
                ? request.getTitle().trim()
                : "BỘ HỒ SƠ TRUY XUẤT NGUỒN GỐC NÔNG SẢN";

            Paragraph pTitle = new Paragraph(batchTitle.toUpperCase(), titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitle);

            document.add(new Paragraph(" "));

            Paragraph pSub = new Paragraph("Đơn vị xuất bộ hồ sơ: " + (currentUser != null ? currentUser.getFullName() : "N/A"), headerFont);
            document.add(pSub);

            Paragraph pDate = new Paragraph("Ngày tạo bộ hồ sơ: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), normalFont);
            document.add(pDate);

            Paragraph pCount = new Paragraph("Tổng số lô hàng xuất hồ sơ: " + eligibleShipments.size() + " lô", normalFont);
            document.add(pCount);

            if (batchTemplate != null) {
                String templateOrgName = batchTemplate.getOrganization() != null
                    ? batchTemplate.getOrganization().getName()
                    : "N/A";
                StringBuilder templateInfo = new StringBuilder("Mẫu hồ sơ áp dụng: ")
                    .append(batchTemplate.getName());
                if ("VT-04".equals(currentUser.getRoleCode())
                    && userOrgId != null
                    && batchTemplate.getOrganization() != null
                    && batchTemplate.getOrganization().getOrganizationId() != null
                    && !batchTemplate.getOrganization().getOrganizationId().equals(userOrgId)) {
                    templateInfo.append(" (thuộc HTX: ").append(templateOrgName).append(")");
                }
                Paragraph pTemplate = new Paragraph(templateInfo.toString(), normalFont);
                document.add(pTemplate);
            }

            if (request.getNote() != null && !request.getNote().trim().isEmpty()) {
                Paragraph pNote = new Paragraph("Ghi chú: " + request.getNote().trim(), normalFont);
                document.add(pNote);
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("DANH SÁCH CÁC LÔ HÀNG ĐỦ ĐIỀU KIỆN TRONG BỘ HỒ SƠ", headerFont));

            PdfPTable summaryTable = new PdfPTable(5);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[] {1f, 3f, 3f, 2f, 2f});
            summaryTable.setSpacingBefore(8f);
            summaryTable.setSpacingAfter(12f);

            addTableHeaderCell(summaryTable, "STT", headerFont);
            addTableHeaderCell(summaryTable, "Tên lô hàng", headerFont);
            addTableHeaderCell(summaryTable, "Mã dải tem / Lô sản xuất", headerFont);
            addTableHeaderCell(summaryTable, "Số lượng", headerFont);
            addTableHeaderCell(summaryTable, "Quy cách", headerFont);

            int stt = 1;
            for (Shipment ship : eligibleShipments) {
                summaryTable.addCell(new Phrase(String.valueOf(stt++), normalFont));
                summaryTable.addCell(new Phrase(ship.getName() != null ? ship.getName() : "", normalFont));
                summaryTable.addCell(new Phrase(ship.getProductionLot() != null ? ship.getProductionLot().getName() : "N/A", normalFont));
                String unitStr = (ship.getProductionLot() != null && ship.getProductionLot().getExpectedQuantityUnit() != null)
                    ? ship.getProductionLot().getExpectedQuantityUnit()
                    : "";
                summaryTable.addCell(new Phrase(ship.getTotalQuantity() + " " + unitStr, normalFont));
                summaryTable.addCell(new Phrase(ship.getPackagingInfo() != null ? ship.getPackagingInfo() : "—", normalFont));
            }
            document.add(summaryTable);

            for (Shipment ship : eligibleShipments) {
                document.newPage();
                renderShipmentDossierPdf(document, ship, batchTemplate, batchSelectedFieldKeys, titleFont, headerFont, boldFont, normalFont);
            }

            document.close();

            byte[] pdfBytes = out.toByteArray();

            for (Shipment ship : eligibleShipments) {
                logDossierExport(ship, currentUser, "SUCCESS", ipAddress, (long) pdfBytes.length,
                    batchTemplate != null ? batchTemplate.getId() : null);
            }

            publishActivityLog(currentUser, "EXPORT_BATCH_DOSSIER",
                "Xuất bộ hồ sơ hợp nhất cho " + eligibleShipments.size() + " lô hàng"
                    + (batchTemplate != null ? " theo mẫu: " + batchTemplate.getName() : ""),
                "BATCH_DOSSIER", request.getTitle() != null ? request.getTitle() : "ALL");

            return pdfBytes;

        } catch (Exception e) {
            log.error("Tạo bộ hồ sơ PDF thất bại: {}", e.getMessage(), e);
            throw new BusinessException("Lỗi khi sinh tệp bộ hồ sơ PDF hợp nhất: " + e.getMessage());
        }
    }

    /** Tải phông chữ cho tệp PDF. */
    private Font loadFont(
        String resource,
        float size,
        int style) {

        String resourcePath = resource.startsWith("/") ? resource : "/" + resource;
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            byte[] fontBytes;
            if (inputStream != null) {
                fontBytes = inputStream.readAllBytes();
            } else {
                try (InputStream cpStream = new org.springframework.core.io.ClassPathResource(resource).getInputStream()) {
                    fontBytes = cpStream.readAllBytes();
                }
            }
            BaseFont baseFont = BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, fontBytes, null);
            return new Font(baseFont, size, style);
        } catch (Exception ex) {
            log.warn("Load custom font failed: {}, falling back to standard font: {}", resource, ex.getMessage());
            return new Font(Font.HELVETICA, size, style);
        }
    }

    /** Lấy lịch sử xuất bộ hồ sơ truy xuất hàng loạt. */
    @Override
    @Transactional(readOnly = true)
    public List<BatchDossierHistoryDto> getBatchExportHistory(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getOrganizationId() == null) {
            return Collections.emptyList();
        }

        List<DossierExportHistory> histories = exportHistoryRepository
            .findByOrganization_OrganizationIdOrderByExportedAtDesc(currentUser.getOrganizationId());

        if (histories == null || histories.isEmpty()) {
            return Collections.emptyList();
        }

        List<BatchDossierHistoryDto> result = new ArrayList<>();
        for (DossierExportHistory h : histories) {
            result.add(BatchDossierHistoryDto.builder()
                .id(h.getId())
                .title(h.getFileName() != null ? h.getFileName() : "Bộ hồ sơ truy xuất")
                .exportedAt(h.getExportedAt())
                .exporterName(h.getExporter() != null ? h.getExporter().getFullName() : "N/A")
                .organizationName(h.getOrganization() != null ? h.getOrganization().getName() : "N/A")
                .totalSelectedLots(1)
                .eligibleLotsCount(1)
                .ineligibleLotsCount(0)
                .fileName(h.getFileName())
                .fileSize(h.getFileSize())
                .status(h.getStatus())
                .ipAddress(h.getIpAddress())
                .templateId(h.getTemplateId())
                .build());
        }
        return result;
    }

    /** Xây dựng dữ liệu lô hàng giả lập chuẩn cho bản xem trước mẫu hồ sơ (SHIP-MOCK-2026-DEMO). */
    private Shipment buildMockShipmentForPreview() {
        Organization org = Organization.builder()
                .name("Hợp tác xã Nông nghiệp Xanh Lâm Đồng")
                .code("HTX-LAMDONG-01")
                .type(vn.nguongocso.organization.enums.OrganizationType.COOPERATIVE)
                .status(vn.nguongocso.organization.enums.OrganizationStatus.ACTIVE)
                .address("Thôn 3, Xã Đạ Ròn, Huyện Đơn Dương, Tỉnh Lâm Đồng")
                .phone("0263.3888.999")
                .email("lienhe@htxxanh.vn")
                .build();

        FarmArea farmArea = FarmArea.builder()
                .name("Vùng chuyên canh Cà Rốt Đơn Dương")
                .area(new java.math.BigDecimal("5.2"))
                .areaUnit(vn.nguongocso.farm.enums.AreaUnit.HA)
                .isActive(true)
                .cropType(vn.nguongocso.farm.entity.ProductCategory.builder().name("Cà rốt F1").build())
                .location(new org.locationtech.jts.geom.GeometryFactory().createPoint(
                        new org.locationtech.jts.geom.Coordinate(108.4567, 11.8345)))
                .build();

        ProductionLot lot = ProductionLot.builder()
                .name("Lô Cà Rốt hữu cơ VietGAP 2026")
                .productCategory(vn.nguongocso.farm.entity.ProductCategory.builder().name("Rau củ quả tươi").build())
                .plantingDate(java.time.LocalDate.of(2026, 6, 15))
                .harvestDate(java.time.LocalDate.of(2026, 9, 10))
                .expectedQuantity(12500.0)
                .expectedQuantityUnit("kg")
                .actualQuantity(12800.0)
                .status(vn.nguongocso.farm.enums.ProductionLotStatus.PACKAGED)
                .organization(org)
                .farmArea(farmArea)
                .build();

        Shipment shipment = new Shipment();
        shipment.setId(null);
        shipment.setName("Chuyến hàng xuất siêu thị Go! - Đà Lạt");
        shipment.setTotalQuantity(2000L);
        shipment.setPackagingInfo("Thùng carton 10kg, dán tem QR GS1");
        shipment.setStatus(vn.nguongocso.trace.enums.ShipmentStatus.ACTIVATED);
        shipment.setCreatedAt(java.time.LocalDateTime.of(2026, 9, 12, 8, 0, 0));
        shipment.setOrganization(org);
        shipment.setProductionLot(lot);
        return shipment;
    }

    /** Xuất tệp PDF xem trước mẫu hồ sơ truy xuất theo cấu hình trường. */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportPreviewPdf(
            UUID orgId,
            String templateName,
            String partnerName,
            Set<String> selectedFieldKeys,
            UUID shipmentId) {

        // Luôn sử dụng dữ liệu mẫu kiểm thử chuẩn SHIP-MOCK-2026-DEMO khi tạo mẫu hồ sơ mới
        Shipment shipment = buildMockShipmentForPreview();

        ProfileTemplate template = ProfileTemplate.builder()
                .name(templateName != null && !templateName.isBlank() ? templateName : "Mẫu hồ sơ mới")
                .partnerName(partnerName)
                .build();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = loadFont("fonts/Roboto-Bold.ttf", 16, Font.BOLD);
            Font headerFont = loadFont("fonts/Roboto-Bold.ttf", 12, Font.BOLD);
            Font boldFont = loadFont("fonts/Roboto-Bold.ttf", 10, Font.BOLD);
            Font normalFont = loadFont("fonts/Roboto-Regular.ttf", 10, Font.NORMAL);

            renderShipmentDossierPdf(document, shipment, template, selectedFieldKeys, titleFont, headerFont, boldFont, normalFont);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi sinh bản xem trước PDF theo mẫu: {}", e.getMessage(), e);
            throw new BusinessException("Lỗi hệ thống khi sinh bản xem trước PDF hồ sơ: " + e.getMessage());
        }
    }
}
