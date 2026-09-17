package vn.nguongocso.integration.apikey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import vn.nguongocso.integration.apikey.controller.PartnerApiKeyController;
import vn.nguongocso.integration.apikey.dto.request.CreateApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyResponse;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;

@ExtendWith(MockitoExtension.class)
class PartnerApiKeyControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PartnerApiKeyService partnerApiKeyService;

    @Mock
    private vn.nguongocso.integration.partner.service.PartnerWebhookService partnerWebhookService;

    @InjectMocks
    private PartnerApiKeyController partnerApiKeyController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(partnerApiKeyController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return parameter.getParameterType().equals(vn.nguongocso.auth.service.CustomUserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                            org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                            org.springframework.web.context.request.NativeWebRequest webRequest,
                            org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return org.mockito.Mockito.mock(vn.nguongocso.auth.service.CustomUserDetails.class);
                    }
                })
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-01: Quản lý hợp tác xã (VT-02) tạo khóa -> Trả về 201 Created và hiển thị rawApiKey 1 lần")
    void testCreateApiKey_ManagerRole_TC01() throws Exception {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .partnerName("Công ty Thu Mua ABC")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        PartnerApiKeyResponse mockResponse = PartnerApiKeyResponse.builder()
                .id(UUID.randomUUID())
                .partnerName("Công ty Thu Mua ABC")
                .keyPrefix("nks_live_a1b2")
                .rawApiKey("nks_live_a1b2c3d4e5f678901234567890abcdef")
                .rateLimitPerHour(100)
                .expiresAt(request.getExpiresAt())
                .status(PartnerApiKeyStatus.ACTIVE)
                .build();

        when(partnerApiKeyService.createApiKey(any(CreateApiKeyRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/organization/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rawApiKey").value("nks_live_a1b2c3d4e5f678901234567890abcdef"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("NCL-12-CN-006: Lấy thông tin cấu hình Webhook bao gồm webhookSecret -> Trả về 200 OK")
    void testGetWebhookConfig_Success() throws Exception {
        UUID apiKeyId = UUID.randomUUID();
        var mockResponse = vn.nguongocso.integration.partner.dto.response.PartnerWebhookResponse.builder()
                .id(apiKeyId)
                .partnerName("Công ty Thu Mua ABC")
                .webhookUrl("https://partner.com/webhook")
                .isWebhookActive(true)
                .webhookSecret("sec_wh_12345")
                .build();

        when(partnerWebhookService.getWebhookForOrganizationKey(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/organization/api-keys/" + apiKeyId + "/webhook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.webhookSecret").value("sec_wh_12345"));
    }

    @Test
    @DisplayName("NCL-12-CN-006: Đăng ký địa chỉ Webhook HTTPS thành công -> Trả về 200 OK")
    void testRegisterWebhook_Success() throws Exception {
        UUID apiKeyId = UUID.randomUUID();
        var request = vn.nguongocso.integration.partner.dto.request.PartnerWebhookRegistrationRequest.builder()
                .webhookUrl("https://partner.com/webhook")
                .isActive(true)
                .build();

        var mockResponse = vn.nguongocso.integration.partner.dto.response.PartnerWebhookResponse.builder()
                .id(apiKeyId)
                .partnerName("Công ty Thu Mua ABC")
                .webhookUrl("https://partner.com/webhook")
                .isWebhookActive(true)
                .webhookSecret("sec_wh_12345")
                .build();

        when(partnerWebhookService.registerWebhookForOrganizationKey(any(), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/organization/api-keys/" + apiKeyId + "/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.webhookUrl").value("https://partner.com/webhook"))
                .andExpect(jsonPath("$.data.isWebhookActive").value(true));
    }

    @Test
    @DisplayName("NCL-12-CN-006: Bắn thử nghiệm webhook (Test Ping) -> Trả về 200 OK kèm kết quả kiểm tra")
    void testPingWebhook_Success() throws Exception {
        UUID apiKeyId = UUID.randomUUID();
        var mockResponse = vn.nguongocso.integration.partner.dto.response.WebhookTestPingResponse.builder()
                .targetUrl("https://partner.com/webhook")
                .httpStatus(200)
                .durationMs(120L)
                .isSuccess(true)
                .responseBody("{\"status\":\"ok\"}")
                .build();

        when(partnerWebhookService.sendTestPing(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/organization/api-keys/" + apiKeyId + "/webhook/test-ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isSuccess").value(true))
                .andExpect(jsonPath("$.data.httpStatus").value(200));
    }

    @Test
    @DisplayName("NCL-12-CN-006: Lấy danh sách lịch sử thông báo thu hồi -> Trả về 200 OK có phân trang")
    void testGetNotifications_Success() throws Exception {
        UUID apiKeyId = UUID.randomUUID();
        var mockPage = new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(
                        vn.nguongocso.integration.partner.dto.response.PartnerWebhookNotificationResponse.builder()
                                .id(UUID.randomUUID())
                                .partnerApiKeyId(apiKeyId)
                                .partnerName("Đối tác BigC")
                                .lotCode("LÔ-001")
                                .newStatus("RECALLING")
                                .deliveryStatus(vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus.SUCCESS)
                                .build()
                ),
                org.springframework.data.domain.PageRequest.of(0, 10),
                1
        );

        when(partnerWebhookService.getNotificationsForOrganizationKey(any(), any(), any(), any())).thenReturn(mockPage);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/organization/api-keys/" + apiKeyId + "/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].lotCode").value("LÔ-001"))
                .andExpect(jsonPath("$.data.content[0].deliveryStatus").value("SUCCESS"));
    }
}
