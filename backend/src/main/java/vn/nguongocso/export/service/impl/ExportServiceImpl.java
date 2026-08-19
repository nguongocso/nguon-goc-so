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
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.export.dto.request.ExportOpenDataRequest;
import vn.nguongocso.export.dto.response.Qtn11ErrorDetailDto;
import vn.nguongocso.export.schema.OpenDataSchema;
import vn.nguongocso.export.service.ExportService;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ xuất dữ liệu công khai.
 */
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final FarmLogRepository farmLogRepository;
    private final ProductionLotCertificationRepository productionLotCertificationRepository;

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
}