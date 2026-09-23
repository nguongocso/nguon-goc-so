package vn.nguongocso.export.service.processor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
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
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Thành phần chuyên trách xử lý luồng xuất Open Data cho Cán bộ quản lý ngành (VT-05).
 * Đọc dữ liệu snapshot trong read-only transaction và render tệp ngoài transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenDataExportProcessor {

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

    /** Đọc và xác thực tính đầy đủ dữ liệu QTN-11 của các lô hàng trong một read-only transaction độc lập. */
    @Transactional(readOnly = true)
    public OpenDataSchema buildSnapshot(ExportOpenDataRequest request, CustomUserDetails currentUser) {
        if (!"VT-05".equals(currentUser.getRoleCode())) {
            throw new BusinessException("Chỉ Cán bộ quản lý ngành (VT-05) mới được xuất dữ liệu này.");
        }

        List<Shipment> shipments = shipmentRepository.findEligibleShipments(
                request.getOrganizationId(),
                request.getFromDate(),
                request.getToDate(),
                request.getProductCategoryIds(),
                request.getShipmentIds());

        if (shipments.isEmpty()) {
            throw new BusinessException("Không có lô hàng nào trong phạm vi lọc.");
        }

        List<Shipment> eligibleShipments = filterEligibleShipments(shipments);
        return buildSchemaFromEligibleShipments(eligibleShipments, currentUser);
    }

    /** Sinh tệp JSON/CSV thuần túy in-memory hoàn toàn ngoài transaction DB. */
    public Resource generateFile(OpenDataSchema schema, String format) {
        try {
            if ("xml".equalsIgnoreCase(format)) {
                throw new BusinessException("Định dạng XML chưa được hỗ trợ trong phiên bản này. Vui lòng chọn JSON.");
            }

            String content;
            if ("csv".equalsIgnoreCase(format)) {
                content = convertToCsv(schema);
            } else {
                content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
            }

            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            return new ByteArrayResource(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi tạo file xuất: {}", e.getMessage(), e);
            throw new BusinessException("Lỗi khi tạo file xuất: " + e.getMessage());
        }
    }

    private List<Shipment> filterEligibleShipments(List<Shipment> shipments) {
        List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).collect(Collectors.toList());
        Map<UUID, Set<ChainEventType>> eventMap = getEventTypesByShipment(shipmentIds);
        Map<UUID, Boolean> docMap = getDocumentationExistence(shipments);

        List<Shipment> eligible = new ArrayList<>();
        List<Qtn11ErrorDetailDto> errorDetails = new ArrayList<>();

        for (Shipment s : shipments) {
            Set<ChainEventType> existingEvents = eventMap.getOrDefault(s.getId(), Collections.emptySet());
            boolean hasAllEvents = existingEvents.containsAll(REQUIRED_EVENT_TYPES);
            boolean hasDocs = docMap.getOrDefault(s.getId(), false);

            if (hasAllEvents && hasDocs) {
                eligible.add(s);
            } else {
                errorDetails.add(buildQtn11ErrorDetail(s, existingEvents, hasDocs));
            }
        }

        if (eligible.isEmpty()) {
            throw new BusinessException(
                    "Không có lô hàng nào đáp ứng đủ điều kiện (thiếu sự kiện chuỗi cung ứng hoặc chứng từ).",
                    errorDetails);
        }
        return eligible;
    }

    private Qtn11ErrorDetailDto buildQtn11ErrorDetail(Shipment s, Set<ChainEventType> existingEvents, boolean hasDocs) {
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

        return Qtn11ErrorDetailDto.builder()
                .id(s.getId())
                .name(s.getName())
                .lotCode(s.getProductionLot() != null ? s.getProductionLot().getName() : "Không xác định")
                .missingEvents(missingEvents)
                .missingDocs(!hasDocs)
                .missingDocDetails(missingDocDetails)
                .build();
    }

    private Map<UUID, Set<ChainEventType>> getEventTypesByShipment(List<UUID> shipmentIds) {
        List<Shipment> shipments = shipmentRepository.findAllById(shipmentIds);
        Map<UUID, UUID> shipmentToLotMap = shipments.stream()
                .collect(Collectors.toMap(
                        Shipment::getId,
                        s -> s.getProductionLot() != null ? s.getProductionLot().getId() : null));

        Map<UUID, Set<ChainEventType>> eventMap = new HashMap<>();
        List<ChainEvent> shipmentEvents = chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(shipmentIds);
        for (ChainEvent e : shipmentEvents) {
            UUID sid = e.getShipment().getId();
            eventMap.computeIfAbsent(sid, k -> new HashSet<>()).add(e.getEventType());
        }

        List<ChainEventType> unassignedTypes = List.of(ChainEventType.HARVEST, ChainEventType.PACKAGING);
        List<ChainEvent> unassignedEvents = chainEventRepository.findByShipmentIsNullAndEventTypeIn(unassignedTypes);

        Map<UUID, Set<ChainEventType>> lotEventMap = new HashMap<>();
        for (ChainEvent e : unassignedEvents) {
            Map<String, Object> data = parseEventData(e.getEventData());
            Object lotIdObj = data.get("productionLotId");
            if (lotIdObj != null) {
                UUID lotId = UUID.fromString(lotIdObj.toString());
                lotEventMap.computeIfAbsent(lotId, k -> new HashSet<>()).add(e.getEventType());
            }
        }

        for (Map.Entry<UUID, UUID> entry : shipmentToLotMap.entrySet()) {
            UUID shipmentId = entry.getKey();
            UUID lotId = entry.getValue();
            if (lotId != null && lotEventMap.containsKey(lotId)) {
                eventMap.computeIfAbsent(shipmentId, k -> new HashSet<>()).addAll(lotEventMap.get(lotId));
            }
        }
        return eventMap;
    }

    private Map<UUID, Boolean> getDocumentationExistence(List<Shipment> shipments) {
        Map<UUID, Boolean> result = new HashMap<>();
        Set<UUID> lotIds = shipments.stream()
                .map(Shipment::getProductionLot)
                .filter(Objects::nonNull)
                .map(ProductionLot::getId)
                .collect(Collectors.toSet());

        Set<UUID> lotsWithFarmLogs = lotIds.stream()
                .filter(farmLogRepository::existsByProductionLotId)
                .collect(Collectors.toSet());

        List<ProductionLotCertification> certs = productionLotCertificationRepository
                .findByProductionLotIdIn(new ArrayList<>(lotIds));
        Set<UUID> lotsWithCerts = certs.stream()
                .map(c -> c.getProductionLot().getId())
                .collect(Collectors.toSet());

        for (Shipment s : shipments) {
            UUID lotId = s.getProductionLot() != null ? s.getProductionLot().getId() : null;
            boolean hasDocs = lotId != null && (lotsWithFarmLogs.contains(lotId) || lotsWithCerts.contains(lotId));
            result.put(s.getId(), hasDocs);
        }
        return result;
    }

    private OpenDataSchema buildSchemaFromEligibleShipments(List<Shipment> shipments, CustomUserDetails currentUser) {
        List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).collect(Collectors.toList());
        List<ChainEvent> allEvents = chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(shipmentIds);
        Map<UUID, List<ChainEvent>> eventsByShipment = allEvents.stream()
                .collect(Collectors.groupingBy(e -> e.getShipment().getId()));

        List<UUID> lotIds = shipments.stream()
                .map(Shipment::getProductionLot)
                .filter(Objects::nonNull)
                .map(ProductionLot::getId)
                .distinct()
                .collect(Collectors.toList());

        List<ProductionLotCertification> allCerts =
                productionLotCertificationRepository.findByProductionLotIdIn(lotIds);
        Map<UUID, List<ProductionLotCertification>> certsByLot = allCerts.stream()
                .collect(Collectors.groupingBy(c -> c.getProductionLot().getId()));

        List<OpenDataSchema.ShipmentData> shipmentDataList = shipments.stream()
                .map(s -> mapToShipmentData(s, eventsByShipment, certsByLot))
                .collect(Collectors.toList());

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

    private OpenDataSchema.ShipmentData mapToShipmentData(
            Shipment s,
            Map<UUID, List<ChainEvent>> eventsByShipment,
            Map<UUID, List<ProductionLotCertification>> certsByLot) {

        ProductionLot lot = s.getProductionLot();
        List<ChainEvent> events = eventsByShipment.getOrDefault(s.getId(), Collections.emptyList());
        List<ProductionLotCertification> certs = lot != null
                ? certsByLot.getOrDefault(lot.getId(), Collections.emptyList())
                : Collections.emptyList();

        List<OpenDataSchema.TimelineEvent> timeline = events.stream()
                .map(this::mapToTimelineEvent)
                .collect(Collectors.toList());

        List<OpenDataSchema.CertificationInfo> certInfos = certs.stream()
                .map(this::mapToCertificationInfo)
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
    }

    private OpenDataSchema.TimelineEvent mapToTimelineEvent(ChainEvent e) {
        return OpenDataSchema.TimelineEvent.builder()
                .eventType(e.getEventType().name())
                .recordedAt(e.getRecordedAt())
                .recordedBy(e.getRecordedBy() != null ? e.getRecordedBy().getFullName() : null)
                .location(OpenDataSchema.Location.builder()
                        .latitude(e.getLocation() != null ? e.getLocation().getY() : null)
                        .longitude(e.getLocation() != null ? e.getLocation().getX() : null)
                        .build())
                .data(parseEventData(e.getEventData()))
                .build();
    }

    private OpenDataSchema.CertificationInfo mapToCertificationInfo(ProductionLotCertification plc) {
        var cert = plc.getCertification();
        return OpenDataSchema.CertificationInfo.builder()
                .standardName(cert.getName())
                .certificationCode(cert.getCode())
                .issueDate(cert.getIssueDate() != null ? cert.getIssueDate().atStartOfDay() : null)
                .expiryDate(cert.getExpiryDate() != null ? cert.getExpiryDate().atStartOfDay() : null)
                .attachedFileUrl(null)
                .build();
    }

    private String convertToCsv(OpenDataSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("shipmentId,name,productionLotName,productCategory,totalQuantity,unit,status,timeline,exportedAt\n");

        for (OpenDataSchema.ShipmentData s : schema.getShipments()) {
            String timelineJson;
            try {
                timelineJson = objectMapper.writeValueAsString(s.getTimeline());
            } catch (Exception e) {
                timelineJson = "[]";
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
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getEventTypeNameInVietnamese(ChainEventType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case HARVEST -> "Thu hoạch (HARVEST)";
            case PACKAGING -> "Đóng gói (PACKAGING)";
            case TRANSPORT -> "Vận chuyển (TRANSPORT)";
            case PROCUREMENT -> "Thu mua (PROCUREMENT)";
            default -> type.name();
        };
    }

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
}
