package vn.nguongocso.unit.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.organization.controller.OrganizationProfileController;
import vn.nguongocso.organization.dto.request.OrganizationUpdateRequest;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.service.OrganizationService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationProfileController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class OrganizationProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private PermissionChecker permissionChecker;

    private final UUID orgId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "VT-02")
    void getProfile_shouldReturnOk() throws Exception {
        OrganizationProfileResponse response = OrganizationProfileResponse.builder()
                .organizationId(orgId)
                .name("HTX Xanh")
                .code("HTX001")
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .address("Số 1, đường A")
                .phone("0900000000")
                .email("htx@example.com")
                .build();

        when(organizationService.getCurrentOrganizationProfile()).thenReturn(response);

        mockMvc.perform(get("/api/v1/organizations/profile")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("HTX Xanh"))
                .andExpect(jsonPath("$.data.code").value("HTX001"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void updateProfile_shouldReturnOk_whenValidData() throws Exception {
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("HTX Xanh Mới");
        request.setAddress("Số 2, đường B");
        request.setPhone("0987654321");
        request.setEmail("new@htx.com");

        OrganizationProfileResponse response = OrganizationProfileResponse.builder()
                .organizationId(orgId)
                .name("HTX Xanh Mới")
                .code("HTX001")
                .type(OrganizationType.COOPERATIVE)
                .status(OrganizationStatus.ACTIVE)
                .address("Số 2, đường B")
                .phone("0987654321")
                .email("new@htx.com")
                .build();

        when(organizationService.updateCurrentOrganization(any(OrganizationUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/organizations/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("HTX Xanh Mới"))
                .andExpect(jsonPath("$.data.phone").value("0987654321"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void updateProfile_shouldReturnBadRequest_whenInvalidEmail() throws Exception {
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("HTX Xanh");
        request.setEmail("invalid-email");

        mockMvc.perform(put("/api/v1/organizations/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value(Matchers.containsString("Email không hợp lệ")));
    }

    @Test
    @WithMockUser(roles = "VT-03")
    void updateProfile_withoutPermission_shouldReturnForbidden() throws Exception {
        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("HTX Xanh");

        mockMvc.perform(put("/api/v1/organizations/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}