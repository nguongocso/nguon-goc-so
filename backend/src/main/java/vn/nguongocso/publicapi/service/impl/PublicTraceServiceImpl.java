package vn.nguongocso.publicapi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.alert.service.ScanAnomalyDetectionService;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.enums.CertificationStatus;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.publicapi.dto.response.PublicCertificationResponse;
import vn.nguongocso.publicapi.dto.response.PublicChainEventItem;
import vn.nguongocso.publicapi.dto.response.PublicLotCertificationsResponse;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.PublicTraceService;
import vn.nguongocso.publicapi.service.ReverseGeocodingService;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.Recall;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.RecallRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
/** Cung cấp dữ liệu truy xuất công khai cho tem lô hàng. */
public class PublicTraceServiceImpl implements PublicTraceService {
    private final TraceCodeRepository traceCodeRepository;
    private final ChainEventRepository chainEventRepository;
    private final ObjectMapper objectMapper;
    private final TraceCodeScanLogRepository traceCodeScanLogRepository;
    private final ScanAnomalyDetectionService scanAnomalyDetectionService;
    private final RecallRepository recallRepository;
    private final ProductionLotCertificationRepository productionLotCertificationRepository;
    private final ReverseGeocodingService reverseGeocodingService;

    /** Lấy thông tin truy xuất công khai. */
    @Override
    public PublicTraceResponse getPublicTrace(String codeValue,
            Double latitude,
            Double longitude,
            String ipAddress,
            String userAgent) {

        // TC-02: Kiểm tra tồn tại mã
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new ResourceNotFoundException("Mã lô hàng không tồn tại."));

        // Lấy Shipment
        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new ResourceNotFoundException("Không tìm thấy lô hàng liên kết.");
        }

        // TC-04: Kiểm tra thu hồi
        boolean isRecalled = shipment.getStatus() == ShipmentStatus.RECALLED;

        String recallMessage = null;
        if (isRecalled) {
            recallMessage = recallRepository
                    .findTopByShipmentOrderByRecalledAtDesc(shipment)
                    .map(Recall::getReason)
                    .orElse("Lô hàng này đã bị thu hồi.");
        }

        // Check if trace code is LOCKED
        boolean isLocked = traceCode.getStatus() == TraceCodeStatus.LOCKED;

        // TC-03: Nếu chưa thu hồi và chưa bị khóa thì tem phải đang ACTIVE
        if (!isRecalled && !isLocked && traceCode.getStatus() != TraceCodeStatus.ACTIVE
                && traceCode.getStatus() != TraceCodeStatus.SUSPECT) {
            throw new BusinessException("Tem chưa có hiệu lực, chưa thể tra cứu hành trình.");
        }

        String resolvedLocation = "Không xác định";

        String location = null;

        if (latitude != null && longitude != null) {
            location = reverseGeocodingService.reverseGeocode(
                    latitude,
                    longitude);

            if (location != null && !location.isBlank()) {
                resolvedLocation = location;
            }
        }

        // Ghi nhận lượt quét
        TraceCodeScanLog scanLog = TraceCodeScanLog.builder()
                .traceCode(traceCode)
                .scannedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .latitude(latitude != null
                        ? BigDecimal.valueOf(latitude)
                        : null)
                .longitude(longitude != null
                        ? BigDecimal.valueOf(longitude)
                        : null)
                .location(resolvedLocation)
                .isAbnormal(false)
                .build();

        traceCodeScanLogRepository.save(scanLog);

        // Kiểm tra phát hiện quét bất thường
        scanAnomalyDetectionService.onScanRecorded(traceCode.getId());

        // Lấy dòng sự kiện của Shipment
        List<ChainEvent> shipmentEvents = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId());

        // Lấy dòng sự kiện của ProductionLot
        List<ChainEvent> productionLotEvents = Collections.emptyList();

        if (shipment.getProductionLot() != null) {
            UUID productionLotId = shipment.getProductionLot().getId();

            List<ChainEvent> allUnassignedEvents = chainEventRepository.findByShipmentIsNullAndEventTypeIn(
                    List.of(ChainEventType.HARVEST, ChainEventType.PACKAGING));

            productionLotEvents = allUnassignedEvents.stream()
                    .filter(event -> {
                        Map<String, Object> data = parseEventData(event.getEventData());
                        Object lotId = data.get("productionLotId");
                        return lotId != null
                                && productionLotId.toString().equals(lotId.toString());
                    })
                    .toList();
        }

        // Gộp timeline
        List<ChainEvent> allEvents = new ArrayList<>();
        allEvents.addAll(shipmentEvents);
        allEvents.addAll(productionLotEvents);
        allEvents.sort(Comparator.comparing(ChainEvent::getRecordedAt));

        List<PublicChainEventItem> publicEvents = allEvents.stream()
                .map(this::convertToPublicEvent)
                .toList();

        String productName = shipment.getProductionLot() != null
                ? shipment.getProductionLot().getName()
                : "Sản phẩm";

        return PublicTraceResponse.builder()
                .codeValue(traceCode.getCodeValue())
                .productionLotId(
                        shipment.getProductionLot() != null
                                ? shipment.getProductionLot().getId()
                                : null)
                .productName(productName)
                .shipmentCode(shipment.getId().toString())
                .shipmentStatus(shipment.getStatus().name())
                .recalled(isRecalled)
                .recallMessage(recallMessage)
                .locked(isLocked)
                .lockReason(traceCode.getLockReason())
                .lockedAt(traceCode.getLockedAt())
                .events(publicEvents)
                .build();
    }

    /** Chuyển một sự kiện nội bộ thành dữ liệu công khai. */
    private PublicChainEventItem convertToPublicEvent(ChainEvent event) {
        // Parse eventData JSON sang Map
        Map<String, Object> rawData = parseEventData(event.getEventData());
        // Lọc dữ liệu công khai
        Map<String, Object> filteredData = filterEventData(rawData, event.getEventType());

        // Trích xuất latitude, longitude từ location
        Double latitude = null;
        Double longitude = null;
        if (event.getLocation() != null) {
            latitude = event.getLocation().getY(); // JTS Point: getY() = latitude
            longitude = event.getLocation().getX(); // getX() = longitude
        }

        return PublicChainEventItem.builder()
                .eventType(event.getEventType().name())
                .eventData(filteredData)
                .recordedAt(event.getRecordedAt())
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    /** Parse JSON eventData thành map an toàn. */
    private Map<String, Object> parseEventData(String eventDataJson) {
        if (eventDataJson == null || eventDataJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(eventDataJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Không thể parse eventData: {}", eventDataJson, e);
            return new HashMap<>();
        }
    }

    /** Lọc trường dữ liệu được phép hiển thị công khai. */
    private Map<String, Object> filterEventData(Map<String, Object> rawData, ChainEventType eventType) {
        Map<String, Object> result = new HashMap<>();

        switch (eventType) {
            case HARVEST:
                keepFields(rawData, result, "productionLotName", "quantity", "harvestDate");
                break;
            case PACKAGING:
                keepFields(rawData, result, "productionLotName", "packagingSpecification", "packagingDate");
                break;
            case TRANSPORT:
                keepFields(rawData, result, "fromLocation", "toLocation", "transportDate");
                break;
            case PROCUREMENT:
                keepFields(rawData, result, "shipmentName", "receivedQuantity", "notes");
                break;
            default:
                // Chỉ giữ các trường an toàn, tránh lộ thông tin nội bộ
                result.putAll(rawData);
                result.remove("recordedBy");
                result.remove("createdAt");
                result.remove("updatedAt");
        }

        return result;
    }

    /** Giữ lại một tập trường dữ liệu cụ thể. */
    private void keepFields(Map<String, Object> source, Map<String, Object> target, String... fields) {
        for (String field : fields) {
            if (source.containsKey(field)) {
                target.put(field, source.get(field));
            }
        }
    }

    /** Lấy chứng nhận công khai của lô hàng. */
    @Override
    public PublicLotCertificationsResponse getPublicCertifications(String codeValue) {
        // 1. Tìm trace code
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new ResourceNotFoundException("Mã lô hàng không tồn tại."));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new ResourceNotFoundException("Không tìm thấy lô hàng liên kết.");
        }

        ProductionLot lot = shipment.getProductionLot();
        if (lot == null) {
            return PublicLotCertificationsResponse.builder()
                    .productionLotId(null)
                    .lotName(null)
                    .hasCertification(false)
                    .certifications(Collections.emptyList())
                    .build();
        }

        // 2. Lấy danh sách chứng nhận của lô sản xuất
        List<ProductionLotCertification> plCertifications = productionLotCertificationRepository
                .findByProductionLotId(lot.getId());

        LocalDate today = LocalDate.now();
        List<PublicCertificationResponse> certResponses = plCertifications.stream()
                .map(plc -> {
                    Certification cert = plc.getCertification();
                    CertificationStatus status;
                    String statusLabel;
                    if (cert.getExpiryDate().isBefore(today)) {
                        status = CertificationStatus.EXPIRED;
                        statusLabel = "Hết hạn";
                    } else {
                        status = CertificationStatus.VALID;
                        statusLabel = "Còn hiệu lực";
                    }
                    return PublicCertificationResponse.builder()
                            .certificationId(cert.getId())
                            .certificationName(lot.getName())
                            .certificationCode(cert.getCode())
                            .issuedBy(cert.getIssuedBy())
                            .issueDate(cert.getIssueDate())
                            .expiryDate(cert.getExpiryDate())
                            .status(status)
                            .statusLabel(statusLabel)
                            .build();
                })
                .collect(Collectors.toList());

        return PublicLotCertificationsResponse.builder()
                .productionLotId(lot.getId())
                .lotName(lot.getName())
                .hasCertification(!certResponses.isEmpty())
                .certifications(certResponses)
                .build();
    }
}
