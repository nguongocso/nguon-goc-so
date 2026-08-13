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

        when(activityLogService.getActivityLogs(anyInt(), anyInt(), any(), any(), any(), any(), any()))
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
}