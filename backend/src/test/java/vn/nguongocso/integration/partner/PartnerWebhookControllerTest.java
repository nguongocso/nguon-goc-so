package vn.nguongocso.integration.partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.partner.controller.PartnerWebhookController;
import vn.nguongocso.integration.partner.dto.request.PartnerWebhookRegistrationRequest;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookNotificationResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerWebhookResponse;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.integration.partner.service.PartnerWebhookService;

/**
 * Controller unit test cho PartnerWebhookController (NCL-12-CN-006).
 */
@ExtendWith(MockitoExtension.class)
class PartnerWebhookControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PartnerWebhookService partnerWebhookService;

    @InjectMocks
    private PartnerWebhookController partnerWebhookController;

    private PartnerApiKey partnerApiKey;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(partnerWebhookController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();

        partnerApiKey = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .partnerName("Đối tác Chuỗi Bán Lẻ")
                .keyPrefix("nks_live_abc1")
                .status(PartnerApiKeyStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Đối tác gửi PUT /api/v1/partner/webhook cấu hình Webhook -> Trả về 200 OK")
    void testPartnerSelfConfigureWebhook_Success() throws Exception {
        PartnerWebhookRegistrationRequest request = PartnerWebhookRegistrationRequest.builder()
                .webhookUrl("https://retail.example.com/api/recalls")
                .isActive(true)
                .build();

        PartnerWebhookResponse mockResponse = PartnerWebhookResponse.builder()
                .id(partnerApiKey.getId())
                .partnerName(partnerApiKey.getPartnerName())
                .keyPrefix(partnerApiKey.getKeyPrefix())
                .webhookUrl("https://retail.example.com/api/recalls")
                .isWebhookActive(true)
                .webhookSecret("sec_wh_secret123")
                .build();

        when(partnerWebhookService.registerWebhookForPartnerKey(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/partner/webhook")
                        .requestAttr("partnerApiKey", partnerApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.webhookUrl").value("https://retail.example.com/api/recalls"))
                .andExpect(jsonPath("$.data.isWebhookActive").value(true));
    }

    @Test
    @DisplayName("Đối tác gửi GET /api/v1/partner/notifications xem lịch sử thông báo -> Trả về 200 OK")
    void testPartnerGetNotifications_Success() throws Exception {
        var mockPage = new PageImpl<>(List.of(
                PartnerWebhookNotificationResponse.builder()
                        .id(UUID.randomUUID())
                        .partnerApiKeyId(partnerApiKey.getId())
                        .partnerName(partnerApiKey.getPartnerName())
                        .lotCode("LOT-GAO-ST25")
                        .newStatus("RECALLING")
                        .deliveryStatus(WebhookDeliveryStatus.SUCCESS)
                        .attemptCount(1)
                        .lastHttpStatus(200)
                        .build()
        ), PageRequest.of(0, 10), 1);

        when(partnerWebhookService.getNotificationsForPartnerKey(any(), any(), any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/partner/notifications")
                        .requestAttr("partnerApiKey", partnerApiKey)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].lotCode").value("LOT-GAO-ST25"))
                .andExpect(jsonPath("$.data.content[0].deliveryStatus").value("SUCCESS"));
    }
}
