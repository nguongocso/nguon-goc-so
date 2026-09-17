package vn.nguongocso.integration.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.repository.PartnerApiKeyRepository;
import vn.nguongocso.integration.partner.dto.request.PartnerWebhookRegistrationRequest;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookNotificationResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookResponse;
import vn.nguongocso.integration.partner.dto.response.WebhookTestPingResponse;
import vn.nguongocso.integration.partner.entity.PartnerWebhookNotification;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.repository.PartnerWebhookNotificationRepository;
import vn.nguongocso.integration.partner.service.PartnerWebhookService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Unit test cho PartnerWebhookService (NCL-12-CN-006).
 */
@ExtendWith(MockitoExtension.class)
class PartnerWebhookServiceTest {

    @Mock
    private PartnerApiKeyRepository partnerApiKeyRepository;

    @Mock
    private PartnerWebhookNotificationRepository partnerWebhookNotificationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PartnerWebhookService partnerWebhookService;

    private UUID apiKeyId;
    private UUID orgId;
    private Organization organization;
    private PartnerApiKey apiKey;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        apiKeyId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("HTX Nông Nghiệp Xanh");

        apiKey = PartnerApiKey.builder()
                .id(apiKeyId)
                .organization(organization)
                .partnerName("Đối tác BigC")
                .status(PartnerApiKeyStatus.ACTIVE)
                .isTest(false)
                .build();

        userDetails = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(userDetails.getRoleCode()).thenReturn("VT-02");
        when(userDetails.getOrganizationId()).thenReturn(orgId);
    }

    @Test
    @DisplayName("Đăng ký Webhook URL hợp lệ HTTPS thành công và sinh webhookSecret")
    void testRegisterWebhook_Success_Https() {
        when(partnerApiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartnerWebhookRegistrationRequest request = PartnerWebhookRegistrationRequest.builder()
                .webhookUrl("https://webhook.partner.com/api/v1/recalls")
                .isActive(true)
                .build();

        PartnerWebhookResponse response = partnerWebhookService.registerWebhookForOrganizationKey(apiKeyId, request, userDetails);

        assertNotNull(response);
        assertEquals("https://webhook.partner.com/api/v1/recalls", response.getWebhookUrl());
        assertNotNull(response.getWebhookSecret());
        assertTrue(response.getWebhookSecret().startsWith("sec_wh_"));
        assertTrue(response.getIsWebhookActive());

        verify(partnerApiKeyRepository).save(apiKey);
        assertEquals("https://webhook.partner.com/api/v1/recalls", apiKey.getWebhookUrl());
        assertTrue(apiKey.getIsWebhookActive());
    }

    @Test
    @DisplayName("Lấy thông tin cấu hình webhook của khóa API bao gồm webhookSecret")
    void testGetWebhookForOrganizationKey_Success() {
        apiKey.setWebhookUrl("https://partner.com/webhook");
        apiKey.setWebhookSecret("sec_wh_currentsecret123");
        apiKey.setIsWebhookActive(true);

        when(partnerApiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        PartnerWebhookResponse response = partnerWebhookService.getWebhookForOrganizationKey(apiKeyId, userDetails);

        assertNotNull(response);
        assertEquals("https://partner.com/webhook", response.getWebhookUrl());
        assertEquals("sec_wh_currentsecret123", response.getWebhookSecret());
        assertTrue(response.getIsWebhookActive());
    }

    @Test
    @DisplayName("Từ chối Webhook URL không phải giao thức HTTPS (TC-02 an toàn)")
    void testRegisterWebhook_RejectNonHttps() {
        PartnerWebhookRegistrationRequest request = PartnerWebhookRegistrationRequest.builder()
                .webhookUrl("http://insecure.partner.com/webhook")
                .isActive(true)
                .build();

        when(partnerApiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                partnerWebhookService.registerWebhookForOrganizationKey(apiKeyId, request, userDetails));

        assertEquals("Địa chỉ nhận thông báo phải sử dụng giao thức bảo mật HTTPS (https://).", ex.getMessage());
    }

    @Test
    @DisplayName("Từ chối Webhook URL có định dạng URL không hợp lệ")
    void testRegisterWebhook_RejectMalformedUrl() {
        PartnerWebhookRegistrationRequest request = PartnerWebhookRegistrationRequest.builder()
                .webhookUrl("not-a-valid-url")
                .isActive(true)
                .build();

        when(partnerApiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                partnerWebhookService.registerWebhookForOrganizationKey(apiKeyId, request, userDetails));

        assertEquals("Định dạng URL địa chỉ nhận thông báo không hợp lệ.", ex.getMessage());
    }

    @Test
    @DisplayName("Hủy cấu hình Webhook URL thành công khi gửi webhookUrl rỗng")
    void testRegisterWebhook_ClearWebhook() {
        apiKey.setWebhookUrl("https://old.partner.com/webhook");
        apiKey.setWebhookSecret("sec_wh_12345");
        apiKey.setIsWebhookActive(true);

        when(partnerApiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));
        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartnerWebhookRegistrationRequest request = PartnerWebhookRegistrationRequest.builder()
                .webhookUrl("")
                .isActive(false)
                .build();

        PartnerWebhookResponse response = partnerWebhookService.registerWebhookForOrganizationKey(apiKeyId, request, userDetails);

        assertNotNull(response);
        assertFalse(response.getIsWebhookActive());
        assertFalse(apiKey.getIsWebhookActive());
    }

    @Test
    @DisplayName("Test ping gửi thử webhook tới endpoint chưa tồn tại trả về isSuccess=false với mã lỗi chi tiết")
    void testTestPing_NonExistentHost() {
        apiKey.setWebhookUrl("https://127.0.0.1:59999/non-existent-ping-test");
        apiKey.setWebhookSecret("sec_wh_testsecret");
        apiKey.setIsWebhookActive(true);

        when(partnerApiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        WebhookTestPingResponse pingResponse = partnerWebhookService.sendTestPing(apiKeyId, userDetails);

        assertNotNull(pingResponse);
        assertFalse(pingResponse.getIsSuccess());
        assertNotNull(pingResponse.getErrorMessage());
        assertTrue(pingResponse.getDurationMs() >= 0);
    }

    @Test
    @DisplayName("Lấy lịch sử thông báo thu hồi có phân trang và lọc theo trạng thái")
    void testGetNotificationHistory() {
        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("LÔ-GẠO-ST25-001");

        PartnerWebhookNotification notification = PartnerWebhookNotification.builder()
                .id(UUID.randomUUID())
                .partnerApiKey(apiKey)
                .shipment(shipment)
                .lotCode("LÔ-GẠO-ST25-001")
                .newStatus("RECALLING")
                .targetUrl("https://partner.com/webhook")
                .publicReason("Phát hiện dư lượng vượt ngưỡng")
                .deliveryStatus(WebhookDeliveryStatus.SUCCESS)
                .attemptCount(1)
                .lastHttpStatus(200)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        Page<PartnerWebhookNotification> page = new PageImpl<>(List.of(notification));
        when(partnerApiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));
        when(partnerWebhookNotificationRepository.findByPartnerApiKey_IdAndDeliveryStatusOrderByCreatedAtDesc(
                apiKeyId, WebhookDeliveryStatus.SUCCESS, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<PartnerWebhookNotificationResponse> result = partnerWebhookService.getNotificationsForOrganizationKey(
                apiKeyId, WebhookDeliveryStatus.SUCCESS, PageRequest.of(0, 10), userDetails);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        PartnerWebhookNotificationResponse item = result.getContent().get(0);
        assertEquals("LÔ-GẠO-ST25-001", item.getLotCode());
        assertEquals("RECALLING", item.getNewStatus());
        assertEquals(WebhookDeliveryStatus.SUCCESS, item.getDeliveryStatus());
        assertEquals(200, item.getLastHttpStatus());
    }
}
