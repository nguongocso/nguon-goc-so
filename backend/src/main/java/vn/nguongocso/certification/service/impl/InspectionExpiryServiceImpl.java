package vn.nguongocso.certification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.certification.dto.response.InspectionScanResult;
import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.certification.enums.InspectionValidityStatus;
import vn.nguongocso.certification.service.InspectionExpiryService;
import vn.nguongocso.certification.service.InspectionValidityService;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.notification.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Triển khai dịch vụ quét và cảnh báo kết quả kiểm nghiệm sắp hết hạn hoặc đã hết hạn.
 * (NCL-11-CN-004)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InspectionExpiryServiceImpl implements InspectionExpiryService {

    private final ProductionLotRepository productionLotRepository;
    private final InspectionValidityService inspectionValidityService;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("${app.inspection.expiry-warning-threshold-days:15}")
    private int warningThresholdDays;

    private static final Set<ProductionLotStatus> EXCLUDED_LOT_STATUSES = Set.of(
            ProductionLotStatus.CANCELLED,
            ProductionLotStatus.DISPOSED,
            ProductionLotStatus.CLOSED,
            ProductionLotStatus.RECALLED
    );

    @Override
    @Transactional
    public InspectionScanResult scanAndAlertExpiringInspections() {
        return scanAndAlertExpiringInspections(LocalDate.now());
    }

    @Override
    @Transactional
    public InspectionScanResult scanAndAlertExpiringInspections(LocalDate today) {
        log.info("⏰ Bắt đầu quét kiểm tra kết quả kiểm nghiệm lô sản xuất cho ngày: {}, ngưỡng cảnh báo: {} ngày",
                today, warningThresholdDays);

        List<ProductionLot> allLots = productionLotRepository.findAll();

        int totalScanned = 0;
        int validCount = 0;
        int expiringCount = 0;
        int expiredCount = 0;
        int skippedCount = 0;
        int alertsCreated = 0;
        int notificationsSent = 0;

        for (ProductionLot lot : allLots) {
            // 1. Kiểm tra trạng thái lô bị loại trừ (CANCELLED, DISPOSED, CLOSED, RECALLED) -> bỏ qua (TC-04)
            if (lot.getStatus() != null && EXCLUDED_LOT_STATUSES.contains(lot.getStatus())) {
                log.debug("Bỏ qua lô {} do trạng thái {}", lot.getId(), lot.getStatus());
                skippedCount++;
                continue;
            }

            totalScanned++;

            // 2. Tính toán trạng thái hiệu lực kiểm nghiệm của lô theo ngày mốc 'today'
            InspectionValidityResponse validity = inspectionValidityService.calculateValidity(lot, today);

            InspectionValidityStatus status = validity.getStatus();

            if (status == InspectionValidityStatus.VALID) {
                validCount++;
                continue;
            }

            if (status != InspectionValidityStatus.EXPIRING && status != InspectionValidityStatus.EXPIRED) {
                // NOT_REQUIRED hoặc NO_VALID_RESULT -> không thuộc diện cảnh báo hết hạn
                continue;
            }

            if (status == InspectionValidityStatus.EXPIRING) {
                expiringCount++;
            } else {
                expiredCount++;
            }

            // 3. Kiểm tra điều kiện tem:
            // Nếu tất cả tem đã kích hoạt (totalStamps > 0 && inactiveStampCount == 0) -> bỏ qua (TC-05)
            // Lô đã xuất xưởng hết không còn tem lưu hành chịu ảnh hưởng của kết quả hết hạn
            boolean allStampsActivated = validity.getTotalStamps() != null
                    && validity.getTotalStamps() > 0
                    && (validity.getInactiveStampCount() == null || validity.getInactiveStampCount() == 0);

            if (allStampsActivated) {
                log.info("Bỏ qua cảnh báo cho lô {} ({}) vì tất cả tem đã được kích hoạt (total={})",
                        lot.getId(), lot.getName(), validity.getTotalStamps());
                skippedCount++;
                continue;
            }

            // 4. Kiểm tra idempotency: 1 lô + 1 loại cảnh báo + 1 ngày = tối đa 1 notification (TC-03)
            AlertType alertType = (status == InspectionValidityStatus.EXPIRED)
                    ? AlertType.INSPECTION_EXPIRED
                    : AlertType.INSPECTION_EXPIRING;

            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

            boolean alreadyAlerted = alertRepository.existsAlertToday(lot.getId(), alertType, startOfDay, endOfDay);
            if (alreadyAlerted) {
                log.info("Lô {} ({}) đã được tạo cảnh báo {} trong ngày {}, bỏ qua tạo trùng.",
                        lot.getId(), lot.getName(), alertType, today);
                skippedCount++;
                continue;
            }

            // 5. Nếu là EXPIRED, tự động chuyển các cảnh báo EXPIRING trước đó của lô về RESOLVED
            if (alertType == AlertType.INSPECTION_EXPIRED) {
                autoResolveExpiringAlert(lot.getId());
            }

            // 6. Tạo Alert bản ghi
            Alert alert = new Alert();
            alert.setId(UUID.randomUUID());
            alert.setType(alertType);
            alert.setRelatedEntityType("ProductionLot");
            alert.setRelatedEntityId(lot.getId());
            alert.setSeverity(alertType == AlertType.INSPECTION_EXPIRED ? AlertSeverity.HIGH : AlertSeverity.MEDIUM);
            alert.setStatus(AlertStatus.PENDING);
            alert.setOrganization(lot.getOrganization());
            alert.setCreatedAt(today.equals(LocalDate.now()) ? LocalDateTime.now() : today.atStartOfDay());

            String message = (alertType == AlertType.INSPECTION_EXPIRED)
                    ? String.format("Kết quả kiểm nghiệm của lô \"%s\" đã hết hiệu lực vào ngày %s.",
                            lot.getName(), validity.getExpiryDate() != null ? validity.getExpiryDate() : "N/A")
                    : String.format("Kết quả kiểm nghiệm của lô \"%s\" sắp hết hiệu lực sau %d ngày (ngày hết hạn: %s).",
                            lot.getName(),
                            validity.getDaysUntilExpiry() != null ? validity.getDaysUntilExpiry() : 0,
                            validity.getExpiryDate() != null ? validity.getExpiryDate() : "N/A");
            alert.setMessage(message);

            Map<String, Object> details = new HashMap<>();
            details.put("lotId", lot.getId() != null ? lot.getId().toString() : null);
            details.put("lotName", lot.getName());
            details.put("validityStatus", status.name());
            details.put("expiryDate", validity.getExpiryDate() != null ? validity.getExpiryDate().toString() : null);
            details.put("daysUntilExpiry", validity.getDaysUntilExpiry());
            details.put("inactiveStampCount", validity.getInactiveStampCount());
            details.put("totalStamps", validity.getTotalStamps());
            details.put("thresholdConfigured", warningThresholdDays);

            if (objectMapper != null) {
                try {
                    alert.setDetails(objectMapper.writeValueAsString(details));
                } catch (Exception e) {
                    log.error("Lỗi serialize details cho cảnh báo kiểm nghiệm lô {}: ", lot.getId(), e);
                }
            }

            alertRepository.save(alert);
            alertsCreated++;

            // 7. Gửi thông báo đến Quản lý HTX thuộc tổ chức của lô có quyền notification:READ (TC-08)
            try {
                notificationService.sendInspectionExpiryNotification(alert, lot, validity);
                notificationsSent++;
            } catch (Exception e) {
                log.error("Lỗi khi gửi thông báo kiểm nghiệm cho lô {}: ", lot.getId(), e);
            }
        }

        log.info("🏁 Hoàn thành quét kết quả kiểm nghiệm. Tổng quét: {}, Sắp hết hạn: {}, Đã hết hạn: {}, Đã tạo Alert: {}, Bỏ qua: {}",
                totalScanned, expiringCount, expiredCount, alertsCreated, skippedCount);

        return InspectionScanResult.builder()
                .totalLotsScanned(totalScanned)
                .validCount(validCount)
                .expiringCount(expiringCount)
                .expiredCount(expiredCount)
                .skippedCount(skippedCount)
                .alertsCreated(alertsCreated)
                .notificationsSent(notificationsSent)
                .scanDate(today)
                .build();
    }

    private void autoResolveExpiringAlert(UUID lotId) {
        if (lotId == null) {
            return;
        }
        List<Alert> pendingExpiringAlerts = alertRepository.findByRelatedEntityIdAndTypeAndStatus(
                lotId,
                AlertType.INSPECTION_EXPIRING,
                AlertStatus.PENDING);

        for (Alert alert : pendingExpiringAlerts) {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
            log.info("⚙️ Tự động RESOLVED cảnh báo sắp hết hiệu lực của lô ID: {}", lotId);
        }
    }
}
