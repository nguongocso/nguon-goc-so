package vn.nguongocso.report.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.report.dto.response.MetricThresholdDto;
import vn.nguongocso.report.dto.response.SystemStatusResponse;
import vn.nguongocso.report.enums.OverallSystemStatus;
import vn.nguongocso.report.service.SystemMonitoringService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemMonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemMonitoringService systemMonitoringService;

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("TC-01: Quản trị viên VT-01 truy cập thành công API giám sát")
    void getSystemStatus_AllowedForVT01() throws Exception {
        SystemStatusResponse mockResponse = SystemStatusResponse.builder()
                .overallStatus(OverallSystemStatus.HEALTHY)
                .hasSufficientData(true)
                .uptimeSeconds(3600)
                .lastUpdated(Instant.now())
                .breachedMetricsCount(0)
                .summaryMessage("Hệ thống hoạt động bình thường.")
                .metrics(Map.of())
                .build();

        when(systemMonitoringService.getSystemStatus()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/admin/monitoring/system-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallStatus").value("HEALTHY"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    @DisplayName("TC-03: Quản lý HTX VT-02 truy cập bị từ chối 403 Forbidden")
    void getSystemStatus_ForbiddenForVT02() throws Exception {
        mockMvc.perform(get("/api/v1/admin/monitoring/system-status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errors").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/monitoring/system-status"));
    }

    @Test
    @DisplayName("Thiếu JWT bị từ chối (403 theo convention anonymous của hệ thống)")
    void getSystemStatus_UnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/monitoring/system-status"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VT-02")
    @DisplayName("TC-03: VT-02 truy cập /thresholds bị từ chối 403")
    void getThresholds_ForbiddenForVT02() throws Exception {
        mockMvc.perform(get("/api/v1/admin/monitoring/thresholds"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("Lấy danh sách ngưỡng cài đặt giám sát thành công")
    void getThresholds_Success() throws Exception {
        List<MetricThresholdDto> thresholds = List.of(
                MetricThresholdDto.builder()
                        .metricCode("SERVER_ERRORS")
                        .metricName("Số lỗi máy chủ")
                        .thresholdValue("20.0")
                        .unit("lỗi/giờ")
                        .build()
        );

        when(systemMonitoringService.getMonitoringThresholds()).thenReturn(thresholds);

        mockMvc.perform(get("/api/v1/admin/monitoring/thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].metricCode").value("SERVER_ERRORS"));
    }
}
