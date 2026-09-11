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
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.enums.CertificationStatus;
import vn.nguongocso.certification.enums.CertificationValidityStatus;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.publicapi.dto.response.PublicCertificationResponse;
import vn.nguongocso.publicapi.dto.response.PublicChainEventItem;
import vn.nguongocso.publicapi.dto.response.PublicInspectionCriterionResultDto;
import vn.nguongocso.publicapi.dto.response.PublicInspectionResponse;
import vn.nguongocso.publicapi.dto.response.PublicInspectionRoundDto;
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
import vn.nguongocso.trace.service.SuspectDetectionService;
import vn.nguongocso.recall.entity.RecallRequest;
import vn.nguongocso.recall.enums.RecallRequestStatus;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.recall.entity.BulkRecallShipment;
import vn.nguongocso.recall.enums.BulkRecallRequestStatus;
import vn.nguongocso.recall.repository.BulkRecallShipmentRepository;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.trace.recall.repository.RecallCaseRepository;

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
    private final SuspectDetectionService suspectDetectionService;
    private final RecallRepository recallRepository;
    private final RecallRequestRepository recallRequestRepository;
    private final RecallCaseRepository recallCaseRepository;
    private final BulkRecallShipmentRepository bulkRecallShipmentRepository;
    private final ProductionLotCertificationRepository productionLotCertificationRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final InspectionCriterionResultRepository inspectionCriterionResultRepository;
    private final ReverseGeocodingService reverseGeocodingService;

    /**
     * Lấy thông tin truy xuất công khai (đọc thuần túy).
     * <p>
     * Không tạo TraceCodeScanLog, không tăng lượt quét và không kích hoạt
     * đánh giá nghi vấn NCL-08-CN-007.
     */
    @Override
    public PublicTraceResponse getPublicTrace(String codeValue,
            Double latitude,
            Double longitude,
            String ipAddress,
            String userAgent) {

        return buildPublicTraceResponse(codeValue);
    }

    /**
     * Ghi nhận lượt quét mã QR thực tế.
     * <p>
     * Tạo TraceCodeScanLog, kích hoạt phát hiện bất thường (gồm cả đánh giá
     * nghi vấn NCL-08-CN-007), sau đó trả về thông tin truy xuất công khai.
     */
    @Override
    public PublicTraceResponse recordPublicScan(String codeValue,
            Double latitude,
            Double longitude,
            String ipAddress,
            String userAgent) {

        // TC-02: Kiểm tra tồn tại mã
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new ResourceNotFoundException("Mã lô hàng không tồn tại."));

        // TC-04/TC-03: Kiểm tra thu hồi / trạng thái tem
        validateTraceCodeLookup(traceCode);

        String resolvedLocation = "Không xác định";

        if (latitude != null && longitude != null) {
            String location = reverseGeocodingService.reverseGeocode(
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

        // Đánh giá nghi vấn (NCL-08-CN-007) — giữ nguyên thứ tự thực thi cũ:
        // trước đây evaluateSuspicion được gọi đầu tiên bên trong onScanRecorded.
        suspectDetectionService.evaluateSuspicion(traceCode.getId());

        // Kiểm tra phát hiện quét bất thường (NCL-08-CN-001)
        scanAnomalyDetectionService.onScanRecorded(traceCode.getId());

        return buildPublicTraceResponse(codeValue);
    }

    /**
     * Xây dựng thông tin truy xuất công khai sau khi đã kiểm tra tồn tại mã,
     * trạng thái tem và lô hàng.
     */
    private PublicTraceResponse buildPublicTraceResponse(String codeValue) {
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new ResourceNotFoundException("Mã lô hàng không tồn tại."));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new ResourceNotFoundException("Không tìm thấy lô hàng liên kết.");
        }

        validateTraceCodeLookup(traceCode);

        boolean isRecalled = shipment.getStatus() == ShipmentStatus.RECALLED
                || shipment.getStatus() == ShipmentStatus.RECALLING;

        String recallMessage = null;
        if (isRecalled) {
            recallMessage = resolveRecallMessage(shipment);
        }

        boolean isLocked = traceCode.getStatus() == TraceCodeStatus.LOCKED;

        // Lấy dòng sự kiện của Shipment
        List<ChainEvent> shipmentEvents = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId());

        List<ChainEvent> sourceShipmentEvents = Collections.emptyList();
        if (shipment.getParentShipment() != null) {
            Shipment parentShipment = shipment.getParentShipment();
            LocalDateTime splitAt = shipment.getSplitAt();
            sourceShipmentEvents = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(parentShipment.getId())
                    .stream().filter(event -> splitAt == null || event.getRecordedAt() == null
                            || !event.getRecordedAt().isAfter(splitAt)).toList();
        }

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
        allEvents.addAll(sourceShipmentEvents);
        allEvents.addAll(shipmentEvents);
        allEvents.addAll(productionLotEvents);
        allEvents.sort(Comparator.comparing(ChainEvent::getRecordedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<PublicChainEventItem> publicEvents = allEvents.stream()
                .map(this::convertToPublicEvent)
                .toList();

        ProductionLot productionLot = shipment.getProductionLot();
        String lotName = productionLot != null ? productionLot.getName() : null;
        String lotCode = (productionLot != null && productionLot.getId() != null)
                ? productionLot.getId().toString()
                : null;
        String productName = (productionLot != null && productionLot.getProductCategory() != null)
                ? productionLot.getProductCategory().getName()
                : (productionLot != null ? productionLot.getName() : "Sản phẩm");
        String shipmentCode = (shipment.getName() != null && !shipment.getName().isBlank())
                ? shipment.getName()
                : shipment.getId().toString();

        List<PublicInspectionCriterionResultDto> inspectionResults =
                resolveLatestResults(fetchPublicInspectionRounds(productionLot));

        return PublicTraceResponse.builder()
                .codeValue(traceCode.getCodeValue())
                .productionLotId(
                        productionLot != null
                                ? productionLot.getId()
                                : null)
                .lotName(lotName)
                .lotCode(lotCode)
                .productName(productName)
                .shipmentCode(shipmentCode)
                .shipmentStatus(shipment.getStatus() != null ? shipment.getStatus().name() : "UNKNOWN")
                .recalled(isRecalled)
                .recallMessage(recallMessage)
                .locked(isLocked)
                .lockReason(traceCode.getLockReason())
                .lockedAt(traceCode.getLockedAt())
                .verificationNote(traceCode.getVerificationNote())
                .unlockedAt(traceCode.getUnlockedAt())
                .events(publicEvents)
                .inspections(inspectionResults)
                .build();
    }

    /**
     * Xác định thông điệp thu hồi hiển thị công khai.
     *
     * <p>
     * Ưu tiên lấy lý do từ yêu cầu thu hồi lô sản xuất đã được duyệt
     * (NCL-08-CN-008) theo lô sản xuất của lô hàng. Nếu không có, fallback
     * về lý do từ bản ghi thu hồi lô hàng cũ (NCL-08-CN-003).
     * </p>
     *
     * @param shipment lô hàng đang bị thu hồi
     * @return thông điệp thu hồi
     */
    private String resolveRecallMessage(Shipment shipment) {
        // Nếu shipment thuộc case CLOSED → trả thông điệp đã xử lý xong (TC-03, NCL-08-CN-012)
        List<RecallCase> closedCases = recallCaseRepository.findClosedByShipmentId(
                shipment.getId(), RecallCaseStatus.CLOSED);
        if (!closedCases.isEmpty()) {
            RecallCase closedCase = closedCases.get(0);
            String dateStr = (closedCase.getClosedAt() != null)
                    ? closedCase.getClosedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "";
            return "LÔ HÀNG ĐÃ XỬ LÝ XONG. Vụ việc thu hồi đã đóng ngày " + dateStr + ".";
        }

        // Kiểm tra yêu cầu thu hồi hàng loạt đã duyệt (NCL-08-CN-011)
        Optional<BulkRecallShipment> bulkRecallShipment = bulkRecallShipmentRepository
                .findTopByShipment_IdAndIncludedAndBulkRecallRequest_StatusOrderByCreatedAtDesc(
                        shipment.getId(), true, BulkRecallRequestStatus.APPROVED);
        if (bulkRecallShipment.isPresent()) {
            String reason = bulkRecallShipment.get().getBulkRecallRequest().getReason();
            if (shipment.getStatus() == ShipmentStatus.RECALLING) {
                return "CẢNH BÁO: Lô hàng đang trong quá trình thu hồi. Lý do: " + reason;
            }
            return reason;
        }

        Optional<RecallRequest> approvedRequest = recallRequestRepository
                .findTopByShipment_IdAndStatusOrderByApprovedAtDesc(
                        shipment.getId(), RecallRequestStatus.APPROVED);
        if (approvedRequest.isPresent()) {
            String reason = approvedRequest.get().getReason();
            if (shipment.getStatus() == ShipmentStatus.RECALLING) {
                return "CẢNH BÁO: Lô hàng đang trong quá trình thu hồi. Lý do: " + reason;
            }
            return reason;
        }

        return recallRepository
                .findTopByShipmentOrderByRecalledAtDesc(shipment)
                .map(r -> shipment.getStatus() == ShipmentStatus.RECALLING
                        ? "CẢNH BÁO: Lô hàng đang trong quá trình thu hồi. Lý do: " + r.getReason()
                        : r.getReason())
                .orElse(shipment.getStatus() == ShipmentStatus.RECALLING
                        ? "CẢNH BÁO: Lô hàng đang trong quá trình thu hồi."
                        : "Lô hàng này đã bị thu hồi.");
    }

    /**
     * Kiểm tra trạng thái hợp lệ của tem để tra cứu/ghi nhận quét.
     */
    private void validateTraceCodeLookup(TraceCode traceCode) {
        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new ResourceNotFoundException("Không tìm thấy lô hàng liên kết.");
        }

        if (traceCode.getStatus() == TraceCodeStatus.CANCELLED) {
            throw new BusinessException("Mã tem này đã được đánh dấu HỦY do sự cố in hỏng/lỗi tem và không có giá trị truy xuất nguồn gốc.");
        }

        boolean isRecalled = shipment.getStatus() == ShipmentStatus.RECALLED
                || shipment.getStatus() == ShipmentStatus.RECALLING;
        boolean isLocked = traceCode.getStatus() == TraceCodeStatus.LOCKED;

        if (!isRecalled && !isLocked && traceCode.getStatus() != TraceCodeStatus.ACTIVE
                && traceCode.getStatus() != TraceCodeStatus.SUSPECT) {
            throw new BusinessException("Tem chưa có hiệu lực, chưa thể tra cứu hành trình.");
        }
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
                .eventType(event.getEventType() != null ? event.getEventType().name() : "UNKNOWN")
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
        if (eventType == null) {
            result.putAll(rawData);
            result.remove("recordedBy");
            result.remove("createdAt");
            result.remove("updatedAt");
            return result;
        }

        switch (eventType) {
            case HARVEST:
                // B-03: Public trace chỉ hiển thị cờ earlyHarvest, eligibleHarvestDate, unmatchedMaterials; KHÔNG expose earlyHarvestReason
                keepFields(rawData, result, "productionLotName", "quantity", "harvestDate", "earlyHarvest", "eligibleHarvestDate", "unmatchedMaterials");
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
                .filter(plc -> plc.getCertification() != null
                        && plc.getCertification().getVerificationStatus() != CertificationVerificationStatus.REJECTED)
                .map(plc -> {
                    Certification cert = plc.getCertification();
                    boolean isExpired = cert.getExpiryDate().isBefore(today);
                    CertificationValidityStatus validityStatus = isExpired
                            ? CertificationValidityStatus.EXPIRED
                            : CertificationValidityStatus.VALID;

                    String publicStatus;
                    String statusLabel;
                    CertificationStatus legacyStatus = isExpired
                            ? CertificationStatus.EXPIRED
                            : CertificationStatus.VALID;

                    if (cert.getVerificationStatus() == CertificationVerificationStatus.PENDING) {
                        publicStatus = "PENDING_VERIFICATION";
                        statusLabel = "Đang chờ xác thực";
                    } else {
                        // VERIFIED
                        if (isExpired) {
                            publicStatus = "EXPIRED";
                            statusLabel = "Đã hết hạn";
                        } else {
                            publicStatus = "VERIFIED";
                            statusLabel = "Đã đạt chuẩn";
                        }
                    }

                    return PublicCertificationResponse.builder()
                            .certificationId(cert.getId())
                            .certificationName(cert.getName())
                            .certificationCode(cert.getCode())
                            .issuedBy(cert.getIssuedBy())
                            .issueDate(cert.getIssueDate())
                            .expiryDate(cert.getExpiryDate())
                            .verificationStatus(cert.getVerificationStatus())
                            .validityStatus(validityStatus)
                            .publicStatus(publicStatus)
                            .status(legacyStatus)
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

    /**
     * Lấy danh sách kết quả kiểm nghiệm công khai của lô hàng.
     */
    @Override
    public PublicInspectionResponse getPublicInspections(String codeValue) {
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new ResourceNotFoundException("Mã lô hàng không tồn tại."));

        Shipment shipment = traceCode.getShipment();
        if (shipment == null) {
            throw new ResourceNotFoundException("Không tìm thấy lô hàng liên kết.");
        }

        ProductionLot lot = shipment.getProductionLot();
        if (lot == null) {
            return PublicInspectionResponse.builder()
                    .productionLotId(null)
                    .lotName(null)
                    .hasInspection(false)
                    .totalCriteria(0)
                    .passedCriteria(0)
                    .failedCriteriaCount(0)
                    .failedRatio(0.0)
                    .roundCount(0)
                    .inspections(Collections.emptyList())
                    .history(Collections.emptyList())
                    .build();
        }

        /*
         * Lịch sử kiểm nghiệm theo từng lần gửi mẫu (cũ → mới).
         * Mỗi lần kiểm nghiệm (kể cả kiểm nghiệm lại) giữ nguyên trong
         * lịch sử — kết quả không đạt của lần trước KHÔNG bị xóa.
         */
        List<PublicInspectionRoundDto> history = fetchPublicInspectionRounds(lot);

        /*
         * Trạng thái HIỆN TẠI của từng chỉ tiêu = kết quả MỚI NHẤT.
         * FAIL lịch sử đã được khắc phục bằng kết quả kiểm nghiệm lại
         * sẽ không còn xuất hiện trong danh sách hiện tại.
         */
        List<PublicInspectionCriterionResultDto> currentResults =
                resolveLatestResults(history);

        /*
         * Thống kê tổng hợp tính trên TRẠNG THÁI HIỆN TẠI của lô:
         * totalCriteria bằng số chỉ tiêu đã có kết quả mới nhất.
         */
        int totalCriteria = currentResults.size();
        int failedCriteriaCount = (int) currentResults.stream()
                .filter(dto -> !Boolean.TRUE.equals(dto.getPassed()))
                .count();
        int passedCriteria = totalCriteria - failedCriteriaCount;

        return PublicInspectionResponse.builder()
                .productionLotId(lot.getId())
                .lotName(lot.getName())
                .hasInspection(!currentResults.isEmpty())
                .totalCriteria(totalCriteria)
                .passedCriteria(passedCriteria)
                .failedCriteriaCount(failedCriteriaCount)
                .failedRatio(computeFailedRatio(failedCriteriaCount, totalCriteria))
                .roundCount(history.size())
                .inspections(currentResults)
                .history(history)
                .build();
    }

    /**
     * Tính tỷ lệ phần trăm chỉ tiêu không đạt trên TỔNG số chỉ tiêu,
     * làm tròn 1 chữ số thập phân.
     *
     * Trả về 0.0 khi tổng số chỉ tiêu là 0 (tránh chia cho 0).
     *
     * @param failedCriteriaCount Số chỉ tiêu không đạt.
     * @param totalCriteria Tổng số chỉ tiêu.
     * @return Tỷ lệ không đạt theo %, ví dụ 40.0.
     */
    private double computeFailedRatio(
            int failedCriteriaCount,
            int totalCriteria) {

        if (totalCriteria <= 0) {
            return 0.0;
        }

        return Math.round(
                failedCriteriaCount * 1000.0 / totalCriteria)
                / 10.0;
    }

    /**
     * Helper truy vấn và chuyển đổi lịch sử kiểm nghiệm của lô sản xuất
     * thành danh sách các LẦN kiểm nghiệm (round), sắp xếp từ cũ đến mới.
     */
    private List<PublicInspectionRoundDto> fetchPublicInspectionRounds(
            ProductionLot lot) {
        if (lot == null || lot.getId() == null) {
            return Collections.emptyList();
        }

        List<InspectionRequest> requests = inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lot.getId());

        // Repo trả mới → cũ; dòng thời gian hiển thị cũ → mới
        Collections.reverse(requests);

        List<PublicInspectionRoundDto> rounds = new ArrayList<>();

        int roundNumber = 0;
        for (InspectionRequest request : requests) {
            roundNumber++;

            List<PublicInspectionCriterionResultDto> roundResults =
                    fetchRoundResults(request);

            int passed = (int) roundResults.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getPassed()))
                    .count();
            int failed = roundResults.size() - passed;

            rounds.add(PublicInspectionRoundDto.builder()
                    .round(roundNumber)
                    .laboratoryName(resolveLaboratoryName(request))
                    .sampleSentDate(request.getSampleSentDate())
                    .status(request.getStatus() != null
                            ? request.getStatus().name()
                            : null)
                    .totalCriteria(roundResults.size())
                    .passedCriteria(passed)
                    .failedCriteriaCount(failed)
                    .results(roundResults)
                    .build());
        }

        return rounds;
    }

    /**
     * Kết quả chi tiết các chỉ tiêu của MỘT lần kiểm nghiệm.
     */
    private List<PublicInspectionCriterionResultDto> fetchRoundResults(
            InspectionRequest request) {

        List<InspectionCriterionResult> results =
                inspectionCriterionResultRepository
                        .findByInspectionCriterion_InspectionRequest_Id(
                                request.getId());

        List<PublicInspectionCriterionResultDto> dtos = new ArrayList<>();
        for (InspectionCriterionResult result : results) {
            dtos.add(toPublicResultDto(result, request));
        }
        return dtos;
    }

    /**
     * Trạng thái HIỆN TẠI của lô: lấy kết quả MỚI NHẤT của từng chỉ tiêu
     * từ lịch sử (lần kiểm nghiệm sau đè lên lần trước theo tên chỉ tiêu).
     */
    private List<PublicInspectionCriterionResultDto> resolveLatestResults(
            List<PublicInspectionRoundDto> history) {

        Map<String, PublicInspectionCriterionResultDto> latestByName =
                new LinkedHashMap<>();

        // Duyệt từ lần MỚI NHẤT về cũ; giữ kết quả gặp đầu tiên
        for (int i = history.size() - 1; i >= 0; i--) {
            for (PublicInspectionCriterionResultDto dto
                    : history.get(i).getResults()) {
                latestByName.putIfAbsent(dto.getCriterionName(), dto);
            }
        }

        return new ArrayList<>(latestByName.values());
    }

    /**
     * Chuyển một kết quả kiểm nghiệm thành DTO công khai.
     */
    private PublicInspectionCriterionResultDto toPublicResultDto(
            InspectionCriterionResult result,
            InspectionRequest request) {

        InspectionCriterion criterion = result.getInspectionCriterion();
        String criterionName = (criterion != null && criterion.getCriterionName() != null)
                ? criterion.getCriterionName()
                : "Chỉ tiêu kiểm nghiệm";

        String standardValue = "QCVN / TCCS";
        if (criterion != null && criterion.getStandard() != null && criterion.getStandard().getName() != null) {
            standardValue = criterion.getStandard().getName();
        }

        String measuredValue = Boolean.TRUE.equals(result.getPassed())
                ? "Đạt chuẩn (Trong ngưỡng an toàn)"
                : "Không đạt (Vượt ngưỡng quy định)";

        String inspectorName = (result.getCreatedBy() != null && result.getCreatedBy().getFullName() != null)
                ? result.getCreatedBy().getFullName()
                : null;

        return PublicInspectionCriterionResultDto.builder()
                .id(result.getId() != null ? result.getId().toString() : UUID.randomUUID().toString())
                .criterionName(criterionName)
                .standardValue(standardValue)
                .measuredValue(measuredValue)
                .passed(result.getPassed())
                .inspectorName(inspectorName)
                .inspectionDate(result.getResultDate())
                .expiryDate(result.getExpiryDate())
                .laboratoryName(resolveLaboratoryName(request))
                .build();
    }

    /**
     * Tên phòng kiểm nghiệm hiển thị công khai cho một lần kiểm nghiệm.
     */
    private String resolveLaboratoryName(InspectionRequest request) {
        return (request.getInspectionUnit() != null
                && !request.getInspectionUnit().isBlank())
                ? request.getInspectionUnit()
                : "Phòng kiểm nghiệm đạt chuẩn";
    }
}
