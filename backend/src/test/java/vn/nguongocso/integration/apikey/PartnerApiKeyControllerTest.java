package vn.nguongocso.integration.apikey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.integration.apikey.dto.request.CreateApiKeyRequest;
import vn.nguongocso.integration.apikey.dto.response.PartnerApiKeyResponse;
import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;
import vn.nguongocso.integration.apikey.service.PartnerApiKeyService;

@SpringBootTest
@AutoConfigureMockMvc
class PartnerApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PartnerApiKeyService partnerApiKeyService;

    @Test
    @DisplayName("NCL-12-CN-001-TC-04: Người dùng là Người ghi sự kiện (VT-03) truy cập trang/API quản lý khóa -> Trả về 403 Forbidden")
    @WithMockUser(username = "event_recorder_user", roles = {"VT-03"})
    void testAccessDenied_NonManagerRole_TC04() throws Exception {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .partnerName("Đối Tác ABC")
                .rateLimitPerHour(100)
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build();

        // Tài khoản VT-03 gọi API tạo mới -> Bị cấm 403
        mockMvc.perform(post("/api/v1/organization/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Tài khoản VT-03 gọi API danh sách -> Bị cấm 403
        mockMvc.perform(get("/api/v1/organization/api-keys"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-01: Quản lý hợp tác xã (VT-02) tạo khóa -> Trả về 201 Created và hiển thị rawApiKey 1 lần")
    @WithMockUser(username = "coop_manager_user", roles = {"VT-02"})
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
