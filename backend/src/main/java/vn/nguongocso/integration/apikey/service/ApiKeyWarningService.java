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
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.notification.service.NotificationService;

/**
 * Dịch vụ cảnh báo khóa truy cập sắp hết hạn và sắp chạm hạn mức (NCL-12-CN-005, QTN-20).
 * <p>
 * Nguyên tắc: quét hằng ngày các khóa {@code ACTIVE} để cảnh báo sắp hết hạn; cảnh
 * báo hạn mức bắn ngay khi lượt gọi trong ngày chạm ngưỡng và được gửi bù bởi job
 * đối soát. Mỗi khóa chỉ nhận một cảnh báo hạn mức trong ngày nhờ cờ claim
 * {@code warning_sent_at} ở DB; khóa đã thu hồi/hết hạn bị bỏ qua (TC-03, TC-04).
 * Thông báo tái dùng hạ tầng hộp thư NCL-08-CN-005 (không tạo endpoint mới).
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

    @Value("${app.apikey.expiry-warning-days:7}")
    private int expiryWarningDays;

    /**
     * Quét hằng ngày: persist khóa đã quá hạn thành {@code EXPIRED} (để filter theo
     * trạng thái ở tầng DB hoạt động đúng) và gửi cảnh báo sắp hết hạn cho các khóa
     * còn hiệu lực nằm trong ngưỡng cấu hình.
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
     * Nhận sự kiện chạm ngưỡng hạn mức và gửi cảnh báo một lần mỗi ngày cho mỗi khóa.
     * <p>
     * Không bao giờ ném lỗi ra ngoài để tránh chặn request của đối tác.
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
     * Job đối soát hạn mức: quét usage hôm nay chưa gửi cảnh báo mà đã vượt ngưỡng
     * và gửi bù (NCL-12-CN-005).
     * <p>
     * Bù cho các trường hợp cảnh báo realtime bị mất: backend vừa khởi động lại,
     * chạy nhiều instance, hoặc bộ đếm đã vượt ngưỡng mà không trúng mốc bắn.
     * Vẫn đảm bảo mỗi khóa chỉ nhận một cảnh báo trong ngày nhờ cờ claim ở DB.
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
            int usedCalls = usage.getCallCount() == null ? 0 : usage.getCallCount();
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
     * <p>
     * Cờ {@code warning_sent_at} ở DB đảm bảo chỉ một tiến trình gửi cho mỗi khóa
     * trong ngày; nếu gửi thông báo thất bại thì nhả cờ để lần đối soát sau thử lại.
     *
     * @return {@code true} nếu đã gửi cảnh báo trong lần gọi này
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
                                    + " lượt gọi trong ngày hôm nay (đạt " + percent
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
