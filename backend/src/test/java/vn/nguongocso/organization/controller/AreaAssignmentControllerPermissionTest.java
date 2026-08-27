package vn.nguongocso.organization.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

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
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.organization.dto.response.AssignAreasResult;
import vn.nguongocso.organization.dto.response.AssignedAreaResponse;
import vn.nguongocso.organization.dto.response.RegulatorUserResponse;
import vn.nguongocso.organization.dto.response.UnassignAreaResult;
import vn.nguongocso.organization.service.AdministrativeUnitService;
import vn.nguongocso.organization.service.AreaAssignmentService;

/**
 * Kiểm tra ma trận quyền NCL-743 trên tầng controller (mirror
 * {@code CropAreaAnalysisControllerTest}).
 */
@WebMvcTest(controllers = {
        AreaAssignmentAdminController.class,
        AdministrativeUnitController.class,
        MeAreaController.class,
        OrganizationDivisionAdminController.class
})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AreaAssignmentControllerPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AreaAssignmentService areaAssignmentService;

    @MockitoBean
    private AdministrativeUnitService administrativeUnitService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String UNIT_ID = UUID.randomUUID().toString();
    private static final String ORG_ID = UUID.randomUUID().toString();

    // ==================== VT-01 được phép ====================

    @Test
    @WithMockUser(roles = "VT-01")
    void vt01_canListRegulators_andSeeAssignments() throws Exception {
        whenListRegulators();
        whenAssignedAreas();

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(csrf())
                        .param("role", "VT-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].username").value("vt05-user"));

        mockMvc.perform(get("/api/v1/admin/users/" + USER_ID + "/areas").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].unitCode").value("04098"));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    void vt01_canAssignAndUnassignAndUpdateDivisions() throws Exception {
        whenAssignAreas();

        mockMvc.perform(post("/api/v1/admin/users/" + USER_ID + "/areas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitIds\":[\"" + UNIT_ID + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedCount").value(1));

        whenUnassignArea();
        mockMvc.perform(delete("/api/v1/admin/users/" + USER_ID + "/areas/" + UNIT_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message")
                        .value("Đã gỡ địa bàn Hoa Lư khỏi tài khoản."));

        mockMvc.perform(put("/api/v1/admin/organizations/" + ORG_ID + "/divisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provinceId\":null,\"communeId\":null}"))
                .andExpect(status().isOk());
    }

    // ==================== VT-02 / vai trò khác bị chặn ====================

    @Test
    @WithMockUser(roles = "VT-02")
    void vt02_isForbiddenOnAllAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").with(csrf()).param("role", "VT-05"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users/" + USER_ID + "/areas").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/users/" + USER_ID + "/areas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitIds\":[\"" + UNIT_ID + "\"]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/admin/users/" + USER_ID + "/areas/" + UNIT_ID).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/organizations/" + ORG_ID + "/divisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provinceId\":null,\"communeId\":null}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VT-05")
    void vt05_cannotUseAdminEndpoints_butCanUseTreeAndMe() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").with(csrf()).param("role", "VT-05"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/users/" + USER_ID + "/areas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitIds\":[\"" + UNIT_ID + "\"]}"))
                .andExpect(status().isForbidden());

        whenMyAreas();
        mockMvc.perform(get("/api/v1/me/areas").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].unitName").value("Hoa Lư"));

        mockMvc.perform(get("/api/v1/administrative-units/tree").with(csrf()))
                .andExpect(status().isOk());
    }

    // ==================== chưa đăng nhập ====================

    @Test
    void anonymous_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").param("role", "VT-05"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/me/areas"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/administrative-units/tree"))
                .andExpect(status().isForbidden());
    }

    // ==================== stub helpers ====================

    private void whenListRegulators() {
        RegulatorUserResponse item = RegulatorUserResponse.builder()
                .userId(UUID.fromString(USER_ID))
                .username("vt05-user")
                .fullName("Nguyễn Văn Cán Bộ")
                .organizationName("Sở NN&PTNT")
                .build();
        org.mockito.Mockito.doReturn(
                new PageResponse<>(List.of(item), 0, 20, 1L, 1, true, true))
                .when(areaAssignmentService).listRegulators(nullable(String.class), any());
    }

    private void whenAssignedAreas() {
        org.mockito.Mockito.when(areaAssignmentService.getAssignedAreas(any(UUID.class)))
                .thenReturn(List.of(AssignedAreaResponse.builder()
                        .assignmentId(UUID.randomUUID())
                        .unitId(UUID.fromString(UNIT_ID))
                        .unitCode("04098")
                        .unitName("Hoa Lư")
                        .unitLevel("COMMUNE")
                        .assignedAt(java.time.LocalDateTime.now())
                        .build()));
    }

    private void whenAssignAreas() {
        org.mockito.Mockito.when(areaAssignmentService.assignAreas(any(), any(UUID.class), any()))
                .thenReturn(AssignAreasResult.builder()
                        .assignedCount(1)
                        .assigned(List.of(AssignedAreaResponse.builder()
                                .unitId(UUID.fromString(UNIT_ID))
                                .unitCode("04098")
                                .unitName("Hoa Lư")
                                .build()))
                        .message("Đã gán 1 địa bàn cho tài khoản.")
                        .build());
    }

    private void whenUnassignArea() {
        org.mockito.Mockito.when(areaAssignmentService.unassignArea(any(), any(UUID.class), any(UUID.class)))
                .thenReturn(new UnassignAreaResult("Đã gỡ địa bàn Hoa Lư khỏi tài khoản."));
    }

    private void whenMyAreas() {
        org.mockito.Mockito.when(areaAssignmentService.getMyAreas(any()))
                .thenReturn(List.of(AssignedAreaResponse.builder()
                        .unitId(UUID.fromString(UNIT_ID))
                        .unitCode("04098")
                        .unitName("Hoa Lư")
                        .unitLevel("COMMUNE")
                        .assignedAt(java.time.LocalDateTime.now())
                        .build()));
    }
}
