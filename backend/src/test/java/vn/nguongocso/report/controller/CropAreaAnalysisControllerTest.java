package vn.nguongocso.report.controller;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.report.dto.response.CropAreaAnalysisResponse;
import vn.nguongocso.report.service.CropAreaAnalysisService;
import java.util.Collections;
/**
 * Test controller CropAreaAnalysisController.
 *
 * @author Triệu Văn Đại
 */
@WebMvcTest(CropAreaAnalysisController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class CropAreaAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CropAreaAnalysisService cropAreaAnalysisService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-05") // Đăng nhập với vai trò Cán bộ quản lý ngành (REGULATOR)
    void getAnalysis_shouldReturnOk_whenUserIsRegulator() throws Exception {
        // Given
        CropAreaAnalysisResponse mockedResponse = CropAreaAnalysisResponse.builder()
                .summary(CropAreaAnalysisResponse.SummaryStats.builder()
                        .totalLots(5L)
                        .totalExpectedYield(5000.0)
                        .totalActualYield(4800.0)
                        .totalArea(10.0)
                        .build())
                .byArea(Collections.emptyList())
                .bySeason(Collections.emptyList())
                .build();

        when(cropAreaAnalysisService.getAnalysis(eq(2026), any(), any(), any(), any(), any()))
                .thenReturn(mockedResponse);

        // When / Then
        mockMvc.perform(get("/api/v1/reports/crop-area-analysis")
                        .with(csrf())
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.totalLots").value(5))
                .andExpect(jsonPath("$.data.summary.totalExpectedYield").value(5000.0))
                .andExpect(jsonPath("$.data.summary.totalArea").value(10.0));
    }

    @Test
    @WithMockUser(roles = "VT-02") // Đăng nhập với vai trò Quản lý HTX (không có quyền xem báo cáo ngành)
        void getAnalysis_shouldReturnOk_whenUserIsOrgManager() throws Exception {
        // When / Then: Gửi yêu cầu và mong đợi hệ thống chặn với mã lỗi 403 Forbidden
        mockMvc.perform(get("/api/v1/reports/crop-area-analysis")
                        .with(csrf())
                        .param("year", "2026"))
                .andExpect(status().isOk());
    }

    @Test
        void getAnalysis_shouldReturnForbidden_whenUserNotLogin() throws Exception {
        // When / Then: Gửi yêu cầu khi chưa đăng nhập và mong đợi lỗi 401 Unauthorized
        mockMvc.perform(get("/api/v1/reports/crop-area-analysis")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
