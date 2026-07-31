package vn.nguongocso.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.report.dto.response.LookupStatisticsResponse;
import vn.nguongocso.report.service.LookupStatisticsService;

import java.util.Collections;

@WebMvcTest(LookupStatisticsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@WithMockUser(roles = "VT-02") // Đăng nhập với vai trò HTX để chạy API Thống kê
public class LookupStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LookupStatisticsService lookupStatisticsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getStatistics_shouldReturnOk_whenAuthorized() throws Exception {
        // Given
        LookupStatisticsResponse response = LookupStatisticsResponse.builder()
                .summary(LookupStatisticsResponse.SummaryStats.builder()
                        .totalScans(100L)
                        .totalUniqueCodes(45L)
                        .abnormalScansCount(5L)
                        .build())
                .build();

        when(lookupStatisticsService.getStatistics(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/v1/reports/lookup-statistics")
                        .with(csrf())
                        .param("groupBy", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.totalScans").value(100))
                .andExpect(jsonPath("$.data.summary.abnormalScansCount").value(5));
    }

    @Test
    void getAbnormalScans_shouldReturnOk_whenAuthorized() throws Exception {
        // Given
        when(lookupStatisticsService.getAbnormalScans(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // When / Then
        mockMvc.perform(get("/api/v1/reports/lookup-statistics/abnormal")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
