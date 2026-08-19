package vn.nguongocso.unit.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.controller.AuthController;
import vn.nguongocso.auth.dto.request.LoginRequest;
import vn.nguongocso.auth.dto.request.SelectOrganizationRequest;
import vn.nguongocso.auth.dto.response.LoginResponse;
import vn.nguongocso.auth.dto.response.OrganizationSelectionResponse;
import vn.nguongocso.auth.dto.response.SelectOrganizationResponse;
import vn.nguongocso.auth.service.AuthService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private JwtTokenProvider jwtTokenProvider;

        @MockitoBean
        private CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private PermissionChecker permissionChecker;

        private CustomUserDetails userDetails;
        private UUID userId;
        private UUID orgId;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                orgId = UUID.randomUUID();

                userDetails = mock(CustomUserDetails.class);

                when(userDetails.getUserId()).thenReturn(userId);
                when(userDetails.getUsername()).thenReturn("laodai");
                when(userDetails.getFullName()).thenReturn("Lao Dai");
                when(userDetails.getRoleCode()).thenReturn("VT-02");
                when(userDetails.getRoleName()).thenReturn("Quản lý HTX");
                when(userDetails.getOrganizationId()).thenReturn(orgId);
                when(userDetails.getOrganizationCode()).thenReturn("HTX_XYZ");
                when(userDetails.getOrganizationName())
                                .thenReturn("HTX Nông Nghiệp XYZ");
                when(userDetails.getOrganizationType())
                                .thenReturn(OrganizationType.COOPERATIVE);

                Authentication authentication = mock(Authentication.class);

                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = mock(SecurityContext.class);
                when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void getCurrentUser_shouldReturnProfileWithPermissions_whenAuthenticated()
                        throws Exception {

                List<String> mockPermissions = List.of(
                                "farm_area:CREATE",
                                "production_lot:READ");

                when(permissionChecker.getPermissionsForCurrentUser())
                                .thenReturn(mockPermissions);

                mockMvc.perform(
                                get("/api/v1/auth/me")
                                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.userId")
                                                .value(userId.toString()))
                                .andExpect(jsonPath("$.data.username")
                                                .value("laodai"))
                                .andExpect(jsonPath("$.data.fullName")
                                                .value("Lao Dai"))
                                .andExpect(jsonPath("$.data.roleCode")
                                                .value("VT-02"))
                                .andExpect(jsonPath("$.data.roleName")
                                                .value("Quản lý HTX"))
                                .andExpect(jsonPath("$.data.organizationId")
                                                .value(orgId.toString()))
                                .andExpect(jsonPath("$.data.organizationCode")
                                                .value("HTX_XYZ"))
                                .andExpect(jsonPath("$.data.organizationName")
                                                .value("HTX Nông Nghiệp XYZ"))
                                .andExpect(jsonPath("$.data.permissions[0]")
                                                .value("farm_area:CREATE"))
                                .andExpect(jsonPath("$.data.permissions[1]")
                                                .value("production_lot:READ"));

                verify(permissionChecker, times(1))
                                .getPermissionsForCurrentUser();
        }

        @Test
        void login_shouldReturnSelectionToken() throws Exception {

                LoginRequest request = new LoginRequest();
                request.setUsername("tpd01");
                request.setPassword("password");

                LoginResponse response = LoginResponse.builder()
                                .selectionToken("selection-token")
                                .tokenType("Bearer")
                                .expiresIn(300L)
                                .user(
                                                LoginResponse.UserInfo.builder()
                                                                .userId(userId.toString())
                                                                .username("tpd01")
                                                                .fullName("Test User")
                                                                .build())
                                .build();

                when(authService.login(
                                any(LoginRequest.class))).thenReturn(response);

                mockMvc.perform(
                                post("/api/v1/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.selectionToken")
                                                .value("selection-token"))
                                .andExpect(jsonPath("$.data.tokenType")
                                                .value("Bearer"))
                                .andExpect(jsonPath("$.data.expiresIn")
                                                .value(300))
                                .andExpect(jsonPath("$.data.user.username")
                                                .value("tpd01"))
                                .andExpect(jsonPath("$.data.user.fullName")
                                                .value("Test User"));

                verify(authService, times(1))
                                .login(any(LoginRequest.class));
        }

        @Test
        void getMyOrganizations_shouldReturnCurrentUserMemberships()
                        throws Exception {

                OrganizationSelectionResponse organization = OrganizationSelectionResponse.builder()
                                .organizationId(orgId.toString())
                                .organizationCode("HTX_XYZ")
                                .organizationName("HTX Nông Nghiệp XYZ")
                                .organizationType(OrganizationType.COOPERATIVE)
                                .roleCode("VT-02")
                                .roleName("Quản lý HTX")
                                .build();

                when(authService.getOrganizationsForUser(userId))
                                .thenReturn(List.of(organization));

                mockMvc.perform(
                                get("/api/v1/auth/my-organizations")
                                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data[0].organizationId")
                                                .value(orgId.toString()))
                                .andExpect(jsonPath("$.data[0].organizationCode")
                                                .value("HTX_XYZ"))
                                .andExpect(jsonPath("$.data[0].organizationName")
                                                .value("HTX Nông Nghiệp XYZ"))
                                .andExpect(jsonPath("$.data[0].roleCode")
                                                .value("VT-02"))
                                .andExpect(jsonPath("$.data[0].roleName")
                                                .value("Quản lý HTX"));

                verify(authService, times(1))
                                .getOrganizationsForUser(userId);
        }

        @Test
        void getOrganizations_shouldPassBearerSelectionTokenToService()
                        throws Exception {

                when(authService.getOrganizations("selection-token"))
                                .thenReturn(List.of());

                mockMvc.perform(
                                get("/api/v1/auth/organizations")
                                                .header(
                                                                "Authorization",
                                                                "Bearer selection-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isEmpty());

                verify(authService, times(1))
                                .getOrganizations("selection-token");
        }

        @Test
        void selectOrganization_shouldPassSelectionTokenAndRequest()
                        throws Exception {

                SelectOrganizationRequest request = new SelectOrganizationRequest();

                request.setOrganizationId(orgId);

                SelectOrganizationResponse response = SelectOrganizationResponse.builder()
                                .accessToken("access-token")
                                .tokenType("Bearer")
                                .expiresIn(3600L)
                                .build();

                when(authService.selectOrganization(
                                eq("selection-token"),
                                any(SelectOrganizationRequest.class))).thenReturn(response);

                mockMvc.perform(
                                post("/api/v1/auth/select-organization")
                                                .header(
                                                                "Authorization",
                                                                "Bearer selection-token")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                                objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.accessToken")
                                                .value("access-token"))
                                .andExpect(jsonPath("$.data.tokenType")
                                                .value("Bearer"))
                                .andExpect(jsonPath("$.data.expiresIn")
                                                .value(3600));

                verify(authService, times(1)).selectOrganization(
                                eq("selection-token"),
                                any(SelectOrganizationRequest.class));
        }

        @Test
        void switchOrganization_shouldIssueNewAccessToken()
                        throws Exception {

                SelectOrganizationRequest request = new SelectOrganizationRequest();

                request.setOrganizationId(orgId);

                SelectOrganizationResponse response = SelectOrganizationResponse.builder()
                                .accessToken("new-access-token")
                                .tokenType("Bearer")
                                .expiresIn(3600L)
                                .build();

                when(authService.switchOrganization(
                                eq(userId),
                                any(SelectOrganizationRequest.class))).thenReturn(response);

                mockMvc.perform(
                                post("/api/v1/auth/switch-organization")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                                objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.accessToken")
                                                .value("new-access-token"))
                                .andExpect(jsonPath("$.data.tokenType")
                                                .value("Bearer"))
                                .andExpect(jsonPath("$.data.expiresIn")
                                                .value(3600));

                verify(authService, times(1)).switchOrganization(
                                eq(userId),
                                any(SelectOrganizationRequest.class));
        }
}
