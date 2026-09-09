package vn.nguongocso.alert.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.dto.request.CategoryThresholdOverrideRequest;
import vn.nguongocso.alert.dto.request.ImpactEstimationRequest;
import vn.nguongocso.alert.dto.request.UpdateGlobalThresholdRequest;
import vn.nguongocso.alert.dto.response.AllThresholdsResponse;
import vn.nguongocso.alert.dto.response.AnomalyThresholdResponse;
import vn.nguongocso.alert.dto.response.ImpactEstimationResponse;
import vn.nguongocso.alert.service.AnomalyThresholdService;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;

/**
 * Kiểm thử Controller quản lý cấu hình ngưỡng quét bất thường (NCL-08-CN-014).
 */
@WebMvcTest(AnomalyThresholdController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AnomalyThresholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnomalyThresholdService anomalyThresholdService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("GET /api/v1/admin/anomaly-thresholds - VT-01 lấy toàn bộ cấu hình thành công")
    void getAllThresholds_shouldReturnOk_whenAdmin() throws Exception {
        AnomalyThresholdResponse global = AnomalyThresholdResponse.builder()
                .id(UUID.randomUUID())
                .maxScansPerHour(5)
                .maxScansPerDay(10)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(50.0))
                .minTimeBetweenScansMinutes(30)
                .activationAgeDays(365)
                .isActive(true)
                .build();

        AllThresholdsResponse response = AllThresholdsResponse.builder()
                .global(global)
                .categoryOverrides(Collections.emptyList())
                .build();

        when(anomalyThresholdService.getAllThresholds()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/anomaly-thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.global.maxScansPerHour").value(5))
                .andExpect(jsonPath("$.data.global.maxScansPerDay").value(10));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("PUT /api/v1/admin/anomaly-thresholds/global - VT-01 cập nhật cấu hình toàn cục thành công")
    void updateGlobalThreshold_shouldReturnOk_whenValid() throws Exception {
        UpdateGlobalThresholdRequest request = UpdateGlobalThresholdRequest.builder()
                .maxScansPerHour(6)
                .maxScansPerDay(12)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(60.0))
                .minTimeBetweenScansMinutes(30)
                .activationAgeDays(365)
                .build();

        AnomalyThresholdResponse response = AnomalyThresholdResponse.builder()
                .id(UUID.randomUUID())
                .maxScansPerHour(6)
                .maxScansPerDay(12)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(60.0))
                .minTimeBetweenScansMinutes(30)
                .activationAgeDays(365)
                .isActive(true)
                .build();

        when(anomalyThresholdService.updateGlobalThreshold(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/anomaly-thresholds/global")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maxScansPerHour").value(6))
                .andExpect(jsonPath("$.data.maxScansPerDay").value(12));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("PUT /api/v1/admin/anomaly-thresholds/global - Trả về 400 Bad Request khi giá trị không hợp lệ")
    void updateGlobalThreshold_shouldReturnBadRequest_whenInvalidValues() throws Exception {
        UpdateGlobalThresholdRequest invalidRequest = UpdateGlobalThresholdRequest.builder()
                .maxScansPerHour(-1) // Số âm không hợp lệ
                .maxScansPerDay(0)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(-5.0))
                .minTimeBetweenScansMinutes(-10)
                .activationAgeDays(-30)
                .build();

        mockMvc.perform(put("/api/v1/admin/anomaly-thresholds/global")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("POST /api/v1/admin/anomaly-thresholds/categories - VT-01 lưu cấu hình ghi đè danh mục thành công")
    void saveCategoryOverride_shouldReturnOk_whenValid() throws Exception {
        UUID catId = UUID.randomUUID();
        CategoryThresholdOverrideRequest request = CategoryThresholdOverrideRequest.builder()
                .productCategoryId(catId)
                .maxScansPerHour(4)
                .maxScansPerDay(8)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(40.0))
                .minTimeBetweenScansMinutes(20)
                .activationAgeDays(180)
                .build();

        AnomalyThresholdResponse response = AnomalyThresholdResponse.builder()
                .id(UUID.randomUUID())
                .productCategoryId(catId)
                .productCategoryName("Sầu riêng")
                .maxScansPerHour(4)
                .maxScansPerDay(8)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(40.0))
                .minTimeBetweenScansMinutes(20)
                .activationAgeDays(180)
                .isActive(true)
                .build();

        when(anomalyThresholdService.saveCategoryOverride(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/anomaly-thresholds/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCategoryId").value(catId.toString()))
                .andExpect(jsonPath("$.data.maxScansPerHour").value(4));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("DELETE /api/v1/admin/anomaly-thresholds/categories/{id} - VT-01 xóa cấu hình ghi đè danh mục thành công")
    void deleteCategoryOverride_shouldReturnOk_whenAdmin() throws Exception {
        UUID overrideId = UUID.randomUUID();
        doNothing().when(anomalyThresholdService).deleteCategoryOverride(eq(overrideId), any());

        mockMvc.perform(delete("/api/v1/admin/anomaly-thresholds/categories/{id}", overrideId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("POST /api/v1/admin/anomaly-thresholds/estimate - VT-01 ước lượng tác động thành công")
    void estimateImpact_shouldReturnOk_whenValid() throws Exception {
        ImpactEstimationRequest request = ImpactEstimationRequest.builder()
                .maxScansPerHour(5)
                .maxScansPerDay(10)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(50.0))
                .minTimeBetweenScansMinutes(30)
                .activationAgeDays(365)
                .build();

        ImpactEstimationResponse response = ImpactEstimationResponse.builder()
                .estimatedAnomaliesCount(8)
                .totalScansAnalyzed(120)
                .totalTraceCodesAnalyzed(30)
                .highFrequencyCount(3)
                .impossibleTravelCount(5)
                .activationAgeCount(0)
                .analysisPeriodDays(30)
                .message("Dự kiến có 8 mã tem bị gắn cờ bất thường.")
                .build();

        when(anomalyThresholdService.estimateImpact(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/anomaly-thresholds/estimate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estimatedAnomaliesCount").value(8))
                .andExpect(jsonPath("$.data.totalScansAnalyzed").value(120));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    @DisplayName("GET /api/v1/admin/anomaly-thresholds - Người dùng không phải VT-01 (VT-02) bị chặn 403 Forbidden")
    void getAllThresholds_shouldReturnForbidden_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/anomaly-thresholds"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VT-03")
    @DisplayName("PUT /api/v1/admin/anomaly-thresholds/global - VT-03 bị chặn 403 Forbidden")
    void updateGlobalThreshold_shouldReturnForbidden_whenRoleVT03() throws Exception {
        UpdateGlobalThresholdRequest request = UpdateGlobalThresholdRequest.builder()
                .maxScansPerHour(5)
                .maxScansPerDay(10)
                .maxDistanceKmPer30Min(BigDecimal.valueOf(50.0))
                .minTimeBetweenScansMinutes(30)
                .activationAgeDays(365)
                .build();

        mockMvc.perform(put("/api/v1/admin/anomaly-thresholds/global")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
