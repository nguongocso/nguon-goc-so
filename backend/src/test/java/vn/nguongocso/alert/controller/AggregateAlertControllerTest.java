package vn.nguongocso.alert.controller;

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
import vn.nguongocso.alert.dto.response.AggregateAlertCountResponse;
import vn.nguongocso.alert.dto.response.AggregateAlertItemResponse;
import vn.nguongocso.alert.dto.response.AggregateAlertPageResponse;
import vn.nguongocso.alert.dto.response.UnviewedAlertCountResponse;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AggregateAlertType;
import vn.nguongocso.alert.service.AggregateAlertService;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử Controller cảnh báo tổng hợp (NCL-08-CN-016).
 */
@WebMvcTest(AggregateAlertController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AggregateAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AggregateAlertService aggregateAlertService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/v1/alerts/aggregate - Thành công khi người dùng có vai trò VT-02")
    @WithMockUser(username = "manager", roles = {"VT-02"})
    void testGetAggregateAlerts_Success_VT02() throws Exception {
        UUID alertId = UUID.randomUUID();
        AggregateAlertItemResponse item = AggregateAlertItemResponse.builder()
                .id(alertId)
                .type(AggregateAlertType.CERT_EXPIRING)
                .typeName("Chứng nhận sắp hết hạn")
                .severity(AlertSeverity.MEDIUM)
                .title("Chứng nhận VietGAP sắp hết hạn")
                .message("Chứng nhận còn 5 ngày nữa sẽ hết hạn")
                .relatedEntityType("CERTIFICATION")
                .relatedEntityId(UUID.randomUUID())
                .relatedEntityName("VietGAP")
                .createdAt(LocalDateTime.now())
                .actionUrl("/certifications")
                .status("OPEN")
                .build();

        AggregateAlertPageResponse mockResponse = AggregateAlertPageResponse.builder()
                .items(List.of(item))
                .totalElements(1)
                .totalPages(1)
                .currentPage(0)
                .pageSize(10)
                .summaryCounts(AggregateAlertCountResponse.builder()
                        .totalOpen(1)
                        .highSeverityCount(0)
                        .mediumSeverityCount(1)
                        .byTypeCounts(Map.of("CERT_EXPIRING", 1L))
                        .build())
                .build();

        when(aggregateAlertService.getAggregateAlerts(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/alerts/aggregate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(alertId.toString()))
                .andExpect(jsonPath("$.data.items[0].type").value("CERT_EXPIRING"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/alerts/aggregate - Thành công khi người dùng có vai trò VT-01 (Admin)")
    @WithMockUser(username = "admin", roles = {"VT-01"})
    void testGetAggregateAlerts_Success_VT01() throws Exception {
        AggregateAlertPageResponse mockResponse = AggregateAlertPageResponse.builder()
                .items(Collections.emptyList())
                .totalElements(0)
                .totalPages(0)
                .currentPage(0)
                .pageSize(10)
                .build();

        when(aggregateAlertService.getAggregateAlerts(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/alerts/aggregate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/alerts/aggregate - Bị chặn 403 Forbidden đối với vai trò không được phép (VT-03)")
    @WithMockUser(username = "recorder", roles = {"VT-03"})
    void testGetAggregateAlerts_Forbidden_VT03() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/aggregate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/alerts/aggregate/counts - Lấy thống kê số lượng thành công")
    @WithMockUser(username = "manager", roles = {"VT-02"})
    void testGetAggregateAlertCounts_Success() throws Exception {
        AggregateAlertCountResponse countResponse = AggregateAlertCountResponse.builder()
                .totalOpen(5)
                .highSeverityCount(2)
                .mediumSeverityCount(3)
                .byTypeCounts(Map.of(
                        "SCAN_ANOMALY", 1L,
                        "CERT_EXPIRING", 1L,
                        "OVERDUE_MILESTONE", 2L,
                        "OPEN_RECALL_CASE", 1L
                ))
                .build();

        when(aggregateAlertService.getAggregateAlertCounts(any()))
                .thenReturn(countResponse);

        mockMvc.perform(get("/api/v1/alerts/aggregate/counts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOpen").value(5))
                .andExpect(jsonPath("$.data.highSeverityCount").value(2))
                .andExpect(jsonPath("$.data.mediumSeverityCount").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/alerts/unviewed-count - Lấy số lượng cho thanh điều hướng thành công")
    @WithMockUser(username = "manager", roles = {"VT-02"})
    void testGetUnviewedAlertCount_Success() throws Exception {
        UnviewedAlertCountResponse unviewedResponse = UnviewedAlertCountResponse.builder()
                .unviewedCount(3)
                .hasHighSeverity(true)
                .build();

        when(aggregateAlertService.getUnviewedAlertCount())
                .thenReturn(unviewedResponse);

        mockMvc.perform(get("/api/v1/alerts/unviewed-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unviewedCount").value(3))
                .andExpect(jsonPath("$.data.hasHighSeverity").value(true));
    }
}
