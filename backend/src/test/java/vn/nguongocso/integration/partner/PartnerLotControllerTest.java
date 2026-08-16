package vn.nguongocso.integration.partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;
import vn.nguongocso.integration.partner.dto.response.PartnerLotDossierResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerLotInfoResponse;
import vn.nguongocso.integration.partner.service.PartnerLotService;
import vn.nguongocso.organization.entity.Organization;

@SpringBootTest
@AutoConfigureMockMvc
class PartnerLotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerApiKeyService partnerApiKeyService;

    @MockBean
    private PartnerLotService partnerLotService;

    @Test
    @DisplayName("NCL-12-CN-002-TC-01: Lấy hồ sơ lô thành công qua Header X-API-KEY -> Trả về 200 OK")
    void testGetLotDossier_Success_TC01() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String rawApiKey = "nks_live_validkey12345678901234567890";

        Organization org = new Organization();
        org.setOrganizationId(orgId);

        PartnerApiKey key = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .partnerName("Doanh Nghiệp Thu Mua ABC")
                .rateLimitPerHour(100)
                .build();

        PartnerLotDossierResponse mockDossier = PartnerLotDossierResponse.builder()
                .lotInfo(PartnerLotInfoResponse.builder()
                        .lotId(lotId.toString())
                        .lotName("Lô Xoài Cát Hòa Lộc Vụ Thu Đông")
                        .build())
                .build();

        when(partnerApiKeyService.validateApiKeyAndCheckRateLimit(eq(rawApiKey), any())).thenReturn(key);
        when(partnerLotService.getLotDossierForPartner(eq(lotId), any(PartnerApiKey.class))).thenReturn(mockDossier);

        mockMvc.perform(get("/api/v1/partner/production-lots/" + lotId + "/dossier")
                        .header("X-API-KEY", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lotInfo.lotName").value("Lô Xoài Cát Hòa Lộc Vụ Thu Đông"));
    }

    @Test
    @DisplayName("NCL-12-CN-002-TC-02: Khóa đã hết hạn -> Hệ thống từ chối với HTTP 401 Unauthorized")
    void testExpiredKey_Returns401_TC02() throws Exception {
        UUID lotId = UUID.randomUUID();
        String rawApiKey = "nks_live_expiredkey12345678901234567";

        when(partnerApiKeyService.validateApiKeyAndCheckRateLimit(eq(rawApiKey), any()))
                .thenThrow(new BusinessException("Khóa truy cập đã hết thời gian hiệu lực"));

        mockMvc.perform(get("/api/v1/partner/production-lots/" + lotId + "/dossier")
                        .header("X-API-KEY", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("NCL-12-CN-002-TC-03: Khóa đã dùng quá 100 lượt/h -> Từ chối tạm thời với HTTP 429 Too Many Requests")
    void testRateLimitExceeded_Returns429_TC03() throws Exception {
        UUID lotId = UUID.randomUUID();
        String rawApiKey = "nks_live_overlimitkey1234567890123456";

        when(partnerApiKeyService.validateApiKeyAndCheckRateLimit(eq(rawApiKey), any()))
                .thenThrow(new BusinessException("Khóa truy cập đã vượt quá hạn mức 100 lượt gọi/giờ"));

        mockMvc.perform(get("/api/v1/partner/production-lots/" + lotId + "/dossier")
                        .header("X-API-KEY", rawApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }
}
