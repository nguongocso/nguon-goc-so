package vn.nguongocso.integration.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.dto.request.CreateApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.request.CreateTestApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.request.RenewApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.request.UpdateApiKeyQuotaRequest;
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyResponse;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.event.ApiKeyLifecycleEvent;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.apikey.service.ApiKeyQuotaPolicy;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyUsageService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class PartnerApiKeyServiceTest {

    @Mock
    private PartnerApiKeyRepository partnerApiKeyRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PartnerApiKeyUsageService partnerApiKeyUsageService;

    @Mock
    private ApiKeyQuotaPolicy apiKeyQuotaPolicy;

    @Mock
    private vn.nguongocso.integration.partner.repository.PartnerWebhookNotificationRepository partnerWebhookNotificationRepository;

    @InjectMocks
    private PartnerApiKeyService partnerApiKeyService;

    private UUID orgId;
    private UUID userId;
    private Organization organization;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("Hợp Tác Xã Nông Nghiệp Sạch");

        user = new User();
        user.setUserId(userId);
        user.setFullName("Nguyễn Văn Quản Lý");

        Role role = new Role();
        role.setCode("VT-02");
        role.setName("Quản lý Hợp tác xã");

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(organization);

        userDetails = new CustomUserDetails(user, orgUser, role);
    }

    private void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-01: Tạo khóa thành công với hạn mức và thời hạn hợp lệ")
    void testCreateApiKey_Success_TC01() {
        setupSecurityContext();

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .partnerName("Công ty Thu Mua Nông Sản ABC")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(invocation -> {
            PartnerApiKey entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PartnerApiKeyResponse response = partnerApiKeyService.createApiKey(request);

        assertNotNull(response);
        assertNotNull(response.getRawApiKey(), "Khóa bản rõ rawApiKey phải hiện đầy đủ 1 lần khi tạo mới");
        assertTrue(response.getRawApiKey().startsWith("nks_live_"));
        assertEquals("nks_live_" + response.getRawApiKey().substring(9, 17), response.getKeyPrefix());
        assertEquals(PartnerApiKeyStatus.ACTIVE, response.getStatus());
        assertEquals(100, response.getRateLimitPerHour());

        // TASK-27: kiểm tra audit log của thao tác cấp khóa truy cập
        // (NCL-12-CN-005: createApiKey còn phát ApiKeyLifecycleEvent để cảnh báo ngay,
        // nên bắt tất cả sự kiện rồi lọc ActivityLogEvent thay vì đòi đúng 1 lần phát).
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        ActivityLogEvent logEvent = eventCaptor.getAllValues().stream()
                .filter(ActivityLogEvent.class::isInstance)
                .map(ActivityLogEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu sự kiện ActivityLogEvent khi cấp khóa"));
        assertEquals("CREATE_API_KEY", logEvent.getAction());
        assertEquals("PARTNER_API_KEY", logEvent.getEntityType());
        assertNotNull(logEvent.getEntityId());
        assertEquals(userId, logEvent.getUserId());
        assertEquals(orgId, logEvent.getOrganizationId());
        assertNotNull(logEvent.getTimestamp());
        // Không được ghi khóa bí mật vào audit log
        assertTrue(!logEvent.getDescription().contains(response.getRawApiKey()),
                "Audit log không được chứa rawApiKey");
        assertTrue(!logEvent.getDescription().contains("nks_live_" + response.getRawApiKey().substring(9)),
                "Audit log không được chứa hậu tố khóa bí mật");
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-03: Đặt ngày hết hạn trong quá khứ -> Hệ thống ném lỗi")
    void testCreateApiKey_PastExpiration_ThrowsException_TC03() {
        setupSecurityContext();

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .partnerName("Công ty Đối Tác")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Trong quá khứ
                .build();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> partnerApiKeyService.createApiKey(request));

        assertTrue(exception.getMessage().contains("tương lai"));
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-02: Quản lý thu hồi khóa đang hoạt động -> Ngừng hiệu lực ngay")
    void testRevokeApiKey_Success_TC02() {
        setupSecurityContext();

        UUID keyId = UUID.randomUUID();
        PartnerApiKey existingKey = PartnerApiKey.builder()
                .id(keyId)
                .organization(organization)
                .partnerName("Công ty Đối Tác")
                .keyPrefix("nks_live_a1b2")
                .keyHash("some_hash")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .status(PartnerApiKeyStatus.ACTIVE)
                .createdBy(user)
                .build();

        when(partnerApiKeyRepository.findByIdAndOrganizationId(keyId, orgId)).thenReturn(Optional.of(existingKey));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartnerApiKeyResponse response = partnerApiKeyService.revokeApiKey(keyId);

        assertNotNull(response);
        assertEquals(PartnerApiKeyStatus.REVOKED, response.getStatus());
        assertNotNull(response.getRevokedAt());

        // TC-04 (NCL-12-CN-006): kiểm tra đã hủy các thông báo chờ thử lại
        verify(partnerWebhookNotificationRepository).cancelPendingNotificationsForApiKey(
                eq(keyId),
                anyString(),
                any(LocalDateTime.class));

        // TASK-27: kiểm tra audit log của thao tác thu hồi khóa truy cập
        ArgumentCaptor<ActivityLogEvent> captor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ActivityLogEvent logEvent = captor.getValue();
        assertEquals("REVOKE_API_KEY", logEvent.getAction());
        assertEquals("PARTNER_API_KEY", logEvent.getEntityType());
        assertEquals(keyId.toString(), logEvent.getEntityId());
        assertEquals(userId, logEvent.getUserId());
        assertEquals(orgId, logEvent.getOrganizationId());
        assertNotNull(logEvent.getTimestamp());
    }

    @Test
    @DisplayName("QTN-20: Gọi API vượt hạn mức số lượt/giờ -> Báo lỗi vọt hạn mức")
    void testValidateApiKey_RateLimitExceeded_ThrowsException_QTN20() {
        String rawApiKey = "nks_live_testkey12345678901234567890";
        String keyHash = PartnerApiKeyService.hashSha256(rawApiKey);

        PartnerApiKey key = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .partnerName("Đối Tác Test")
                .keyPrefix("nks_live_test")
                .keyHash(keyHash)
                .rateLimitPerHour(2) // Hạn mức 2 lượt/giờ
                .expiresAt(LocalDateTime.now().plusDays(10))
                .status(PartnerApiKeyStatus.ACTIVE)
                .totalCalls(0L)
                .failedCalls(0L)
                .build();

        when(partnerApiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(key));

        // NCL-12-CN-005: lượt gọi trong ngày đếm ở DB; ngưỡng cảnh báo lấy từ policy dùng chung.
        when(partnerApiKeyUsageService.recordCallAndGetDailyCount(key.getId())).thenReturn(1, 2);
        when(apiKeyQuotaPolicy.warningThreshold(2)).thenReturn(2);

        // Lượt 1 OK
        partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1");
        // Lượt 2 OK
        partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1");

        // Lượt 3 -> Vượt hạn mức -> Throw BusinessException
        BusinessException ex = assertThrows(BusinessException.class,
                () -> partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("vượt quá hạn mức"));
    }

    @Test
    @DisplayName("NCL-12-CN-005-TC-06: Gia hạn khóa ACTIVE thành công")
    void testRenewApiKey_Active_Success() {
        setupSecurityContext();
        UUID keyId = UUID.randomUUID();
        PartnerApiKey existingKey = PartnerApiKey.builder()
                .id(keyId)
                .organization(organization)
                .partnerName("Đối Tác Test")
                .keyPrefix("nks_live_test")
                .keyHash("hash")
                .rateLimitPerHour(50)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .status(PartnerApiKeyStatus.ACTIVE)
                .createdBy(user)
                .build();

        when(partnerApiKeyRepository.findByIdAndOrganizationId(keyId, orgId)).thenReturn(Optional.of(existingKey));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        RenewApiKeyRequest request = RenewApiKeyRequest.builder().expiresAt(LocalDateTime.now().plusDays(60)).build();
        PartnerApiKeyResponse response = partnerApiKeyService.renewApiKey(keyId, request);
        assertEquals(PartnerApiKeyStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("NCL-12-CN-005-TC-07: Gia hạn khóa EXPIRED thành công và trở lại ACTIVE")
    void testRenewApiKey_Expired_ReturnsActive() {
        setupSecurityContext();
        UUID keyId = UUID.randomUUID();
        PartnerApiKey existingKey = PartnerApiKey.builder()
                .id(keyId)
                .organization(organization)
                .partnerName("Đối Tác")
                .keyPrefix("nks_live_exp")
                .keyHash("hash")
                .rateLimitPerHour(50)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .status(PartnerApiKeyStatus.EXPIRED)
                .createdBy(user)
                .build();

        when(partnerApiKeyRepository.findByIdAndOrganizationId(keyId, orgId)).thenReturn(Optional.of(existingKey));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        RenewApiKeyRequest request = RenewApiKeyRequest.builder().expiresAt(LocalDateTime.now().plusDays(30)).build();
        PartnerApiKeyResponse response = partnerApiKeyService.renewApiKey(keyId, request);
        assertEquals(PartnerApiKeyStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("NCL-12-CN-005-TC-08: Gia hạn khóa REVOKED bị từ chối")
    void testRenewApiKey_Revoked_ThrowsException() {
        setupSecurityContext();
        UUID keyId = UUID.randomUUID();
        PartnerApiKey existingKey = PartnerApiKey.builder()
                .id(keyId)
                .organization(organization)
                .partnerName("Đối Tác")
                .keyPrefix("nks_revoked")
                .keyHash("hash")
                .status(PartnerApiKeyStatus.REVOKED)
                .build();

        when(partnerApiKeyRepository.findByIdAndOrganizationId(keyId, orgId)).thenReturn(Optional.of(existingKey));
        RenewApiKeyRequest request = RenewApiKeyRequest.builder().expiresAt(LocalDateTime.now().plusDays(10)).build();
        assertThrows(BusinessException.class, () -> partnerApiKeyService.renewApiKey(keyId, request));
    }

    @Test
    @DisplayName("NCL-12-CN-005-TC-09: Nâng hạn mức cộng thêm 100 vào 100 thành 200")
    void testUpdateQuota_Success() {
        setupSecurityContext();
        UUID keyId = UUID.randomUUID();
        PartnerApiKey existingKey = PartnerApiKey.builder()
                .id(keyId)
                .organization(organization)
                .partnerName("Đối Tác")
                .keyPrefix("nks_live_quota")
                .keyHash("hash")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .status(PartnerApiKeyStatus.ACTIVE)
                .build();

        when(partnerApiKeyRepository.findByIdAndOrganizationIdForUpdate(keyId, orgId)).thenReturn(Optional.of(existingKey));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateApiKeyQuotaRequest request = UpdateApiKeyQuotaRequest.builder().incrementBy(100).build();
        PartnerApiKeyResponse response = partnerApiKeyService.updateApiKeyQuota(keyId, request);
        assertEquals(200, response.getRateLimitPerHour());
    }

    @Test
    @DisplayName("NCL-12-CN-005-TC-10: Số lượt hạn mức bổ sung bằng 0 bị từ chối")
    void testUpdateQuota_ZeroIncrement_ThrowsException() {
        setupSecurityContext();
        UUID keyId = UUID.randomUUID();
        PartnerApiKey existingKey = PartnerApiKey.builder()
                .id(keyId)
                .organization(organization)
                .partnerName("Đối Tác")
                .keyPrefix("nks_same")
                .keyHash("hash")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .status(PartnerApiKeyStatus.ACTIVE)
                .build();

        when(partnerApiKeyRepository.findByIdAndOrganizationIdForUpdate(keyId, orgId)).thenReturn(Optional.of(existingKey));
        UpdateApiKeyQuotaRequest request = UpdateApiKeyQuotaRequest.builder().incrementBy(0).build();
        assertThrows(BusinessException.class, () -> partnerApiKeyService.updateApiKeyQuota(keyId, request));
    }

    @Test
    @DisplayName("QTN-20 concurrency (Case A): 20 request đồng thời cùng 1 key, limit=2 — không bao giờ vượt hạn mức")
    void testRateLimit_ConcurrentBurst_NeverExceedsLimit() throws InterruptedException {
        String rawApiKey = "nks_live_concurrent_aaaaaaaaaaaaaaaaaaaaaa";
        String keyHash = PartnerApiKeyService.hashSha256(rawApiKey);
        PartnerApiKey key = buildActiveRateLimitedKey(keyHash, 2);
        when(partnerApiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(key));
        when(partnerApiKeyUsageService.recordCallAndGetDailyCount(key.getId())).thenReturn(0);
        when(apiKeyQuotaPolicy.warningThreshold(2)).thenReturn(0);

        int totalThreads = 20;
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rateLimited = new AtomicInteger();
        AtomicInteger otherError = new AtomicInteger();
        runConcurrentRequests(rawApiKey, totalThreads, success, rateLimited, otherError);

        assertEquals(0, otherError.get(), "Không được phát sinh lỗi ngoài phạm vi hạn mức");
        assertEquals(totalThreads, success.get() + rateLimited.get(), "Toàn bộ request phải được xử lý");
        assertEquals(2, success.get(),
                "limit=2: chỉ đúng 2 request được phép thành công dù 20 request đồng thời, thực tế: " + success.get());
        assertTrue(success.get() <= 2, "Số request thành công không được vượt rateLimitPerHour=2");
        assertEquals(totalThreads - 2, rateLimited.get(), "Các request còn lại phải nhận 429");
        assertEquals(2, partnerApiKeyService.getCurrentHourCalls(key.getId()),
                "Counter giờ cuối phải đúng bằng limit (2) — 429 không được làm tăng counter");
    }

    @Test
    @DisplayName("QTN-20 concurrency (Case B): limit=2, counter sẵn có=1, 10 request đồng thời — chỉ 1 request được phép thêm")
    void testRateLimit_ConcurrentRaceNearThreshold_OnlyOneMoreAllowed() throws InterruptedException {
        String rawApiKey = "nks_live_race_bbbbbbbbbbbbbbbbbbbbbbbb";
        String keyHash = PartnerApiKeyService.hashSha256(rawApiKey);
        PartnerApiKey key = buildActiveRateLimitedKey(keyHash, 2);
        when(partnerApiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(key));
        when(partnerApiKeyUsageService.recordCallAndGetDailyCount(key.getId())).thenReturn(0);
        when(apiKeyQuotaPolicy.warningThreshold(2)).thenReturn(0);

        // Warmup đúng 1 lượt -> counter giờ = 1 (sát ngưỡng limit = 2)
        partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1");
        assertEquals(1, partnerApiKeyService.getCurrentHourCalls(key.getId()));

        int totalThreads = 10;
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rateLimited = new AtomicInteger();
        AtomicInteger otherError = new AtomicInteger();
        runConcurrentRequests(rawApiKey, totalThreads, success, rateLimited, otherError);

        assertEquals(0, otherError.get(), "Không được phát sinh lỗi ngoài phạm vi hạn mức");
        assertEquals(totalThreads, success.get() + rateLimited.get(), "Toàn bộ request phải được xử lý");
        assertEquals(1, success.get(),
                "counter sẵn có=1, limit=2: chỉ được thêm đúng 1 request thành công dù 10 request đồng thời, thực tế: "
                        + success.get());
        assertEquals(totalThreads - 1, rateLimited.get());
        assertEquals(2, partnerApiKeyService.getCurrentHourCalls(key.getId()),
                "Counter giờ cuối phải đúng bằng limit (2) — không vượt hạn mức dưới concurrency");
    }

    @Test
    @DisplayName("QTN-20 sequential (Case C): limit=2, lượt 1-2 thành công, lượt 3-4 nhận 429, counter giữ nguyên = 2")
    void testRateLimit_SequentialRegression_CounterStopsAtLimit() {
        String rawApiKey = "nks_live_seq_cccccccccccccccccccccccc";
        String keyHash = PartnerApiKeyService.hashSha256(rawApiKey);
        PartnerApiKey key = buildActiveRateLimitedKey(keyHash, 2);
        when(partnerApiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(key));
        when(partnerApiKeyUsageService.recordCallAndGetDailyCount(key.getId())).thenReturn(0);
        when(apiKeyQuotaPolicy.warningThreshold(2)).thenReturn(0);

        // Lượt 1, 2 thành công
        partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1");
        partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1");
        assertEquals(2, partnerApiKeyService.getCurrentHourCalls(key.getId()));

        // Lượt 3, 4 nhận 429 (BusinessException vượt hạn mức)
        assertThrows(BusinessException.class,
                () -> partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1"));
        assertThrows(BusinessException.class,
                () -> partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1"));

        // Counter không đổi sau 429
        assertEquals(2, partnerApiKeyService.getCurrentHourCalls(key.getId()),
                "429 không được làm tăng counter theo giờ; counter cuối phải bằng 2");
        // Số lượt gọi trong ngày (DB) chỉ được ghi cho request thành công
        verify(partnerApiKeyUsageService, times(2)).recordCallAndGetDailyCount(key.getId());
    }

    @Test
    @DisplayName("NCL-12-CN-005: Cấp khóa hạn +5 ngày phát sự kiện vòng đời để cảnh báo ngay")
    void testCreateApiKey_ShortExpiry_PublishesLifecycleEvent() {
        setupSecurityContext();

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(5);
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .partnerName("Đối Tác Sắp Hết Hạn")
                .rateLimitPerHour(100)
                .expiresAt(expiresAt)
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(invocation -> {
            PartnerApiKey entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PartnerApiKeyResponse response = partnerApiKeyService.createApiKey(request);
        assertNotNull(response);

        ApiKeyLifecycleEvent lifecycleEvent = captureLifecycleEvent();
        assertEquals(response.getId(), lifecycleEvent.getApiKeyId());
        assertEquals(orgId, lifecycleEvent.getOrganizationId());
        assertEquals(PartnerApiKeyStatus.ACTIVE, lifecycleEvent.getStatus());
    }

    @Test
    @DisplayName("NCL-12-CN-005: Cấp khóa hạn +30 ngày vẫn phát sự kiện (listener tự bỏ qua, không cảnh báo)")
    void testCreateApiKey_LongExpiry_PublishesEventButNoWarningNeeded() {
        setupSecurityContext();

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .partnerName("Đối Tác Dài Hạn")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(invocation -> {
            PartnerApiKey entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PartnerApiKeyResponse response = partnerApiKeyService.createApiKey(request);
        assertNotNull(response);

        // Thiết kế: luôn phát sự kiện để tránh nhân bản ngưỡng 7 ngày ở tầng quản lý khóa;
        // ApiKeyWarningService quyết định không gửi thông báo khi hạn còn xa.
        ApiKeyLifecycleEvent lifecycleEvent = captureLifecycleEvent();
        assertEquals(response.getId(), lifecycleEvent.getApiKeyId());
    }

    @Test
    @DisplayName("NCL-12-CN-005: Cấp khóa thử nghiệm hạn ngắn phát sự kiện vòng đời")
    void testCreateTestApiKey_ShortExpiry_PublishesLifecycleEvent() {
        setupSecurityContext();

        CreateTestApiKeyRequest request = CreateTestApiKeyRequest.builder()
                .partnerName("Đối Tác Thử Nghiệm")
                .rateLimitPerHour(60)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(invocation -> {
            PartnerApiKey entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PartnerApiKeyResponse response = partnerApiKeyService.createTestApiKey(request);
        assertNotNull(response);

        ApiKeyLifecycleEvent lifecycleEvent = captureLifecycleEvent();
        assertEquals(response.getId(), lifecycleEvent.getApiKeyId());
        assertEquals(PartnerApiKeyStatus.ACTIVE, lifecycleEvent.getStatus());
    }

    @Test
    @DisplayName("NCL-12-CN-005: Gia hạn về hạn ngắn phát sự kiện vòng đời để cảnh báo ngay")
    void testRenewApiKey_ShortExpiry_PublishesLifecycleEvent() {
        setupSecurityContext();
        UUID keyId = UUID.randomUUID();
        PartnerApiKey existingKey = PartnerApiKey.builder()
                .id(keyId)
                .organization(organization)
                .partnerName("Đối Tác Gia Hạn")
                .keyPrefix("nks_live_renew")
                .keyHash("hash")
                .rateLimitPerHour(50)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .status(PartnerApiKeyStatus.ACTIVE)
                .createdBy(user)
                .build();

        when(partnerApiKeyRepository.findByIdAndOrganizationId(keyId, orgId)).thenReturn(Optional.of(existingKey));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        RenewApiKeyRequest request = RenewApiKeyRequest.builder().expiresAt(LocalDateTime.now().plusDays(5)).build();
        PartnerApiKeyResponse response = partnerApiKeyService.renewApiKey(keyId, request);
        assertNotNull(response);

        ApiKeyLifecycleEvent lifecycleEvent = captureLifecycleEvent();
        assertEquals(keyId, lifecycleEvent.getApiKeyId());
        assertEquals(PartnerApiKeyStatus.ACTIVE, lifecycleEvent.getStatus());
    }

    private ApiKeyLifecycleEvent captureLifecycleEvent() {
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(eventCaptor.capture());
        return eventCaptor.getAllValues().stream()
                .filter(ApiKeyLifecycleEvent.class::isInstance)
                .map(ApiKeyLifecycleEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu sự kiện ApiKeyLifecycleEvent"));
    }

    private PartnerApiKey buildActiveRateLimitedKey(String keyHash, int rateLimit) {
        return PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .partnerName("Đối Tác Concurrency")
                .keyPrefix("nks_live_con")
                .keyHash(keyHash)
                .rateLimitPerHour(rateLimit)
                .expiresAt(LocalDateTime.now().plusDays(10))
                .status(PartnerApiKeyStatus.ACTIVE)
                .totalCalls(0L)
                .failedCalls(0L)
                .build();
    }

    private void runConcurrentRequests(String rawApiKey, int threadCount, AtomicInteger success,
            AtomicInteger rateLimited, AtomicInteger otherError) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        ready.countDown();
                        start.await();
                        try {
                            partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1");
                            success.incrementAndGet();
                        } catch (BusinessException e) {
                            if (e.getMessage().contains("vượt quá hạn mức")) {
                                rateLimited.incrementAndGet();
                            } else {
                                otherError.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Không đủ thread sẵn sàng trước khi bắn start");
            }
            start.countDown();
            if (!done.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("Một số request concurrency không hoàn tất trong 30 giây");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
