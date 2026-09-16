package vn.nguongocso.integration.apikey.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import vn.nguongocso.integration.apikey.entity.PartnerApiKeyDailyUsage;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyDailyUsageRepository;

/**
 * Dịch vụ đọc/ghi bộ đếm lượt gọi theo ngày của khóa truy cập đối tác (NCL-12-CN-005).
 * <p>
 * Là nguồn sự thật cho ngưỡng cảnh báo hạn mức: số liệu được lưu ở DB nên cảnh
 * báo không mất khi khởi động lại backend và không gửi trùng khi chạy nhiều
 * instance. Mọi thao tác ghi đều đi qua {@link PartnerApiKeyUsageWriter} để nằm
 * trong transaction riêng, không ảnh hưởng transaction xử lý request của đối tác.
 */
@Service
@RequiredArgsConstructor
public class PartnerApiKeyUsageService {

    private static final Logger log = LoggerFactory.getLogger(PartnerApiKeyUsageService.class);

    private final PartnerApiKeyDailyUsageRepository usageRepository;
    private final PartnerApiKeyUsageWriter usageWriter;

    /**
     * Ngày nghiệp vụ hiện tại dùng làm khóa đếm theo ngày.
     */
    public LocalDate currentUsageDate() {
        return LocalDate.now();
    }

    /**
     * Ghi nhận một lượt gọi đã xác thực thành công và trả về tổng lượt gọi trong ngày.
     * <p>
     * Không bao giờ ném lỗi ra ngoài: nếu không ghi được (ví dụ tranh chấp tạo
     * dòng), trả về số lượt đã đọc được để không chặn request của đối tác; job
     * đối soát sẽ bù cảnh báo khi dữ liệu hoàn tất.
     *
     * @param apiKeyId ID khóa truy cập
     * @return số lượt gọi trong ngày hôm nay của khóa
     */
    public int recordCallAndGetDailyCount(UUID apiKeyId) {
        LocalDate today = currentUsageDate();
        try {
            return usageWriter.incrementInNewTransaction(apiKeyId, today);
        } catch (DataIntegrityViolationException e) {
            // Thua tranh chấp tạo dòng: instance khác vừa tạo dòng cho cùng khóa + ngày,
            // gọi lại chỉ còn bước cộng lượt gọi trên dòng đã có.
            log.debug("Dòng usage đã được tạo bởi tiến trình khác cho khóa {} ngày {}", apiKeyId, today);
            return usageWriter.incrementInNewTransaction(apiKeyId, today);
        } catch (RuntimeException e) {
            log.warn("Không ghi nhận được lượt gọi trong ngày cho khóa {}", apiKeyId, e);
            return getDailyCallCount(apiKeyId);
        }
    }

    /**
     * Số lượt gọi trong ngày hôm nay của một khóa (0 nếu chưa có dòng usage).
     */
    @Transactional(readOnly = true)
    public int getDailyCallCount(UUID apiKeyId) {
        if (apiKeyId == null) {
            return 0;
        }
        return getDailyCallCounts(List.of(apiKeyId)).getOrDefault(apiKeyId, 0);
    }

    /**
     * Số lượt gọi trong ngày hôm nay của nhiều khóa bằng một truy vấn duy nhất.
     *
     * @param apiKeyIds danh sách ID khóa truy cập
     * @return map ID khóa → số lượt gọi trong ngày (khóa không có dữ liệu sẽ không xuất hiện)
     */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> getDailyCallCounts(Collection<UUID> apiKeyIds) {
        if (apiKeyIds == null || apiKeyIds.isEmpty()) {
            return new HashMap<>();
        }
        List<PartnerApiKeyDailyUsage> rows = usageRepository.findByApiKeyIdInAndUsageDate(apiKeyIds, currentUsageDate());
        Map<UUID, Integer> result = new HashMap<>();
        for (PartnerApiKeyDailyUsage row : rows) {
            result.put(row.getApiKeyId(), row.getCallCount() == null ? 0 : row.getCallCount());
        }
        return result;
    }

    /**
     * Lấy dòng usage hôm nay của một khóa.
     */
    @Transactional(readOnly = true)
    public Optional<PartnerApiKeyDailyUsage> findTodayUsage(UUID apiKeyId) {
        return usageRepository.findByApiKeyIdAndUsageDate(apiKeyId, currentUsageDate());
    }

    /**
     * Các dòng usage trong ngày hôm nay chưa gửi cảnh báo hạn mức (phục vụ job đối soát).
     */
    @Transactional(readOnly = true)
    public List<PartnerApiKeyDailyUsage> findTodayUnwarnedUsages() {
        return usageRepository.findByUsageDateAndWarningSentAtIsNull(currentUsageDate());
    }

    /**
     * Giành quyền gửi cảnh báo hạn mức cho một dòng usage (chống trùng giữa nhiều instance).
     *
     * @return {@code true} nếu giành được quyền gửi
     */
    public boolean claimQuotaWarning(UUID usageId) {
        return usageWriter.claimQuotaWarningInNewTransaction(usageId);
    }

    /**
     * Nhả quyền gửi cảnh báo để lần đối soát sau gửi lại (dùng khi gửi thông báo thất bại).
     */
    public void releaseQuotaWarning(UUID usageId) {
        usageWriter.releaseQuotaWarningInNewTransaction(usageId);
    }
}