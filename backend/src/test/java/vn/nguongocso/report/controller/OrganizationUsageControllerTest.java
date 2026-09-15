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
import vn.nguongocso.report.dto.response.OrganizationUsageDashboardResponse;
import vn.nguongocso.report.service.OrganizationUsageService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử controller bảng điều khiển mức độ sử dụng theo tổ chức (NCL-07-CN-008),
 * trọng tâm là phân quyền VT-01/VT-02.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationUsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationUsageService organizationUsageService;

    private OrganizationUsageDashboardResponse mockDashboard() {
        return OrganizationUsageDashboardResponse.builder()
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 30))
                .previousStartDate(LocalDate.of(2026, 8, 2))
                .previousEndDate(LocalDate.of(2026, 8, 31))
                .totalOrganizations(1)
                .items(List.of())
                .build();
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("AC-03: VT-01 được phép truy cập dashboard")
    void getOrganizationUsage_AllowedForVT01() throws Exception {
        when(organizationUsageService.getDashboard(any(), any(), any())).thenReturn(mockDashboard());

        mockMvc.perform(get("/api/v1/reports/organization-usage")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrganizations").value(1))
                .andExpect(jsonPath("$.data.previousStartDate").value("2026-08-02"))
                .andExpect(jsonPath("$.data.previousEndDate").value("2026-08-31"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    @DisplayName("AC-03: VT-02 bị từ chối 403 khi truy cập dashboard")
    void getOrganizationUsage_ForbiddenForVT02() throws Exception {
        mockMvc.perform(get("/api/v1/reports/organization-usage")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errors").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/v1/reports/organization-usage"));
    }

    @Test
    @DisplayName("Thiếu JWT bị từ chối (403 theo convention anonymous của hệ thống)")
    void getOrganizationUsage_UnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/reports/organization-usage"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VT-02")
    @DisplayName("AC-03: VT-02 bị từ chối 403 khi export báo cáo")
    void exportOrganizationUsage_ForbiddenForVT02() throws Exception {
        mockMvc.perform(get("/api/v1/reports/organization-usage/export")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("VT-01 export báo cáo CSV thành công")
    void exportOrganizationUsage_SuccessForVT01() throws Exception {
        when(organizationUsageService.exportCsv(any(), any(), any()))
                .thenReturn(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', ',', 'b'});

        mockMvc.perform(get("/api/v1/reports/organization-usage/export")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("VT-01 export báo cáo PDF thành công khi format=pdf")
    void exportOrganizationUsage_PdfFormatSuccess() throws Exception {
        when(organizationUsageService.exportPdf(any(), any(), any()))
                .thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46, 0x0A}); // "%PDF-"

        mockMvc.perform(get("/api/v1/reports/organization-usage/export")
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    @DisplayName("Kiểu xuất không hợp lệ dùng lại CSV mặc định")
    void exportOrganizationUsage_UnknownFormat_FallbackToCsv() throws Exception {
        when(organizationUsageService.exportCsv(any(), any(), any()))
                .thenReturn(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a'});

        mockMvc.perform(get("/api/v1/reports/organization-usage/export")
                        .param("format", "xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));

        verify(organizationUsageService).exportCsv(any(), any(), any());
    }
}
