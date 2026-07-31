package vn.nguongocso.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.notification.NotificationService;
import vn.nguongocso.report.dto.response.LookupResponse;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.report.service.PublicLookupService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicLookupServiceImpl implements PublicLookupService {

    private final TraceCodeRepository traceCodeRepository;
    private final TraceCodeScanLogRepository traceCodeScanLogRepository;
    private final FarmLogRepository farmLogRepository;
    private final FarmLogAttachmentRepository farmLogAttachmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public LookupResponse lookupCode(String codeValue, Double latitude, Double longitude, String location, String ipAddress, String userAgent) {
        // 1. Tìm mã truy xuất
        TraceCode traceCode = traceCodeRepository.findByCodeValue(codeValue)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã truy xuất: " + codeValue));

        // 2. Ghi nhận lượt quét
        TraceCodeScanLog scanLog = TraceCodeScanLog.builder()
                .traceCode(traceCode)
                .scannedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .latitude(latitude != null ? BigDecimal.valueOf(latitude) : null)
                .longitude(longitude != null ? BigDecimal.valueOf(longitude) : null)
                .location(location != null && !location.trim().isEmpty() ? location : "Không xác định")
                .isAbnormal(false)
                .build();

        // 3. Phân tích phát hiện bất thường (Rule QTN-10)
        detectAnomalies(traceCode, scanLog, codeValue);

        // Lưu nhật ký quét
        traceCodeScanLogRepository.save(scanLog);

        // 4. Kiểm tra trạng thái mã kích hoạt hay chưa
        if (traceCode.getStatus() == TraceCodeStatus.INACTIVE) {
            throw new BusinessException("Mã truy xuất chưa được kích hoạt.");
        }
        if (traceCode.getStatus() == TraceCodeStatus.RECALLED) {
            throw new BusinessException("Mã truy xuất đã bị thu hồi.");
        }

        // 5. Build DTO trả về thông tin chi tiết
        Shipment shipment = traceCode.getShipment();
        var productionLot = shipment.getProductionLot();
        var organization = shipment.getOrganization();

        // Lấy nhật ký canh tác
        List<FarmLog> farmLogs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(productionLot.getId());
        List<LookupResponse.FarmLogInfo> farmLogInfos = farmLogs.stream().map(fl -> {
            List<FarmLogAttachment> attachments = farmLogAttachmentRepository.findByFarmLogId(fl.getId());
            List<LookupResponse.AttachmentInfo> attachmentInfos = attachments.stream()
                    .map(att -> new LookupResponse.AttachmentInfo(att.getId(), att.getFileName()))
                    .collect(Collectors.toList());

            return LookupResponse.FarmLogInfo.builder()
                    .id(fl.getId())
                    .logDate(fl.getExecutedDate().toString())
                    .activityType(fl.getActivityType().name())
                    .description(fl.getNotes())
                    .attachments(attachmentInfos)
                    .build();
        }).collect(Collectors.toList());

        // Lấy lịch sử sự kiện chuỗi cung ứng
        List<ChainEvent> chainEvents = chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipment.getId());
        List<LookupResponse.ChainEventInfo> chainEventInfos = chainEvents.stream().map(ce ->
                LookupResponse.ChainEventInfo.builder()
                        .id(ce.getId())
                        .eventType(ce.getEventType().name())
                        .eventDate(ce.getRecordedAt())
                        .eventData(ce.getEventData()) // Sử dụng ce.getEventData()
                        .build()
        ).collect(Collectors.toList());


        return LookupResponse.builder()
                .codeValue(traceCode.getCodeValue())
                .status(traceCode.getStatus())
                .activatedAt(traceCode.getActivatedAt())
                .shipment(LookupResponse.ShipmentInfo.builder()
                        .id(shipment.getId())
                        .name(shipment.getName())
                        .packagingInfo(shipment.getPackagingInfo())
                        .totalQuantity(shipment.getTotalQuantity())
                        .build())
                .productionLot(LookupResponse.ProductionLotInfo.builder()
                        .id(productionLot.getId())
                        .name(productionLot.getName())
                        .plantingDate(productionLot.getPlantingDate() != null ? productionLot.getPlantingDate().toString() : null)
                        .harvestDate(productionLot.getHarvestDate() != null ? productionLot.getHarvestDate().toString() : null)
                        .cropType(productionLot.getProductCategory() != null ? productionLot.getProductCategory().getName() : null)
                        .organization(LookupResponse.OrgInfo.builder()
                                .id(organization.getOrganizationId())
                                .name(organization.getName())
                                .build())
                        .build())
                .farmLogs(farmLogInfos)
                .chainEvents(chainEventInfos)
                .build();
    }

    private void detectAnomalies(TraceCode traceCode, TraceCodeScanLog currentScan, String codeValue) {
        LocalDateTime now = currentScan.getScannedAt();

        // Tiêu chí 1: Quét quá 10 lần trong 24 giờ
        long scans24h = traceCodeScanLogRepository.countByTraceCodeIdAndScannedAtAfter(traceCode.getId(), now.minusHours(24));
        if (scans24h >= 10) {
            currentScan.setIsAbnormal(true);
            currentScan.setAbnormalReason("Mã truy xuất bị quét quá giới hạn 10 lần trong vòng 24 giờ.");
            notificationService.sendAlert("Phát hiện mã bị quét quá nhiều lần (TraceCode: " + codeValue + ").");
            return;
        }

        // Tiêu chí 2: Impossible Travel (quét ở 2 nơi xa nhau > 100km trong 1 giờ)
        List<TraceCodeScanLog> recentScans = traceCodeScanLogRepository.findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(traceCode.getId(), now.minusHours(1));
        if (!recentScans.isEmpty()) {
            for (TraceCodeScanLog recentScan : recentScans) {
                // Kiểm tra tọa độ GPS
                if (currentScan.getLatitude() != null && currentScan.getLongitude() != null
                        && recentScan.getLatitude() != null && recentScan.getLongitude() != null) {

                    double distance = calculateDistanceInKm(
                            currentScan.getLatitude().doubleValue(),
                            currentScan.getLongitude().doubleValue(),
                            recentScan.getLatitude().doubleValue(),
                            recentScan.getLongitude().doubleValue()
                    );

                    if (distance > 100.0) {
                        currentScan.setIsAbnormal(true);
                        String reason = String.format("Quét ở 2 địa điểm cách xa nhau %.1f km trong vòng 1 giờ (Từ %s đến %s).",
                                distance, recentScan.getLocation(), currentScan.getLocation());
                        currentScan.setAbnormalReason(reason);
                        notificationService.sendAlert("Phát hiện quét bất thường về khoảng cách địa lý (TraceCode: " + codeValue + "). " + reason);
                        return;
                    }
                }
                // Kiểm tra bằng tên vị trí text nếu không có toạ độ GPS
                else if (currentScan.getLocation() != null && recentScan.getLocation() != null
                        && !"Không xác định".equalsIgnoreCase(currentScan.getLocation())
                        && !"Không xác định".equalsIgnoreCase(recentScan.getLocation())
                        && !currentScan.getLocation().equalsIgnoreCase(recentScan.getLocation())) {

                    currentScan.setIsAbnormal(true);
                    String reason = String.format("Quét ở 2 địa điểm khác nhau trong vòng 1 giờ (Từ %s đến %s).",
                            recentScan.getLocation(), currentScan.getLocation());
                    currentScan.setAbnormalReason(reason);
                    notificationService.sendAlert("Phát hiện quét bất thường về địa điểm (TraceCode: " + codeValue + "). " + reason);
                    return;
                }
            }
        }
    }

    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Bán kính Trái Đất (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
