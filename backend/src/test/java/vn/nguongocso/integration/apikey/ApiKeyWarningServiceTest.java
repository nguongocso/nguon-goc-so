package vn.nguongocso.integration.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.event.ApiKeyQuotaThresholdEvent;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.apikey.service.ApiKeyWarningService;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;

/**
 * Kiểm thử dịch vụ cảnh báo khóa truy cập (NCL-12-CN-005: TC-01 đến TC-04).
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyWarningServiceTest {

    @Mock
    private PartnerApiKeyRepository partnerApiKeyRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ApiKeyWarningService apiKeyWarningService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apiKeyWarningService, "expiryWarningDays", 7);
        ReflectionTestUtils.setField(apiKeyWarningService, "quotaWarningRatio", 0.8);
        organization = Organization.builder()
                .organizationId(UUID.randomUUID())
                .name("HTX Test")
                .build();
    }

    private PartnerApiKey buildKey(String partnerName, LocalDateTime expiresAt, PartnerApiKeyStatus status) {
        return PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .partnerName(partnerName)
                .keyPrefix("nks_live_test")
                .keyHash("hash")
                .rateLimitPerHour(100)
                .expiresAt(expiresAt)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("TC-01: Khóa còn 5 ngày được cảnh báo sắp hết hạn đúng 1 lần")
    void scanExpiringKeys_sendsWarningOnce() {
        PartnerApiKey key = buildKey("Doi tac TC-01", LocalDateTime.now().plusDays(5), PartnerApiKeyStatus.ACTIVE);
        when(partnerApiKeyRepository.findByStatus(PartnerApiKeyStatus.ACTIVE)).thenReturn(List.of(key));
        when(notificationRepository.existsByEntityIdAndTitleAndCreatedAtAfter(
                eq(key.getId()), eq("Khóa truy cập sắp hết hạn"), any(LocalDateTime.class)))
                .thenReturn(false);

        apiKeyWarningService.scanExpiringKeys();

        verify(notificationService, times(1)).sendHandoverNotification(
                eq("Khóa truy cập sắp hết hạn"), anyString(), eq(key.getId()), eq(organization.getOrganizationId()));
    }

    @Test
    @DisplayName("TC-04: Đã cảnh báo trong ngày thì quét lại không tạo trùng")
    void scanExpiringKeys_skipsAlreadyWarnedToday() {
        PartnerApiKey key = buildKey("Doi tac TC-04", LocalDateTime.now().plusDays(5), PartnerApiKeyStatus.ACTIVE);
        when(partnerApiKeyRepository.findByStatus(PartnerApiKeyStatus.ACTIVE)).thenReturn(List.of(key));
        when(notificationRepository.existsByEntityIdAndTitleAndCreatedAtAfter(
                eq(key.getId()), eq("Khóa truy cập sắp hết hạn"), any(LocalDateTime.class)))
                .thenReturn(true);

        apiKeyWarningService.scanExpiringKeys();

        verify(notificationService, never()).sendHandoverNotification(
                anyString(), anyString(), any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("TC-03: Khóa REVOKED không được quét (repository chỉ trả ACTIVE)")
    void scanExpiringKeys_ignoresRevokedKeys() {
        PartnerApiKey revoked = buildKey("Doi tac TC-03", LocalDateTime.now().plusDays(20), PartnerApiKeyStatus.REVOKED);
        when(partnerApiKeyRepository.findByStatus(PartnerApiKeyStatus.ACTIVE)).thenReturn(List.of());

        apiKeyWarningService.scanExpiringKeys();

        assertEquals(PartnerApiKeyStatus.REVOKED, revoked.getStatus());
        verify(notificationService, never()).sendHandoverNotification(
                anyString(), anyString(), any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("Khóa đã quá hạn được persist EXPIRED và báo 1 lần")
    void scanExpiringKeys_persistsExpiredAndNotifies() {
        PartnerApiKey key = buildKey("Doi tac het han", LocalDateTime.now().minusHours(1), PartnerApiKeyStatus.ACTIVE);
        when(partnerApiKeyRepository.findByStatus(PartnerApiKeyStatus.ACTIVE)).thenReturn(List.of(key));
        when(notificationRepository.existsByEntityIdAndTitleAndCreatedAtAfter(
                eq(key.getId()), eq("Khóa truy cập đã hết hạn"), any(LocalDateTime.class)))
                .thenReturn(false);

        apiKeyWarningService.scanExpiringKeys();

        assertEquals(PartnerApiKeyStatus.EXPIRED, key.getStatus());
        verify(partnerApiKeyRepository, times(1)).save(key);
        verify(notificationService, times(1)).sendHandoverNotification(
                eq("Khóa truy cập đã hết hạn"), anyString(), eq(key.getId()), eq(organization.getOrganizationId()));
    }

    @Test
    @DisplayName("Khóa bình thường (còn 365 ngày) không bị cảnh báo")
    void scanExpiringKeys_ignoresHealthyKeys() {
        PartnerApiKey key = buildKey("Doi tac binh thuong", LocalDateTime.now().plusDays(365), PartnerApiKeyStatus.ACTIVE);
        when(partnerApiKeyRepository.findByStatus(PartnerApiKeyStatus.ACTIVE)).thenReturn(List.of(key));

        apiKeyWarningService.scanExpiringKeys();

        verify(notificationService, never()).sendHandoverNotification(
                anyString(), anyString(), any(UUID.class), any(UUID.class));
        verify(partnerApiKeyRepository, never()).save(any(PartnerApiKey.class));
    }

    @Test
    @DisplayName("TC-02: Chạm ngưỡng hạn mức gửi cảnh báo 1 lần mỗi ngày")
    void handleQuotaThreshold_notifiesOncePerHour() {
        UUID keyId = UUID.randomUUID();
        ApiKeyQuotaThresholdEvent event = ApiKeyQuotaThresholdEvent.builder()
                .apiKeyId(keyId)
                .organizationId(organization.getOrganizationId())
                .partnerName("Doi tac TC-02")
                .rateLimitPerHour(10)
                .usedCalls(8)
                .build();
        when(notificationRepository.existsByEntityIdAndTitleAndCreatedAtAfter(
                eq(keyId), eq("Khóa truy cập sắp chạm hạn mức"), any(LocalDateTime.class)))
                .thenReturn(false)
                .thenReturn(true);

        apiKeyWarningService.handleQuotaThreshold(event);
        apiKeyWarningService.handleQuotaThreshold(event);

        verify(notificationService, times(1)).sendHandoverNotification(
                eq("Khóa truy cập sắp chạm hạn mức"), anyString(), eq(keyId), eq(organization.getOrganizationId()));
    }
}
