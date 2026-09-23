package vn.nguongocso.integration.apikey.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.integration.apikey.entity.PartnerApiKeyDailyUsage;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyDailyUsageRepository;

/**
 * Ghi dữ liệu bộ đếm lượt gọi theo ngày trong transaction riêng.
*/
@Component
@RequiredArgsConstructor
public class PartnerApiKeyUsageWriter {
    private final PartnerApiKeyDailyUsageRepository usageRepository;

    /**
     * Cộng thêm một lượt gọi cho khóa trong ngày.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int incrementInNewTransaction(UUID apiKeyId, LocalDate usageDate) {
        LocalDateTime now = LocalDateTime.now();

        if (usageRepository.incrementCallCount(apiKeyId, usageDate, now) > 0) {
            return readCount(apiKeyId, usageDate);
        }

        usageRepository.saveAndFlush(PartnerApiKeyDailyUsage.builder()
                .apiKeyId(apiKeyId)
                .usageDate(usageDate)
                .callCount(1)
                .build());

        return readCount(apiKeyId, usageDate);
    }

    /**
     * Giành quyền gửi cảnh báo hạn mức của một dòng usage trong transaction riêng.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimQuotaWarningInNewTransaction(UUID usageId) {
        return usageRepository.claimWarning(usageId, LocalDateTime.now()) == 1;
    }

    /**
     * Nhả quyền gửi cảnh báo để lần đối soát sau gửi lại.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseQuotaWarningInNewTransaction(UUID usageId) {
        usageRepository.releaseWarning(usageId, LocalDateTime.now());
    }

    /**
     * Đọc tổng số lượt gọi của khóa trong ngày.
     */
    private int readCount(UUID apiKeyId, LocalDate usageDate) {
        return usageRepository.findByApiKeyIdAndUsageDate(apiKeyId, usageDate)
                .map(PartnerApiKeyDailyUsage::getCallCount)
                .orElse(0);
    }
}
