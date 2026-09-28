package vn.nguongocso.integration.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import vn.nguongocso.integration.apikey.entity.PartnerApiKeyDailyUsage;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyDailyUsageRepository;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyUsageService;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyUsageWriter;

/**
 * Kiểm thử dịch vụ đếm lượt gọi theo ngày của khóa truy cập (NCL-12-CN-005).
 */
@ExtendWith(MockitoExtension.class)
class PartnerApiKeyUsageServiceTest {

    @Mock
    private PartnerApiKeyDailyUsageRepository usageRepository;

    @Mock
    private PartnerApiKeyUsageWriter usageWriter;

    @InjectMocks
    private PartnerApiKeyUsageService partnerApiKeyUsageService;

    private UUID apiKeyId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        apiKeyId = UUID.randomUUID();
        today = LocalDate.now();
    }

    @Test
    @DisplayName("Ghi nhận lượt gọi trả về tổng lượt trong ngày do writer cung cấp")
    void recordCallAndGetDailyCount_returnsWriterCount() {
        when(usageWriter.incrementInNewTransaction(apiKeyId, today)).thenReturn(8);

        assertEquals(8, partnerApiKeyUsageService.recordCallAndGetDailyCount(apiKeyId));
    }

    @Test
    @DisplayName("Thua tranh chấp tạo dòng thì thử lại và vẫn trả đúng số lượt")
    void recordCallAndGetDailyCount_retriesOnRace() {
        when(usageWriter.incrementInNewTransaction(apiKeyId, today))
                .thenThrow(new DataIntegrityViolationException("trùng dòng usage"))
                .thenReturn(3);

        assertEquals(3, partnerApiKeyUsageService.recordCallAndGetDailyCount(apiKeyId));
        verify(usageWriter, times(2)).incrementInNewTransaction(apiKeyId, today);
    }

    @Test
    @DisplayName("Lỗi không mong đợi không chặn request đối tác: chỉ đọc lại số lượt hiện có")
    void recordCallAndGetDailyCount_swallowsUnexpectedError() {
        when(usageWriter.incrementInNewTransaction(apiKeyId, today)).thenThrow(new RuntimeException("db down"));
        when(usageRepository.findByApiKeyIdInAndUsageDate(any(), eq(today))).thenReturn(List.of());

        assertEquals(0, partnerApiKeyUsageService.recordCallAndGetDailyCount(apiKeyId));
        verify(usageWriter, times(1)).incrementInNewTransaction(apiKeyId, today);
    }

    @Test
    @DisplayName("Đọc số lượt gọi trong ngày của nhiều khóa bằng một truy vấn")
    void getDailyCallCounts_mapsRows() {
        UUID otherKeyId = UUID.randomUUID();
        PartnerApiKeyDailyUsage first = PartnerApiKeyDailyUsage.builder()
                .id(UUID.randomUUID())
                .apiKeyId(apiKeyId)
                .usageDate(today)
                .callCount(8)
                .build();
        PartnerApiKeyDailyUsage second = PartnerApiKeyDailyUsage.builder()
                .id(UUID.randomUUID())
                .apiKeyId(otherKeyId)
                .usageDate(today)
                .callCount(null)
                .build();
        when(usageRepository.findByApiKeyIdInAndUsageDate(any(), eq(today)))
                .thenReturn(List.of(first, second));

        Map<UUID, Integer> counts = partnerApiKeyUsageService.getDailyCallCounts(List.of(apiKeyId, otherKeyId));

        assertEquals(8, counts.get(apiKeyId));
        assertEquals(0, counts.get(otherKeyId));
    }

    @Test
    @DisplayName("Danh sách rỗng thì không truy vấn DB")
    void getDailyCallCounts_emptyInput() {
        assertTrue(partnerApiKeyUsageService.getDailyCallCounts(List.of()).isEmpty());
        verify(usageRepository, never()).findByApiKeyIdInAndUsageDate(any(), any());
    }

    @Test
    @DisplayName("Không có dòng usage thì số lượt trong ngày bằng 0")
    void getDailyCallCount_noRow() {
        when(usageRepository.findByApiKeyIdInAndUsageDate(any(), eq(today))).thenReturn(List.of());

        assertEquals(0, partnerApiKeyUsageService.getDailyCallCount(apiKeyId));
    }

    @Test
    @DisplayName("Giành quyền gửi cảnh báo hạn mức uỷ quyền cho writer")
    void claimQuotaWarning_delegatesToWriter() {
        UUID usageId = UUID.randomUUID();
        when(usageWriter.claimQuotaWarningInNewTransaction(usageId)).thenReturn(true);

        assertTrue(partnerApiKeyUsageService.claimQuotaWarning(usageId));
    }

    @Test
    @DisplayName("Nhả quyền cảnh báo hạn mức uỷ quyền cho writer")
    void releaseQuotaWarning_delegatesToWriter() {
        UUID usageId = UUID.randomUUID();

        partnerApiKeyUsageService.releaseQuotaWarning(usageId);

        verify(usageWriter).releaseQuotaWarningInNewTransaction(usageId);
    }

    @Test
    @DisplayName("Tìm dòng usage hôm nay của khóa")
    void findTodayUsage_returnsRow() {
        PartnerApiKeyDailyUsage usage = PartnerApiKeyDailyUsage.builder()
                .id(UUID.randomUUID())
                .apiKeyId(apiKeyId)
                .usageDate(today)
                .callCount(5)
                .build();
        when(usageRepository.findByApiKeyIdAndUsageDate(apiKeyId, today)).thenReturn(Optional.of(usage));

        assertTrue(partnerApiKeyUsageService.findTodayUsage(apiKeyId).isPresent());
    }

    @Test
    @DisplayName("Lấy danh sách dòng usage chưa cảnh báo trong ngày")
    void findTodayUnwarnedUsages_returnsRows() {
        PartnerApiKeyDailyUsage usage = PartnerApiKeyDailyUsage.builder()
                .id(UUID.randomUUID())
                .apiKeyId(apiKeyId)
                .usageDate(today)
                .callCount(9)
                .build();
        when(usageRepository.findByUsageDateAndWarningSentAtIsNull(today)).thenReturn(List.of(usage));

        assertEquals(1, partnerApiKeyUsageService.findTodayUnwarnedUsages().size());
    }
}