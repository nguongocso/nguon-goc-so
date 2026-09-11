package vn.nguongocso.certification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.certification.dto.request.InspectionExpiryThresholdRequest;
import vn.nguongocso.certification.dto.response.InspectionExpiryThresholdResponse;
import vn.nguongocso.certification.service.InspectionCriterionCatalogService;
import vn.nguongocso.certification.service.InspectionExpiryConfigService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API Controller quản lý chỉ tiêu kiểm nghiệm và cấu hình ngưỡng cảnh báo (NCL-11-CN-004).
 */
@WebMvcTest(InspectionCriterionCatalogController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class InspectionCriterionCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InspectionCriterionCatalogService inspectionCriterionCatalogService;

    @MockitoBean
    private InspectionExpiryConfigService inspectionExpiryConfigService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/v1/inspection-criteria/expiry-threshold: VT-01 được phép truy cập -> 200 OK")
    @WithMockUser(roles = "VT-01")
    void testGetExpiryThreshold_asAdmin_success() throws Exception {
        when(inspectionExpiryConfigService.getThresholdConfig()).thenReturn(
                InspectionExpiryThresholdResponse.builder()
                        .warningThresholdDays(15)
                        .updatedAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                        .updatedByName("Quản trị viên")
                        .build()
        );

        mockMvc.perform(get("/api/v1/inspection-criteria/expiry-threshold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warningThresholdDays").value(15))
                .andExpect(jsonPath("$.data.updatedByName").value("Quản trị viên"));
    }

    @Test
    @DisplayName("GET /api/v1/inspection-criteria/expiry-threshold: VT-02 không có quyền -> 403 Forbidden")
    @WithMockUser(roles = "VT-02")
    void testGetExpiryThreshold_asCoopManager_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/inspection-criteria/expiry-threshold"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/inspection-criteria/expiry-threshold: VT-01 cập nhật hợp lệ -> 200 OK")
    @WithMockUser(roles = "VT-01")
    void testUpdateExpiryThreshold_asAdmin_success() throws Exception {
        InspectionExpiryThresholdRequest request = InspectionExpiryThresholdRequest.builder()
                .warningThresholdDays(20)
                .build();

        when(inspectionExpiryConfigService.updateThresholdConfig(any(), any())).thenReturn(
                InspectionExpiryThresholdResponse.builder()
                        .warningThresholdDays(20)
                        .updatedAt(LocalDateTime.of(2026, 9, 11, 8, 0))
                        .updatedByName("Quản trị viên")
                        .build()
        );

        mockMvc.perform(put("/api/v1/inspection-criteria/expiry-threshold")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warningThresholdDays").value(20));
    }

    @Test
    @DisplayName("PUT /api/v1/inspection-criteria/expiry-threshold: VT-02 không có quyền cập nhật -> 403 Forbidden")
    @WithMockUser(roles = "VT-02")
    void testUpdateExpiryThreshold_asCoopManager_forbidden() throws Exception {
        InspectionExpiryThresholdRequest request = InspectionExpiryThresholdRequest.builder()
                .warningThresholdDays(20)
                .build();

        mockMvc.perform(put("/api/v1/inspection-criteria/expiry-threshold")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/inspection-criteria/expiry-threshold: Giá trị 0 ngày không hợp lệ -> 400 Bad Request")
    @WithMockUser(roles = "VT-01")
    void testUpdateExpiryThreshold_invalidZero_badRequest() throws Exception {
        InspectionExpiryThresholdRequest request = InspectionExpiryThresholdRequest.builder()
                .warningThresholdDays(0)
                .build();

        mockMvc.perform(put("/api/v1/inspection-criteria/expiry-threshold")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
