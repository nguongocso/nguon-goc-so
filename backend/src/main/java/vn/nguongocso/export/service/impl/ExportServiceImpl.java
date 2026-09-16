package vn.nguongocso.export.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.export.constant.MandatoryFields;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.dto.response.Qtn11ErrorDetailDto;
import vn.nguongocso.export.entity.ExportLog;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.export.entity.ProfileTemplateField;
import vn.nguongocso.export.exception.TemplateNotOwnedException;
import vn.nguongocso.export.repository.ExportLogRepository;
import vn.nguongocso.export.repository.ProfileTemplateRepository;
import vn.nguongocso.export.schema.OpenDataSchema;
import vn.nguongocso.export.service.ExportService;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ xuất dữ liệu công khai.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final FarmLogRepository farmLogRepository;
    private final ProductionLotCertificationRepository productionLotCertificationRepository;
    private final ProfileTemplateRepository profileTemplateRepository;
    private final ExportLogRepository exportLogRepository;
    private final UserRepository userRepository;
    private final vn.nguongocso.export.service.ProfileTemplateService profileTemplateService;

    private static final List<ChainEventType> REQUIRED_EVENT_TYPES = List.of(
            ChainEventType.HARVEST,
            ChainEventType.PACKAGING,
            ChainEventType.TRANSPORT,
            ChainEventType.PROCUREMENT);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /** Xuất dữ liệu công khai theo yêu cầu. */
    @Override
    @Transactional(readOnly = true)
    public Resource exportOpenData(ExportOpenDataRequest request, CustomUserDetails currentUser) {
        // 1. Role validation (belt-and-suspenders with @PreAuthorize in Controller)
        if (!"VT-05".equals(currentUser.getRoleCode())) {
            throw new BusinessException("Chỉ Cán bộ quản lý ngành (VT-05) mới được xuất dữ liệu này.");
        }

        // 2. Fetch shipments matching basic filters
        List<Shipment> shipments = shipmentRepository.findEligibleShipments(
                request.getOrganizationId(),
                request.getFromDate(),
                request.getToDate(),
                request.getProductCategoryIds(),
                request.getShipmentIds());

        if (shipments.isEmpty()) {
            throw new BusinessException("Không có lô hàng nào trong phạm vi lọc.");
        }

        List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).collect(Collectors.toList());

        // 3. QTN-11: Validate completeness (all required events + documentation)
        Map<UUID, Set<ChainEventType>> eventMap = getEventTypesByShipment(shipmentIds);
        Map<UUID, Boolean> docMap = getDocumentationExistence(shipments);

        List<Shipment> eligibleShipments = new ArrayList<>();
        List<Qtn11ErrorDetailDto> qtn11ErrorDetails = new ArrayList<>();

        for (Shipment s : shipments) {
            Set<ChainEventType> existingEvents = eventMap.getOrDefault(s.getId(), Collections.emptySet());
            boolean hasAllEvents = existingEvents.containsAll(REQUIRED_EVENT_TYPES);
            boolean hasDocs = docMap.getOrDefault(s.getId(), false);

            if (hasAllEvents && hasDocs) {
                eligibleShipments.add(s);
            } else {
                List<String> missingEvents = new ArrayList<>();
                for (ChainEventType type : REQUIRED_EVENT_TYPES) {
                    if (!existingEvents.contains(type)) {
                        missingEvents.add(getEventTypeNameInVietnamese(type));
                    }
                }
                List<String> missingDocDetails = new ArrayList<>();
                if (!hasDocs) {
                    missingDocDetails.add("Chưa có nhật ký nông hộ hoặc tệp chứng nhận lô hàng đính kèm");
                }

                qtn11ErrorDetails.add(Qtn11ErrorDetailDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .lotCode(s.getProductionLot() != null ? s.getProductionLot().getName() : "Không xác định")
                        .missingEvents(missingEvents)
                        .missingDocs(!hasDocs)
                        .missingDocDetails(missingDocDetails)
                        .build());
            }
        }

        if (eligibleShipments.isEmpty()) {
            throw new BusinessException(
                    "Không có lô hàng nào đáp ứng đủ điều kiện (thiếu sự kiện chuỗi cung ứng hoặc chứng từ).",
                    qtn11ErrorDetails);
        }

        // 4. Build export schema
        OpenDataSchema schema = buildSchema(eligibleShipments, currentUser);

        // 5. Generate file
        return generateFile(schema, request.getFormat());
    }

    private Map<UUID, Set<ChainEventType>> getEventTypesByShipment(List<UUID> shipmentIds) {
        // 1. Lấy danh sách shipment để biết productionLotId
        List<Shipment> shipments = shipmentRepository.findAllById(shipmentIds);
        Map<UUID, UUID> shipmentToLotMap = shipments.stream()
                .collect(Collectors.toMap(
                        Shipment::getId,
                        s -> s.getProductionLot() != null ? s.getProductionLot().getId() : null
                ));

        // 2. Lấy tất cả events của các shipment (TRANSPORT, PROCUREMENT)
        Map<UUID, Set<ChainEventType>> eventMap = new HashMap<>();
        List<ChainEvent> shipmentEvents = chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(shipmentIds);
        for (ChainEvent e : shipmentEvents) {
            UUID sid = e.getShipment().getId();
            eventMap.computeIfAbsent(sid, k -> new HashSet<>()).add(e.getEventType());
        }

        // 3. Lấy các events không gắn shipment (HARVEST, PACKAGING) và gắn vào production lot
        List<ChainEventType> unassignedTypes = List.of(ChainEventType.HARVEST, ChainEventType.PACKAGING);
        List<ChainEvent> unassignedEvents = chainEventRepository.findByShipmentIsNullAndEventTypeIn(unassignedTypes);

        // Gom nhóm events theo productionLotId (từ eventData)
        Map<UUID, Set<ChainEventType>> lotEventMap = new HashMap<>();
        for (ChainEvent e : unassignedEvents) {
            Map<String, Object> data = parseEventData(e.getEventData());
            Object lotIdObj = data.get("productionLotId");
            if (lotIdObj != null) {
                UUID lotId = UUID.fromString(lotIdObj.toString());
                lotEventMap.computeIfAbsent(lotId, k -> new HashSet<>()).add(e.getEventType());
            }
        }

        // 4. Merge: với mỗi shipment, thêm các event từ production lot tương ứng
        for (Map.Entry<UUID, UUID> entry : shipmentToLotMap.entrySet()) {
            UUID shipmentId = entry.getKey();
            UUID lotId = entry.getValue();
            if (lotId != null && lotEventMap.containsKey(lotId)) {
                eventMap.computeIfAbsent(shipmentId, k -> new HashSet<>())
                        .addAll(lotEventMap.get(lotId));
            }
        }

        return eventMap;
    }

    /**
     * QTN-11 documentation check: a shipment has documentation if:
     * - its ProductionLot has at least 1 FarmLog (farm diary entry), OR
     * - its ProductionLot has at least 1 ProductionLotCertification attached
     */
    private Map<UUID, Boolean> getDocumentationExistence(List<Shipment> shipments) {
        Map<UUID, Boolean> result = new HashMap<>();

        // Collect unique production lot IDs
        Set<UUID> lotIds = shipments.stream()
                .map(Shipment::getProductionLot)
                .filter(Objects::nonNull)
                .map(ProductionLot::getId)
                .collect(Collectors.toSet());

        // Pre-load: which lots have farm logs
        Set<UUID> lotsWithFarmLogs = lotIds.stream()
                .filter(farmLogRepository::existsByProductionLotId)
                .collect(Collectors.toSet());

        // Pre-load: which lots have certifications
        List<ProductionLotCertification> certs = productionLotCertificationRepository
                .findByProductionLotIdIn(new ArrayList<>(lotIds));
        Set<UUID> lotsWithCerts = certs.stream()
                .map(c -> c.getProductionLot().getId())
                .collect(Collectors.toSet());

        for (Shipment s : shipments) {
            UUID lotId = s.getProductionLot() != null ? s.getProductionLot().getId() : null;
            boolean hasDocs = lotId != null &&
                    (lotsWithFarmLogs.contains(lotId) || lotsWithCerts.contains(lotId));
            result.put(s.getId(), hasDocs);
        }

        return result;
    }

    private OpenDataSchema buildSchema(List<Shipment> shipments, CustomUserDetails currentUser) {
        List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).collect(Collectors.toList());
        List<ChainEvent> allEvents = chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(shipmentIds);

        Map<UUID, List<ChainEvent>> eventsByShipment = allEvents.stream()
                .collect(Collectors.groupingBy(e -> e.getShipment().getId()));

        // Collect all production lot IDs for certification fetch
        List<UUID> lotIds = shipments.stream()
                .map(Shipment::getProductionLot)
                .filter(Objects::nonNull)
                .map(ProductionLot::getId)
                .distinct()
                .collect(Collectors.toList());

        List<ProductionLotCertification> allCerts = productionLotCertificationRepository
                .findByProductionLotIdIn(lotIds);
        Map<UUID, List<ProductionLotCertification>> certsByLot = allCerts.stream()
                .collect(Collectors.groupingBy(c -> c.getProductionLot().getId()));

        List<OpenDataSchema.ShipmentData> shipmentDataList = shipments.stream().map(s -> {
            ProductionLot lot = s.getProductionLot();
            List<ChainEvent> events = eventsByShipment.getOrDefault(s.getId(), Collections.emptyList());
            List<ProductionLotCertification> certs = lot != null
                    ? certsByLot.getOrDefault(lot.getId(), Collections.emptyList())
                    : Collections.emptyList();

            List<OpenDataSchema.TimelineEvent> timeline = events.stream()
                    .map(e -> OpenDataSchema.TimelineEvent.builder()
                            .eventType(e.getEventType().name())
                            .recordedAt(e.getRecordedAt())
                            .recordedBy(e.getRecordedBy() != null ? e.getRecordedBy().getFullName() : null)
                            .location(OpenDataSchema.Location.builder()
                                    .latitude(e.getLocation() != null ? e.getLocation().getY() : null)
                                    .longitude(e.getLocation() != null ? e.getLocation().getX() : null)
                                    .build())
                            .data(parseEventData(e.getEventData()))
                            .build())
                    .collect(Collectors.toList());

            List<OpenDataSchema.CertificationInfo> certInfos = certs.stream()
                    .map(plc -> {
                        var cert = plc.getCertification();
                        return OpenDataSchema.CertificationInfo.builder()
                                .standardName(cert.getName())
                                .certificationCode(cert.getCode())
                                .issueDate(cert.getIssueDate() != null
                                        ? cert.getIssueDate().atStartOfDay()
                                        : null)
                                .expiryDate(cert.getExpiryDate() != null
                                        ? cert.getExpiryDate().atStartOfDay()
                                        : null)
                                .attachedFileUrl(null) // No file URL model in current schema
                                .build();
                    })
                    .collect(Collectors.toList());

            return OpenDataSchema.ShipmentData.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .productionLotName(lot != null ? lot.getName() : null)
                    .productCategory(lot != null && lot.getProductCategory() != null
                            ? lot.getProductCategory().getName()
                            : null)
                    .totalQuantity((double) s.getTotalQuantity())
                    .unit(lot != null ? lot.getExpectedQuantityUnit() : null)
                    .status(s.getStatus().name())
                    .timeline(timeline)
                    .certifications(certInfos)
                    .build();
        }).collect(Collectors.toList());

        return OpenDataSchema.builder()
                .exportedAt(LocalDateTime.now())
                .exporter(OpenDataSchema.ExporterInfo.builder()
                        .userId(currentUser.getUserId())
                        .fullName(currentUser.getFullName())
                        .organizationId(currentUser.getOrganizationId())
                        .organizationName(currentUser.getOrganizationName())
                        .build())
                .shipments(shipmentDataList)
                .build();
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

    private Resource generateFile(OpenDataSchema schema, String format) {
        try {
            String content;
            String extension;

            if ("xml".equalsIgnoreCase(format)) {
                throw new BusinessException("Định dạng XML chưa được hỗ trợ trong phiên bản này. Vui lòng chọn JSON.");
            }

            if ("csv".equalsIgnoreCase(format)) {
                content = convertToCsv(schema);
                extension = "csv";
            } else {
                // JSON (default)
                content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
                extension = "json";
            }

            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            return new ByteArrayResource(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Lỗi khi tạo file xuất: " + e.getMessage());
        }
    }

    /**
     * Converts OpenDataSchema to CSV by flattening shipments into rows.
     * Each row represents one shipment with timeline/certifications as JSON
     * columns.
     */
    private String convertToCsv(OpenDataSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("shipmentId,name,productionLotName,productCategory,totalQuantity,unit,status,timeline,exportedAt\n");

        String timelineJson;
        try {
            timelineJson = objectMapper.writeValueAsString(
                    schema.getShipments().stream().map(s -> s.getTimeline()).collect(Collectors.toList()));
        } catch (Exception e) {
            timelineJson = "[]";
        }

        // For CSV, we flatten the certification info into JSON as well
        for (OpenDataSchema.ShipmentData s : schema.getShipments()) {
            String certsJson;
            try {
                certsJson = objectMapper.writeValueAsString(s.getCertifications());
            } catch (Exception e) {
                certsJson = "[]";
            }
            sb.append(String.format("%s,%s,%s,%s,%.2f,%s,%s,\"%s\",%s\n",
                    escapeCsv(s.getId().toString()),
                    escapeCsv(s.getName()),
                    escapeCsv(s.getProductionLotName()),
                    escapeCsv(s.getProductCategory()),
                    s.getTotalQuantity(),
                    escapeCsv(s.getUnit()),
                    s.getStatus(),
                    timelineJson.replace("\"", "\"\""),
                    escapeCsv(schema.getExportedAt().toString())));
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getEventTypeNameInVietnamese(ChainEventType type) {
        if (type == null) return "";
        return switch (type) {
            case HARVEST -> "Thu hoạch (HARVEST)";
            case PACKAGING -> "Đóng gói (PACKAGING)";
            case TRANSPORT -> "Vận chuyển (TRANSPORT)";
            case PROCUREMENT -> "Thu mua (PROCUREMENT)";
            default -> type.name();
        };
    }

    /**
     * Xuất hồ sơ truy xuất áp dụng mẫu cấu hình theo yêu cầu đối tác (NCL-07-CN-007).
     */
    @Override
    @Transactional
    public Resource exportWithTemplate(UUID shipmentId, UUID templateId, String format, CustomUserDetails currentUser) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin lô hàng."));

        UUID userOrgId = currentUser.getOrganizationId();
        // Tổ chức hiệu dụng: đối với VT-04 là tổ chức HTX sở hữu lô hàng, đối với VT-02 là tổ chức của người dùng
        UUID effectiveOrgId = userOrgId;
        if ("VT-04".equals(currentUser.getRoleCode()) && shipment.getOrganization() != null) {
            effectiveOrgId = shipment.getOrganization().getOrganizationId();
        }

        if ("VT-02".equals(currentUser.getRoleCode())) {
            if (shipment.getOrganization() == null || !shipment.getOrganization().getOrganizationId().equals(userOrgId)) {
                throw new TemplateNotOwnedException("Từ chối thao tác: Lô hàng không thuộc tổ chức của bạn.");
            }
        }

        // 1. Xác định mẫu hồ sơ áp dụng
        ProfileTemplate template = null;
        if (templateId != null) {
            template = profileTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin mẫu hồ sơ."));

            // VT-04: Kiểm tra template có thuộc tổ chức hiệu dụng (HTX) không
            if ("VT-04".equals(currentUser.getRoleCode())) {
                if (!template.getOrganization().getOrganizationId().equals(effectiveOrgId)) {
                    throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của lô hàng này.");
                }
            } else {
                if (!template.getOrganization().getOrganizationId().equals(userOrgId)) {
                    throw new TemplateNotOwnedException("Mẫu hồ sơ không thuộc tổ chức của bạn.");
                }
            }
        } else {
            // TC-03: không chọn mẫu -> dùng mẫu mặc định của tổ chức hiệu dụng
            template = profileTemplateRepository.findByOrganization_OrganizationIdAndIsDefaultTrue(effectiveOrgId).orElse(null);
        }

        // 2. Thu thập các trường được chọn
        Set<String> selectedFieldKeys;
        if (template != null && template.getFields() != null && !template.getFields().isEmpty()) {
            selectedFieldKeys = template.getFields().stream()
                    .map(ProfileTemplateField::getFieldKey)
                    .collect(Collectors.toSet());
        } else {
            selectedFieldKeys = new HashSet<>(MandatoryFields.FIELD_DISPLAY_NAMES.keySet());
        }

        // 3. Build dữ liệu xem trước hồ sơ đã lọc theo mẫu (đồng bộ 100% với preview)
        Map<String, Object> previewData = profileTemplateService.buildPreview(shipmentId, templateId, currentUser);

        // 4. Ghi nhận nhật ký xuất hồ sơ ExportLog (TC-03)
        User user = currentUser.getUserId() != null
                ? userRepository.findById(currentUser.getUserId()).orElse(null)
                : null;

        ExportLog exportLog = ExportLog.builder()
                .shipment(shipment)
                .template(template)
                .exportedBy(user)
                .exportedAt(LocalDateTime.now())
                .build();
        exportLogRepository.save(exportLog);

        log.info("Đã ghi nhận nhật ký xuất hồ sơ (ExportLog ID: {}) cho shipment ID: {}, template: {}",
                exportLog.getId(), shipment.getId(), template != null ? template.getName() : "Mặc định hệ thống");

        // 5. Sinh file trả về theo định dạng yêu cầu (khớp 100% với bản xem trước)
        if ("csv".equalsIgnoreCase(format)) {
            String csvContent = convertPreviewToCsv(previewData);
            return new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));
        }

        // Mặc định JSON
        try {
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(previewData);
            return new ByteArrayResource(jsonContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi dữ liệu hồ sơ sang JSON: {}", e.getMessage(), e);
            throw new BusinessException("Lỗi khi tạo file xuất JSON: " + e.getMessage());
        }
    }

    private String convertPreviewToCsv(Map<String, Object> preview) {
        StringBuilder sb = new StringBuilder("\uFEFF");

        sb.append("# HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM\n");
        if (preview.get("appliedTemplate") instanceof Map<?, ?> tpl) {
            sb.append("# Mẫu hồ sơ: ").append(escapeCsv(String.valueOf(tpl.get("templateName")))).append("\n");
        }
        sb.append("# Thời gian xuất: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        sb.append("Nhóm thông tin,Trường dữ liệu,Giá trị\n");

        // Đơn vị sản xuất
        if (preview.get("organization") instanceof Map<?, ?> org) {
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Tên tổ chức", org.get("name"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Mã định danh", org.get("code"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Loại hình tổ chức", org.get("type"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Trạng thái tổ chức", org.get("status"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Địa chỉ", org.get("address"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Tỉnh / Thành phố", org.get("province"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Số điện thoại", org.get("phone"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Email", org.get("email"));
        }

        // Vùng trồng
        if (preview.get("farmArea") instanceof Map<?, ?> farmArea) {
            appendCsvRowIfPresent(sb, "Vùng trồng", "Tên vùng trồng", farmArea.get("name"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Tọa độ địa lý", farmArea.get("location"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Diện tích canh tác", farmArea.get("area"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Đơn vị diện tích", farmArea.get("areaUnit"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Loại cây trồng", farmArea.get("cropType"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Trạng thái vùng trồng", farmArea.get("isActive"));
        }

        // Lô sản xuất
        if (preview.get("productionLot") instanceof Map<?, ?> lot) {
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Tên lô sản xuất", lot.get("name"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Danh mục sản phẩm", lot.get("productCategory"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Ngày xuống giống", lot.get("plantingDate"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Ngày thu hoạch", lot.get("harvestDate"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Sản lượng dự kiến", lot.get("expectedQuantity"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Đơn vị tính sản lượng", lot.get("expectedQuantityUnit"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Sản lượng thực tế", lot.get("actualQuantity"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Trạng thái", lot.get("status"));
        }

        // Lô hàng vận chuyển
        if (preview.get("shipment") instanceof Map<?, ?> shipment) {
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Tên lô hàng", shipment.get("name"));
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Số lượng", shipment.get("totalQuantity"));
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Quy cách đóng gói", shipment.get("packagingInfo"));
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Trạng thái", shipment.get("status"));
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Thời điểm tạo lô hàng", shipment.get("createdAt"));
        }

        // Chứng nhận tiêu chuẩn
        if (preview.get("certifications") instanceof List<?> certs && !certs.isEmpty()) {
            sb.append("\n# CHỨNG NHẬN TIÊU CHUẨN\n");
            sb.append("STT,Tên chứng nhận,Tiêu chuẩn,Số hiệu,Ngày cấp,Hạn hiệu lực,Tổ chức chứng nhận\n");
            int idx = 1;
            for (Object item : certs) {
                if (item instanceof Map<?, ?> cItem) {
                    sb.append(idx++).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "name"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "standardName"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "certificationCode"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "issueDate"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "expiryDate"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "certifier"))).append("\n");
                }
            }
        }

        // Nhật ký canh tác
        if (preview.get("farmLogs") instanceof List<?> logs && !logs.isEmpty()) {
            sb.append("\n# LỊCH TRÌNH CANH TÁC & CHỨNG TỪ\n");
            sb.append("STT,Ngày thực hiện,Hoạt động,Vật tư / Số lượng,Ghi chú,Chứng từ đính kèm\n");
            int idx = 1;
            for (Object item : logs) {
                if (item instanceof Map<?, ?> logItem) {
                    sb.append(idx++).append(",");
                    sb.append(escapeCsv(getMapValue(logItem, "executedDate"))).append(",");
                    sb.append(escapeCsv(getMapValue(logItem, "activityType"))).append(",");
                    String mat = getMapValue(logItem, "material");
                    Object qty = logItem.get("quantity");
                    String unit = getMapValue(logItem, "unit");
                    String matInfo = mat + (qty != null ? " (" + qty + (!unit.isBlank() ? " " + unit : "") + ")" : "");
                    sb.append(escapeCsv(matInfo.trim())).append(",");
                    sb.append(escapeCsv(getMapValue(logItem, "notes"))).append(",");

                    String attStr = "";
                    if (logItem.get("attachments") instanceof List<?> attList) {
                        attStr = attList.stream().map(Object::toString).collect(Collectors.joining("; "));
                    }
                    sb.append(escapeCsv(attStr)).append("\n");
                }
            }
        }

        // Kiểm nghiệm
        if (preview.get("inspections") instanceof List<?> insps && !insps.isEmpty()) {
            sb.append("\n# LỊCH SỬ KIỂM NGHIỆM\n");
            sb.append("STT,Ngày gửi mẫu,Đơn vị kiểm nghiệm,Chỉ tiêu / Tiêu chuẩn,Kết quả,Ngày cấp kết quả,Hạn hiệu lực\n");
            int idx = 1;
            for (Object item : insps) {
                if (item instanceof Map<?, ?> inspItem) {
                    sb.append(idx++).append(",");
                    sb.append(escapeCsv(getMapValue(inspItem, "sampleSentDate"))).append(",");
                    sb.append(escapeCsv(getMapValue(inspItem, "inspectionUnit"))).append(",");
                    sb.append(escapeCsv(getMapValue(inspItem, "criterionName"))).append(",");
                    String res = getMapValue(inspItem, "passed");
                    if (res.isBlank()) res = getMapValue(inspItem, "status");
                    sb.append(escapeCsv(res)).append(",");
                    sb.append(escapeCsv(getMapValue(inspItem, "resultDate"))).append(",");
                    sb.append(escapeCsv(getMapValue(inspItem, "expiryDate"))).append("\n");
                }
            }
        }

        // Dòng sự kiện
        if (preview.get("timelineEvents") instanceof List<?> events && !events.isEmpty()) {
            sb.append("\n# DÒNG SỰ KIỆN CHUỖI CUNG ỨNG\n");
            sb.append("STT,Thời điểm ghi nhận,Loại sự kiện,Tọa độ địa điểm,Chi tiết sự kiện,Người ghi nhận\n");
            int idx = 1;
            for (Object item : events) {
                if (item instanceof Map<?, ?> ev) {
                    sb.append(idx++).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "recordedAt"))).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "eventType"))).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "location"))).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "eventData"))).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "recordedBy"))).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private String getMapValue(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private void appendCsvRowIfPresent(StringBuilder sb, String group, String label, Object val) {
        if (val != null) {
            sb.append(escapeCsv(group)).append(",")
              .append(escapeCsv(label)).append(",")
              .append(escapeCsv(val.toString())).append("\n");
        }
    }

    private OpenDataSchema buildFilteredSchema(List<Shipment> shipments, CustomUserDetails currentUser, Set<String> selectedFieldKeys) {
        List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).collect(Collectors.toList());

        boolean includeTimeline = selectedFieldKeys.stream().anyMatch(k -> k.startsWith("chainEvent."));
        Map<UUID, List<ChainEvent>> eventsByShipment = new HashMap<>();
        if (includeTimeline) {
            List<ChainEvent> allEvents = chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(shipmentIds);
            eventsByShipment = allEvents.stream().collect(Collectors.groupingBy(e -> e.getShipment().getId()));
        }

        boolean includeCerts = selectedFieldKeys.stream().anyMatch(k -> k.startsWith("certification."));
        Map<UUID, List<ProductionLotCertification>> certsByLot = new HashMap<>();
        if (includeCerts) {
            List<UUID> lotIds = shipments.stream()
                    .map(Shipment::getProductionLot)
                    .filter(Objects::nonNull)
                    .map(ProductionLot::getId)
                    .distinct()
                    .collect(Collectors.toList());
            List<ProductionLotCertification> allCerts = productionLotCertificationRepository.findByProductionLotIdIn(lotIds);
            certsByLot = allCerts.stream().collect(Collectors.groupingBy(c -> c.getProductionLot().getId()));
        }

        Map<UUID, List<ChainEvent>> finalEventsMap = eventsByShipment;
        Map<UUID, List<ProductionLotCertification>> finalCertsMap = certsByLot;

        List<OpenDataSchema.ShipmentData> shipmentDataList = shipments.stream().map(s -> {
            ProductionLot lot = s.getProductionLot();
            List<ChainEvent> events = finalEventsMap.getOrDefault(s.getId(), Collections.emptyList());
            List<ProductionLotCertification> certs = lot != null
                    ? finalCertsMap.getOrDefault(lot.getId(), Collections.emptyList())
                    : Collections.emptyList();

            List<OpenDataSchema.TimelineEvent> timeline = includeTimeline
                    ? events.stream().map(e -> OpenDataSchema.TimelineEvent.builder()
                            .eventType(selectedFieldKeys.contains("chainEvent.eventType") ? e.getEventType().name() : null)
                            .recordedAt(selectedFieldKeys.contains("chainEvent.recordedAt") ? e.getRecordedAt() : null)
                            .recordedBy(selectedFieldKeys.contains("chainEvent.recordedBy") && e.getRecordedBy() != null ? e.getRecordedBy().getFullName() : null)
                            .location(selectedFieldKeys.contains("chainEvent.location") && e.getLocation() != null ? OpenDataSchema.Location.builder()
                                    .latitude(e.getLocation().getY())
                                    .longitude(e.getLocation().getX())
                                    .build() : null)
                            .data(selectedFieldKeys.contains("chainEvent.eventData") ? parseEventData(e.getEventData()) : null)
                            .build()).collect(Collectors.toList())
                    : Collections.emptyList();

            List<OpenDataSchema.CertificationInfo> certInfos = includeCerts
                    ? certs.stream().map(plc -> {
                        var cert = plc.getCertification();
                        return OpenDataSchema.CertificationInfo.builder()
                                .standardName(selectedFieldKeys.contains("certification.standardName") ? cert.getName() : null)
                                .certificationCode(selectedFieldKeys.contains("certification.certificationCode") ? cert.getCode() : null)
                                .issueDate(selectedFieldKeys.contains("certification.issueDate") && cert.getIssueDate() != null ? cert.getIssueDate().atStartOfDay() : null)
                                .expiryDate(selectedFieldKeys.contains("certification.expiryDate") && cert.getExpiryDate() != null ? cert.getExpiryDate().atStartOfDay() : null)
                                .build();
                    }).collect(Collectors.toList())
                    : Collections.emptyList();

            return OpenDataSchema.ShipmentData.builder()
                    .id(s.getId())
                    .name(selectedFieldKeys.contains("shipment.name") ? s.getName() : null)
                    .productionLotName(selectedFieldKeys.contains("productionLot.name") && lot != null ? lot.getName() : null)
                    .productCategory(selectedFieldKeys.contains("productionLot.productCategory") && lot != null && lot.getProductCategory() != null ? lot.getProductCategory().getName() : null)
                    .totalQuantity(selectedFieldKeys.contains("shipment.totalQuantity") ? (double) s.getTotalQuantity() : null)
                    .unit(lot != null ? lot.getExpectedQuantityUnit() : null)
                    .status(selectedFieldKeys.contains("shipment.status") ? s.getStatus().name() : null)
                    .timeline(timeline)
                    .certifications(certInfos)
                    .build();
        }).collect(Collectors.toList());

        OpenDataSchema.ExporterInfo exporterInfo = null;
        if (currentUser != null && (selectedFieldKeys.contains("organization.name") || selectedFieldKeys.contains("organization.code"))) {
            exporterInfo = OpenDataSchema.ExporterInfo.builder()
                    .userId(currentUser.getUserId())
                    .fullName(currentUser.getFullName())
                    .organizationId(currentUser.getOrganizationId())
                    .organizationName(selectedFieldKeys.contains("organization.name") ? currentUser.getOrganizationName() : null)
                    .build();
        }

        return OpenDataSchema.builder()
                .exportedAt(LocalDateTime.now())
                .exporter(exporterInfo)
                .shipments(shipmentDataList)
                .build();
    }
}