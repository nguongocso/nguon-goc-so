package vn.nguongocso.integration.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import vn.nguongocso.integration.apikey.entity.PartnerApiKeyDailyUsage;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.event.ApiKeyQuotaThresholdEvent;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.apikey.service.ApiKeyQuotaPolicy;
import vn.nguongocso.integration.apikey.service.ApiKeyWarningService;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyUsageService;
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

    @Mock
    private PartnerApiKeyUsageService partnerApiKeyUsageService;

    @Mock
    private ApiKeyQuotaPolicy apiKeyQuotaPolicy;

    @Mock
    private PartnerApiKeyService partnerApiKeyService;

    @InjectMocks
    private ApiKeyWarningService apiKeyWarningService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apiKeyWarningService, "expiryWarningDays", 7);
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
    @DisplayName("TC-02: Chạm ngưỡng hạn mức gửi cảnh báo 1 lần trong ngày (claim ở DB)")
    void handleQuotaThreshold_notifiesOncePerDay() {
        UUID keyId = UUID.randomUUID();
        UUID usageId = UUID.randomUUID();
        PartnerApiKeyDailyUsage usage = PartnerApiKeyDailyUsage.builder()
                .id(usageId)
                .apiKeyId(keyId)
                .usageDate(LocalDate.now())
                .callCount(8)
                .build();
        ApiKeyQuotaThresholdEvent event = buildQuotaEvent(keyId, 8);

        when(partnerApiKeyUsageService.findTodayUsage(keyId)).thenReturn(Optional.of(usage));
        when(partnerApiKeyUsageService.claimQuotaWarning(usageId)).thenReturn(true, false);
        when(apiKeyQuotaPolicy.warningThresholdPercent()).thenReturn(80);

        apiKeyWarningService.handleQuotaThreshold(event);
        apiKeyWarningService.handleQuotaThreshold(event);

        verify(notificationService, times(1)).sendHandoverNotification(
                eq("Khóa truy cập sắp chạm hạn mức"), anyString(), eq(keyId), eq(organization.getOrganizationId()));
    }

    @Test
    @DisplayName("Gửi cảnh báo hạn mức thất bại thì nhả cờ để lần đối soát sau gửi lại")
    void handleQuotaThreshold_releasesClaimOnFailure() {
        UUID keyId = UUID.randomUUID();
        UUID usageId = UUID.randomUUID();
        PartnerApiKeyDailyUsage usage = PartnerApiKeyDailyUsage.builder()
                .id(usageId)
                .apiKeyId(keyId)
                .usageDate(LocalDate.now())
                .callCount(8)
                .build();
        ApiKeyQuotaThresholdEvent event = buildQuotaEvent(keyId, 8);

        when(partnerApiKeyUsageService.findTodayUsage(keyId)).thenReturn(Optional.of(usage));
        when(partnerApiKeyUsageService.claimQuotaWarning(usageId)).thenReturn(true);
        when(apiKeyQuotaPolicy.warningThresholdPercent()).thenReturn(80);
        doThrow(new RuntimeException("lỗi gửi thông báo"))
                .when(notificationService)
                .sendHandoverNotification(anyString(), anyString(), any(UUID.class), any(UUID.class));

        apiKeyWarningService.handleQuotaThreshold(event);

        verify(partnerApiKeyUsageService).releaseQuotaWarning(usageId);
    }

    @Test
    @DisplayName("TC-05: Job đối soát gửi bù cảnh báo cho khóa đã vượt ngưỡng nhưng chưa cảnh báo")
    void reconcileQuotaWarnings_sendsCatchUpWarning() {
        PartnerApiKey key = buildKey("Doi tac TC-05", LocalDateTime.now().plusDays(30), PartnerApiKeyStatus.ACTIVE);
        PartnerApiKeyDailyUsage usage = PartnerApiKeyDailyUsage.builder()
                .id(UUID.randomUUID())
                .apiKeyId(key.getId())
                .usageDate(LocalDate.now())
                .callCount(85)
                .build();

        when(partnerApiKeyUsageService.findTodayUnwarnedUsages()).thenReturn(List.of(usage));
        when(partnerApiKeyRepository.findAllById(List.of(key.getId()))).thenReturn(List.of(key));
        when(partnerApiKeyService.getCurrentHourCalls(key.getId())).thenReturn(85);
        when(apiKeyQuotaPolicy.isReached(85, key.getRateLimitPerHour())).thenReturn(true);
        when(partnerApiKeyUsageService.claimQuotaWarning(usage.getId())).thenReturn(true);
        when(apiKeyQuotaPolicy.warningThresholdPercent()).thenReturn(80);

        apiKeyWarningService.reconcileQuotaWarnings();

        verify(notificationService, times(1)).sendHandoverNotification(
                eq("Khóa truy cập sắp chạm hạn mức"), anyString(), eq(key.getId()),
                eq(organization.getOrganizationId()));
    }

    @Test
    @DisplayName("TC-03: Job đối soát bỏ qua khóa đã thu hồi và khóa chưa chạm ngưỡng")
    void reconcileQuotaWarnings_skipsRevokedAndBelowThreshold() {
        PartnerApiKey revoked = buildKey("Doi tac TC-03", LocalDateTime.now().plusDays(30), PartnerApiKeyStatus.REVOKED);
        PartnerApiKey healthy = buildKey("Doi tac binh thuong", LocalDateTime.now().plusDays(30),
                PartnerApiKeyStatus.ACTIVE);
        PartnerApiKeyDailyUsage revokedUsage = PartnerApiKeyDailyUsage.builder()
                .id(UUID.randomUUID())
                .apiKeyId(revoked.getId())
                .usageDate(LocalDate.now())
                .callCount(95)
                .build();
        PartnerApiKeyDailyUsage healthyUsage = PartnerApiKeyDailyUsage.builder()
                .id(UUID.randomUUID())
                .apiKeyId(healthy.getId())
                .usageDate(LocalDate.now())
                .callCount(10)
                .build();

        when(partnerApiKeyUsageService.findTodayUnwarnedUsages()).thenReturn(List.of(revokedUsage, healthyUsage));
        when(partnerApiKeyRepository.findAllById(List.of(revoked.getId(), healthy.getId())))
                .thenReturn(List.of(revoked, healthy));
        when(partnerApiKeyService.getCurrentHourCalls(healthy.getId())).thenReturn(10);
        when(apiKeyQuotaPolicy.isReached(10, healthy.getRateLimitPerHour())).thenReturn(false);

        apiKeyWarningService.reconcileQuotaWarnings();

        verify(notificationService, never()).sendHandoverNotification(
                anyString(), anyString(), any(UUID.class), any(UUID.class));
        verify(partnerApiKeyUsageService, never()).claimQuotaWarning(any(UUID.class));
    }

    private ApiKeyQuotaThresholdEvent buildQuotaEvent(UUID keyId, int usedCalls) {
        return ApiKeyQuotaThresholdEvent.builder()
                .apiKeyId(keyId)
                .organizationId(organization.getOrganizationId())
                .partnerName("Doi tac TC-02")
                .rateLimitPerHour(10)
                .usedCalls(usedCalls)
                .warningThreshold(8)
                .build();
    }
}
