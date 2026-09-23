package vn.nguongocso.integration.apikey.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.entity.PartnerApiKeyDailyUsage;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.event.ApiKeyQuotaThresholdEvent;
import vn.nguongocso.integration.apikey.event.ApiKeyLifecycleEvent;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.notification.service.NotificationService;

/**
 * Dịch vụ cảnh báo khóa truy cập sắp hết hạn và sắp chạm hạn mức.
*/
@Service
@RequiredArgsConstructor
public class ApiKeyWarningService {
    private static final Logger log = LoggerFactory.getLogger(ApiKeyWarningService.class);

    static final String EXPIRY_SOON_TITLE = "Khóa truy cập sắp hết hạn";
    static final String EXPIRED_TITLE = "Khóa truy cập đã hết hạn";
    static final String QUOTA_TITLE = "Khóa truy cập sắp chạm hạn mức";

    private static final DateTimeFormatter VI_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final PartnerApiKeyUsageService partnerApiKeyUsageService;
    private final ApiKeyQuotaPolicy apiKeyQuotaPolicy;
    private final PartnerApiKeyService partnerApiKeyService;

    @Value("${app.apikey.expiry-warning-days:7}")
    private int expiryWarningDays;

    /**
     * Quét hằng ngày các khóa sắp hết hạn và gửi cảnh báo.
     */
    @Transactional
    public void scanExpiringKeys() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningLimit = now.plusDays(expiryWarningDays);
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        List<PartnerApiKey> activeKeys = partnerApiKeyRepository.findByStatus(PartnerApiKeyStatus.ACTIVE);
        int expiredCount = 0;
        int warnedCount = 0;

        for (PartnerApiKey key : activeKeys) {
            if (key.getExpiresAt() == null) {
                continue;
            }
            if (!now.isBefore(key.getExpiresAt())) {
                key.setStatus(PartnerApiKeyStatus.EXPIRED);
                partnerApiKeyRepository.save(key);
                expiredCount++;
                if (!notificationRepository.existsByEntityIdAndTitleAndCreatedAtAfter(
                        key.getId(), EXPIRED_TITLE, startOfDay)) {
                    notificationService.sendHandoverNotification(
                            EXPIRED_TITLE,
                            "Khóa truy cập của đối tác \"" + key.getPartnerName() + "\" đã hết hạn vào "
                                    + key.getExpiresAt().format(VI_DATE_TIME)
                                    + ". Đối tác không gọi được cổng dữ liệu nữa. Vui lòng cấp khóa mới.",
                            key.getId(),
                            key.getOrganization().getOrganizationId());
                    warnedCount++;
                }
            } else if (!warningLimit.isBefore(key.getExpiresAt())) {
                if (!notificationRepository.existsByEntityIdAndTitleAndCreatedAtAfter(
                        key.getId(), EXPIRY_SOON_TITLE, startOfDay)) {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), key.getExpiresAt().toLocalDate());
                    notificationService.sendHandoverNotification(
                            EXPIRY_SOON_TITLE,
                            "Khóa truy cập của đối tác \"" + key.getPartnerName() + "\" sẽ hết hạn sau "
                                    + daysLeft + " ngày (vào " + key.getExpiresAt().format(VI_DATE_TIME)
                                    + "). Vui lòng gia hạn để đối tác không bị gián đoạn kết nối.",
                            key.getId(),
                            key.getOrganization().getOrganizationId());
                    warnedCount++;
                }
            }
        }

        log.info("Quét cảnh báo khóa truy cập: {} khóa ACTIVE, {} khóa chuyển EXPIRED, {} cảnh báo đã gửi",
                activeKeys.size(), expiredCount, warnedCount);
    }

    /**
     * Nhận sự kiện cấp hoặc gia hạn khóa và gửi cảnh báo sắp hết hạn.
     */
    @EventListener
    @Transactional
    public void handleApiKeyLifecycle(ApiKeyLifecycleEvent event) {
        try {
            if (event.getStatus() != PartnerApiKeyStatus.ACTIVE || event.getExpiresAt() == null) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            if (!now.isBefore(event.getExpiresAt())
                    || event.getExpiresAt().isAfter(now.plusDays(expiryWarningDays))) {
                return;
            }
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            if (notificationRepository.existsByEntityIdAndTitleAndCreatedAtAfter(
                    event.getApiKeyId(), EXPIRY_SOON_TITLE, startOfDay)) {
                return;
            }
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), event.getExpiresAt().toLocalDate());
            notificationService.sendHandoverNotification(
                    EXPIRY_SOON_TITLE,
                    "Khóa truy cập của đối tác \"" + event.getPartnerName() + "\" sẽ hết hạn sau "
                            + daysLeft + " ngày (vào " + event.getExpiresAt().format(VI_DATE_TIME)
                            + "). Vui lòng gia hạn để đối tác không bị gián đoạn kết nối.",
                    event.getApiKeyId(),
                    event.getOrganizationId());
        } catch (Exception e) {
            log.warn("Bỏ qua lỗi gửi cảnh báo sắp hết hạn cho khóa {}", event.getApiKeyId(), e);
        }
    }

    /**
     * Nhận sự kiện chạm ngưỡng hạn mức và gửi cảnh báo.
     */
    @EventListener
    @Transactional
    public void handleQuotaThreshold(ApiKeyQuotaThresholdEvent event) {
        try {
            partnerApiKeyUsageService.findTodayUsage(event.getApiKeyId())
                    .ifPresent(usage -> sendQuotaWarningIfNeeded(
                            event.getApiKeyId(),
                            usage.getId(),
                            event.getOrganizationId(),
                            event.getPartnerName(),
                            event.getRateLimitPerHour(),
                            event.getUsedCalls()));
        } catch (Exception e) {
            log.warn("Bỏ qua lỗi gửi cảnh báo hạn mức cho khóa {}", event.getApiKeyId(), e);
        }
    }

    /**
     * Đối soát hạn mức và gửi bù cảnh báo chưa được gửi.
     */
    @Transactional
    public void reconcileQuotaWarnings() {
        List<PartnerApiKeyDailyUsage> unwarnedUsages = partnerApiKeyUsageService.findTodayUnwarnedUsages();
        if (unwarnedUsages.isEmpty()) {
            log.info("Đối soát cảnh báo hạn mức: không có khóa nào cần gửi bù.");
            return;
        }

        List<UUID> keyIds = unwarnedUsages.stream()
                .map(PartnerApiKeyDailyUsage::getApiKeyId)
                .toList();
        Map<UUID, PartnerApiKey> keysById = partnerApiKeyRepository.findAllById(keyIds).stream()
                .collect(Collectors.toMap(PartnerApiKey::getId, key -> key));

        int sentCount = 0;
        for (PartnerApiKeyDailyUsage usage : unwarnedUsages) {
            PartnerApiKey key = keysById.get(usage.getApiKeyId());
            if (key == null || key.getStatus() != PartnerApiKeyStatus.ACTIVE) {
                continue;
            }
            int usedCalls = partnerApiKeyService.getCurrentHourCalls(key.getId());
            if (!apiKeyQuotaPolicy.isReached(usedCalls, key.getRateLimitPerHour())) {
                continue;
            }
            if (sendQuotaWarningIfNeeded(
                    key.getId(),
                    usage.getId(),
                    key.getOrganization().getOrganizationId(),
                    key.getPartnerName(),
                    key.getRateLimitPerHour(),
                    usedCalls)) {
                sentCount++;
            }
        }

        log.info("Đối soát cảnh báo hạn mức: {} dòng usage chưa cảnh báo, {} cảnh báo bù đã gửi.",
                unwarnedUsages.size(), sentCount);
    }

    /**
     * Gửi cảnh báo hạn mức nếu giành được quyền gửi cho dòng usage tương ứng.
     */
    private boolean sendQuotaWarningIfNeeded(UUID apiKeyId, UUID usageId, UUID organizationId,
            String partnerName, int rateLimitPerHour, int usedCalls) {
        if (usageId == null || !partnerApiKeyUsageService.claimQuotaWarning(usageId)) {
            return false;
        }

        int percent = rateLimitPerHour > 0 ? (int) Math.round(usedCalls * 100.0 / rateLimitPerHour) : 0;
        try {
            notificationService.sendHandoverNotification(
                    QUOTA_TITLE,
"Khóa truy cập của đối tác \"" + partnerName + "\" đã dùng "
                                    + usedCalls + "/" + rateLimitPerHour
                                    + " lượt gọi trong giờ hiện tại (đạt " + percent
                                    + "%, ngưỡng cảnh báo " + apiKeyQuotaPolicy.warningThresholdPercent()
                                    + "%). Vui lòng nâng hạn mức hoặc điều tiết tần suất gọi.",
                    apiKeyId,
                    organizationId);
            return true;
        } catch (Exception e) {
            partnerApiKeyUsageService.releaseQuotaWarning(usageId);
            log.warn("Gửi cảnh báo hạn mức thất bại cho khóa {}, sẽ gửi bù ở lần đối soát sau.", apiKeyId, e);
            return false;
        }
    }
}
