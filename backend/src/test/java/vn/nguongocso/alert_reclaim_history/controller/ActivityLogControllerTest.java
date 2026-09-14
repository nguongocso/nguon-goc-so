package vn.nguongocso.alert_reclaim_history.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.alert.controller.ActivityLogController;
import vn.nguongocso.alert.dto.response.ActivityLogExportPreviewResponse;
import vn.nguongocso.alert.dto.response.ActivityLogExportResult;
import vn.nguongocso.alert.service.ActivityLogExportService;
import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityLogController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityLogService activityLogService;

    @MockitoBean
    private ActivityLogExportService activityLogExportService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // Helper tạo CustomUserDetails
    private CustomUserDetails createCustomUserDetails(String username, String roleCode) {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName(username);
        user.setFullName("Test User");
        user.setPasswordHash("password");

        Organization org = new Organization();
        org.setOrganizationId(UUID.randomUUID());
        org.setName("Test Organization");
        org.setCode("TEST");

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(org);

        Role role = new Role();
        role.setCode(roleCode);
        role.setName("Role Name");

        return new CustomUserDetails(user, orgUser, role);
    }

    @Test
    void getActivityLogs_shouldReturnOk_whenUserIsOrgManager() throws Exception {
        CustomUserDetails mockUserDetails = createCustomUserDetails("manager", "VT-02");

        PageResponse response = PageResponse.builder()
                .items(Collections.emptyList())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(activityLogService.getActivityLogs(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/organizations/activity-logs")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                    mockUserDetails, null, mockUserDetails.getAuthorities())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void getActivityLogs_shouldReturnForbidden_whenUserHasWrongRole() throws Exception {
        CustomUserDetails mockUserDetails = createCustomUserDetails("recorder", "VT-03");

        mockMvc.perform(get("/api/v1/organizations/activity-logs")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                mockUserDetails, null, mockUserDetails.getAuthorities())))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActivityLogs_shouldReturnForbidden_whenAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/activity-logs")
                        .with(csrf()))
                .andExpect(status().isForbidden()); // 403 vì chưa đăng nhập
    }

    @Test
    void previewExport_shouldReturnCount_whenUserIsOrgManager() throws Exception {
        CustomUserDetails user = createCustomUserDetails("manager", "VT-02");
        when(activityLogExportService.preview(any(), any()))
                .thenReturn(ActivityLogExportPreviewResponse.builder()
                        .count(7)
                        .mode("DIRECT")
                        .build());

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports/preview")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"objectType\":\"PRODUCTION_LOT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(7))
                .andExpect(jsonPath("$.data.mode").value("DIRECT"));
    }

    @Test
    void exportActivityLogs_shouldReturnCsv_whenUserIsOrgManager() throws Exception {
        CustomUserDetails user = createCustomUserDetails("manager", "VT-02");
        byte[] csv = "\ufeffoccurredAt,actorName".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(activityLogExportService.requestExport(any(), any())).thenReturn(
                ActivityLogExportResult.builder().mode("DIRECT").csvBytes(csv).build());

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().bytes(csv));
    }

    @Test
    void previewExport_shouldReturnForbidden_whenUserHasWrongRole() throws Exception {
        CustomUserDetails user = createCustomUserDetails("recorder", "VT-03");

        mockMvc.perform(post("/api/v1/organizations/activity-logs/exports/preview")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities())))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
