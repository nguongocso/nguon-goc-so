package vn.nguongocso.integration.apikey.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.event.ApiKeyQuotaThresholdEvent;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.notification.service.NotificationService;

/**
 * Dịch vụ cảnh báo khóa truy cập sắp hết hạn và sắp chạm hạn mức (NCL-12-CN-005, QTN-20).
 * <p>
 * Nguyên tắc: quét hằng ngày các khóa {@code ACTIVE}, cảnh báo một lần mỗi ngày cho
 * hết hạn và một lần mỗi ngày cho hạn mức, bỏ qua khóa đã thu hồi/hết hạn (TC-03, TC-04).
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

    @Value("${app.apikey.expiry-warning-days:7}")
    private int expiryWarningDays;

    @Value("${app.apikey.quota-warning-ratio:0.8}")
    private double quotaWarningRatio;

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
                                    + "). Vui lòng gia hạn để đối tác không bị gián đoạn kết nối. "
                                    + "Xem chi tiết tại Quản trị khóa truy cập.",
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
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            if (notificationRepository.existsByEntityIdAndTitleAndCreatedAtAfter(
                    event.getApiKeyId(), QUOTA_TITLE, startOfDay)) {
                return;
            }
            int percent = (int) Math.round(event.getUsedCalls() * 100.0 / event.getRateLimitPerHour());
            notificationService.sendHandoverNotification(
                    QUOTA_TITLE,
                    "Khóa truy cập của đối tác \"" + event.getPartnerName() + "\" đã dùng "
                            + event.getUsedCalls() + "/" + event.getRateLimitPerHour()
                            + " lượt gọi trong ngày hôm nay (đạt " + percent
                            + "%). Vui lòng nâng hạn mức hoặc chờ sang giờ tiếp theo. "
                            + "Xem chi tiết tại Quản trị khóa truy cập.",
                    event.getApiKeyId(),
                    event.getOrganizationId());
        } catch (Exception e) {
            log.warn("Bỏ qua lỗi gửi cảnh báo hạn mức cho khóa {}", event.getApiKeyId(), e);
        }
    }
}
