package vn.nguongocso.certification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.certification.dto.response.InspectionResultEntryLinkResponse;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;
import vn.nguongocso.certification.service.InspectionResultEntryLinkService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.exception.BusinessException;

/**
 * Kiểm thử controller cho việc cấp và tra cứu liên kết nhập kết quả kiểm nghiệm.
 */
@WebMvcTest(InspectionResultEntryLinkController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class InspectionResultEntryLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InspectionResultEntryLinkService linkService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Cấp liên kết thành công trả về HTTP 201 khi vai trò là VT-02")
    @WithMockUser(roles = "VT-02")
    void testIssueLink_Success_Returns201() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();

        InspectionResultEntryLinkResponse response = InspectionResultEntryLinkResponse.builder()
                .id(linkId)
                .entryUrl("https://nguongocso.vn/inspection-result-entry/test-raw-token")
                .recipientEmail("lab@quatest3.vn")
                .status(InspectionResultEntryLinkStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(linkService.issueLink(eq(requestId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/inspection-requests/{id}/result-entry-links", requestId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientEmail\":\"lab@quatest3.vn\",\"expiryDays\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(linkId.toString()))
                .andExpect(jsonPath("$.data.entryUrl").value("https://nguongocso.vn/inspection-result-entry/test-raw-token"))
                .andExpect(jsonPath("$.data.recipientEmail").value("lab@quatest3.vn"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Cấp liên kết bị từ chối HTTP 403 khi vai trò là VT-01 (Admin)")
    @WithMockUser(roles = "VT-01")
    void testIssueLink_RoleVT01_Returns403() throws Exception {
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/inspection-requests/{id}/result-entry-links", requestId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientEmail\":\"lab@quatest3.vn\",\"expiryDays\":7}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cấp liên kết bị từ chối HTTP 403 khi vai trò là VT-03 (Kỹ thuật viên)")
    @WithMockUser(roles = "VT-03")
    void testIssueLink_RoleVT03_Returns403() throws Exception {
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/inspection-requests/{id}/result-entry-links", requestId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientEmail\":\"lab@quatest3.vn\",\"expiryDays\":7}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cấp liên kết bị từ chối HTTP 403 khi vai trò là VT-04 (Xã viên)")
    @WithMockUser(roles = "VT-04")
    void testIssueLink_RoleVT04_Returns403() throws Exception {
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/inspection-requests/{id}/result-entry-links", requestId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientEmail\":\"lab@quatest3.vn\",\"expiryDays\":7}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cấp liên kết bị từ chối HTTP 403 khi vai trò là VT-05 (Thanh tra viên)")
    @WithMockUser(roles = "VT-05")
    void testIssueLink_RoleVT05_Returns403() throws Exception {
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/inspection-requests/{id}/result-entry-links", requestId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientEmail\":\"lab@quatest3.vn\",\"expiryDays\":7}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cấp liên kết thất bại HTTP 400 khi email người nhận không hợp lệ")
    @WithMockUser(roles = "VT-02")
    void testIssueLink_InvalidEmail_Returns400() throws Exception {
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/inspection-requests/{id}/result-entry-links", requestId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientEmail\":\"invalid-email-format\",\"expiryDays\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lấy liên kết gần nhất thành công trả về HTTP 200")
    @WithMockUser(roles = "VT-02")
    void testGetLatestLink_Success_Returns200() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();

        InspectionResultEntryLinkResponse response = InspectionResultEntryLinkResponse.builder()
                .id(linkId)
                .recipientEmail("lab@quatest3.vn")
                .status(InspectionResultEntryLinkStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(linkService.getLatestLink(eq(requestId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/inspection-requests/{id}/result-entry-links/latest", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(linkId.toString()))
                .andExpect(jsonPath("$.data.recipientEmail").value("lab@quatest3.vn"));
    }

    @Test
    @DisplayName("Lấy liên kết gần nhất trả về HTTP 404 khi không tìm thấy")
    @WithMockUser(roles = "VT-02")
    void testGetLatestLink_NotFound_Returns404() throws Exception {
        UUID requestId = UUID.randomUUID();

        when(linkService.getLatestLink(eq(requestId), any()))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy liên kết"));

        mockMvc.perform(get("/api/v1/inspection-requests/{id}/result-entry-links/latest", requestId))
                .andExpect(status().isNotFound());
    }
}
