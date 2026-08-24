package vn.nguongocso.integration.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyResponse;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;
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
        ArgumentCaptor<ActivityLogEvent> captor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ActivityLogEvent logEvent = captor.getValue();
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

        // Lượt 1 OK
        partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1");
        // Lượt 2 OK
        partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1");

        // Lượt 3 -> Vượt hạn mức -> Throw BusinessException
        BusinessException ex = assertThrows(BusinessException.class,
                () -> partnerApiKeyService.validateApiKeyAndCheckRateLimit(rawApiKey, "127.0.0.1"));

        assertTrue(ex.getMessage().contains("vượt quá hạn mức"));
    }
}
