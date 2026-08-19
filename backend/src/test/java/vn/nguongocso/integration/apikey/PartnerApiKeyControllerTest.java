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

    @InjectMocks
    private PartnerApiKeyController partnerApiKeyController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(partnerApiKeyController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
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
}
