package vn.nguongocso.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.report.dto.response.AlertBadgeSummary;
import vn.nguongocso.report.dto.response.AlertLotDetailResponse;
import vn.nguongocso.report.dto.response.AlertLotInfoItem;
import vn.nguongocso.report.dto.response.AlertLotOrgItem;
import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;
import vn.nguongocso.report.enums.LotAlertType;
import vn.nguongocso.report.service.TerritoryLotAlertService;

/**
 * Kiểm thử tầng Web/Controller cho TerritoryAlertLotController (NCL-07-CN-006).
 */
@WebMvcTest(TerritoryAlertLotController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TerritoryAlertLotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TerritoryLotAlertService territoryLotAlertService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Cán bộ quản lý ngành (VT-05) truy vấn danh sách lô cảnh báo thành công (200 OK)")
    @WithMockUser(roles = "VT-05")
    void getAlertLots_success_asRegulator() throws Exception {
        UUID lotId = UUID.randomUUID();
        AlertLotSummaryResponse item = AlertLotSummaryResponse.builder()
                .lotId(lotId)
                .lotCode("LOT-12345678")
                .lotName("Lô Rau hữu cơ")
                .organizationName("HTX Nông Sản Sạch")
                .alertTypes(List.of(LotAlertType.RECALLING))
                .primaryAlertType(LotAlertType.RECALLING)
                .alertCount(1)
                .alertSummaries(List.of(AlertBadgeSummary.builder()
                        .alertType(LotAlertType.RECALLING)
                        .alertName("Lô đang thu hồi")
                        .severity("CRITICAL")
                        .build()))
                .build();

        PageResponse<AlertLotSummaryResponse> pageResponse = PageResponse.<AlertLotSummaryResponse>builder()
                .items(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(territoryLotAlertService.getAlertLots(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/reports/alert-lots")
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.items[0].lotCode").value("LOT-12345678"))
                .andExpect(jsonPath("$.data.items[0].alertTypes[0]").value("RECALLING"));
    }

    @Test
    @DisplayName("Quản trị hệ thống (VT-01) truy vấn danh sách thành công (200 OK)")
    @WithMockUser(roles = "VT-01")
    void getAlertLots_success_asAdmin() throws Exception {
        PageResponse<AlertLotSummaryResponse> emptyPage = PageResponse.<AlertLotSummaryResponse>builder()
                .items(Collections.emptyList())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();

        when(territoryLotAlertService.getAlertLots(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/reports/alert-lots").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("Vai trò không được phép (VT-03 - Người ghi sự kiện) bị chặn 403 Forbidden")
    @WithMockUser(roles = "VT-03")
    void getAlertLots_forbidden_forUnauthorizedRoles() throws Exception {
        mockMvc.perform(get("/api/v1/reports/alert-lots").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Truy vấn chi tiết lô thành công (200 OK)")
    @WithMockUser(roles = "VT-05")
    void getAlertLotDetail_success() throws Exception {
        UUID lotId = UUID.randomUUID();
        AlertLotDetailResponse response = AlertLotDetailResponse.builder()
                .lotInfo(AlertLotInfoItem.builder()
                        .lotId(lotId)
                        .lotCode("LOT-ABCDEF12")
                        .lotName("Lô Cà chua")
                        .status("APPROVED")
                        .build())
                .organization(AlertLotOrgItem.builder()
                        .organizationId(UUID.randomUUID())
                        .organizationName("HTX Rau Sạch")
                        .taxCode("0102030405")
                        .build())
                .activeAlerts(Collections.emptyList())
                .timelineEvents(Collections.emptyList())
                .build();

        when(territoryLotAlertService.getAlertLotDetail(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/alert-lots/{lotId}", lotId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lotInfo.lotCode").value("LOT-ABCDEF12"))
                .andExpect(jsonPath("$.data.organization.organizationName").value("HTX Rau Sạch"));
    }

    @Test
    @DisplayName("Xuất file Excel thành công với header attachment (200 OK)")
    @WithMockUser(roles = "VT-05")
    void exportAlertLots_success() throws Exception {
        byte[] fakeExcel = new byte[]{1, 2, 3, 4, 5};
        when(territoryLotAlertService.exportAlertLots(any(), any(), any(), any(), any(), any()))
                .thenReturn(fakeExcel);

        mockMvc.perform(get("/api/v1/reports/alert-lots/export").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment; filename=")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @DisplayName("Mutation request (POST) trên endpoint báo cáo bị từ chối")
    @WithMockUser(roles = "VT-05")
    void mutationPost_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/reports/alert-lots")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
