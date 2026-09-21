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
 * Ghi dữ liệu bộ đếm lượt gọi theo ngày trong transaction riêng (NCL-12-CN-005).
 * <p>
 * Vì sao phải tách bean + {@code REQUIRES_NEW}: các thao tác này chạy trong luồng
 * xử lý request của đối tác (đang có transaction riêng). Khi dòng usage của
 * khóa + ngày chưa tồn tại, việc chèn mới có thể vi phạm ràng buộc duy nhất nếu
 * nhiều instance cùng chèn. Nếu chèn trong transaction của request, lỗi đó sẽ
 * đánh dấu rollback-only cả transaction và làm hỏng request; transaction riêng
 * giúp cô lập lỗi và cho phép tầng gọi bắt lỗi rồi thử lại.
 * <p>
 * Các phương thức claim/nhả quyền cảnh báo cũng dùng transaction riêng để khóa
 * dòng được nhả ngay khi commit, tránh tự khóa chéo với transaction đang mở.
 */
@Component
@RequiredArgsConstructor
public class PartnerApiKeyUsageWriter {

    private final PartnerApiKeyDailyUsageRepository usageRepository;

    /**
     * Cộng thêm một lượt gọi cho khóa trong ngày (tạo dòng nếu chưa có).
     *
     * @param apiKeyId  ID khóa truy cập
     * @param usageDate ngày nghiệp vụ
     * @return tổng số lượt gọi trong ngày sau khi cộng
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int incrementInNewTransaction(UUID apiKeyId, LocalDate usageDate) {
        LocalDateTime now = LocalDateTime.now();

        if (usageRepository.incrementCallCount(apiKeyId, usageDate, now) > 0) {
            return readCount(apiKeyId, usageDate);
        }

        // Chưa có dòng usage cho khóa + ngày: tạo mới và tính lượt gọi hiện tại là lượt đầu tiên.
        usageRepository.saveAndFlush(PartnerApiKeyDailyUsage.builder()
                .apiKeyId(apiKeyId)
                .usageDate(usageDate)
                .callCount(1)
                .build());

        return readCount(apiKeyId, usageDate);
    }

    /**
     * Giành quyền gửi cảnh báo hạn mức của một dòng usage trong transaction riêng.
     *
     * @return {@code true} nếu giành được quyền gửi (chưa có nơi nào gửi)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimQuotaWarningInNewTransaction(UUID usageId) {
        return usageRepository.claimWarning(usageId, LocalDateTime.now()) == 1;
    }

    /**
     * Nhả quyền gửi cảnh báo để lần đối soát sau gửi lại (dùng khi gửi thông báo thất bại).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseQuotaWarningInNewTransaction(UUID usageId) {
        usageRepository.releaseWarning(usageId, LocalDateTime.now());
    }

    private int readCount(UUID apiKeyId, LocalDate usageDate) {
        return usageRepository.findByApiKeyIdAndUsageDate(apiKeyId, usageDate)
                .map(PartnerApiKeyDailyUsage::getCallCount)
                .orElse(0);
    }
}
