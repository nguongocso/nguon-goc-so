package vn.nguongocso.integration.partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.controller.PartnerLotController;
import vn.nguongocso.integration.partner.dto.response.PartnerLotDossierResponse;
import vn.nguongocso.integration.partner.dto.response.PartnerLotInfoResponse;
import vn.nguongocso.integration.partner.service.PartnerLotService;
import vn.nguongocso.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class PartnerLotControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PartnerLotService partnerLotService;

    @InjectMocks
    private PartnerLotController partnerLotController;

    private PartnerApiKey partnerApiKey;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(partnerLotController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        Organization org = new Organization();
        org.setOrganizationId(UUID.randomUUID());

        partnerApiKey = PartnerApiKey.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .partnerName("Doanh Nghiệp Thu Mua ABC")
                .build();
    }

    @Test
    @DisplayName("NCL-12-CN-002-TC-01: Lấy hồ sơ lô thành công qua Header X-API-KEY -> Trả về 200 OK")
    void testGetLotDossier_Success_TC01() throws Exception {
        UUID lotId = UUID.randomUUID();

        PartnerLotDossierResponse mockDossier = PartnerLotDossierResponse.builder()
                .lotInfo(PartnerLotInfoResponse.builder()
                        .lotId(lotId.toString())
                        .lotName("Lô Xoài Cát Hòa Lộc Vụ Thu Đông")
                        .build())
                .build();

        when(partnerLotService.getLotDossierForPartner(eq(lotId), any())).thenReturn(mockDossier);

        mockMvc.perform(get("/api/v1/partner/production-lots/" + lotId + "/dossier")
                        .requestAttr("partnerApiKey", partnerApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lotInfo.lotName").value("Lô Xoài Cát Hòa Lộc Vụ Thu Đông"));
    }
}
