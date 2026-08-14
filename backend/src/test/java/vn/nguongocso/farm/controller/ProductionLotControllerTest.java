package vn.nguongocso.farm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.service.ProductionLotService;
import vn.nguongocso.farm.service.ProductionLotImportService;
import vn.nguongocso.farm.repository.ProductionLotImportHistoryRepository;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.report.dto.response.ProductionLotDashboardResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductionLotController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ProductionLotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductionLotService productionLotService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private PermissionChecker permissionChecker;

        @MockitoBean
        private ProductionLotImportService productionLotImportService;

        @MockitoBean
        private ProductionLotImportHistoryRepository importHistoryRepository;

    private UUID lotId;
    private CustomUserDetails userDetails;
    private UpdateProductionLotRequest validRequest;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();


    private void setSecurityContextWithRole(String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getOrganizationId()).thenReturn(orgId);
        when(userDetails.getRoleCode()).thenReturn(roleCode);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getFullName()).thenReturn("Test User");

        // ✅ Sửa: dùng Collections.singletonList
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode)))
                .when(userDetails).getAuthorities();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveProductionLot_shouldReturnOk_whenApproved() throws Exception {
        setSecurityContextWithRole("VT-02");

        UUID lotId = UUID.randomUUID();
        ApproveProductionLotRequest request = new ApproveProductionLotRequest();
        request.setApproved(true);

        CreateProductionLotResponse response = CreateProductionLotResponse.builder()
                .id(lotId)
                .status(ProductionLotStatus.APPROVED.name())
                .name("Test lot")
                .build();

        when(productionLotService.approveProductionLot(eq(lotId), any(ApproveProductionLotRequest.class), any(CustomUserDetails.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/production-lots/{id}/approve", lotId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void approveProductionLot_shouldReturnBadRequest_whenLotNotPending() throws Exception {
        setSecurityContextWithRole("VT-02");

        UUID lotId = UUID.randomUUID();
        ApproveProductionLotRequest request = new ApproveProductionLotRequest();
        request.setApproved(true);

        when(productionLotService.approveProductionLot(eq(lotId), any(ApproveProductionLotRequest.class), any(CustomUserDetails.class)))
                .thenThrow(new BusinessException("Chỉ có thể duyệt lô đang ở trạng thái chờ duyệt"));

        mockMvc.perform(post("/api/v1/production-lots/{id}/approve", lotId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ có thể duyệt lô đang ở trạng thái chờ duyệt"));
    }

    @Test
    @WithMockUser(roles = "VT-03")
    void approveProductionLot_shouldReturnForbidden_whenNotManager() throws Exception {
        UUID lotId = UUID.randomUUID();
        ApproveProductionLotRequest request = new ApproveProductionLotRequest();
        request.setApproved(true);

        mockMvc.perform(post("/api/v1/production-lots/{id}/approve", lotId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
    @BeforeEach
    void setUp() {
        lotId = UUID.randomUUID();

        // Khởi tạo user giả lập
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName("quanly_htx");

        Organization organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());

        Role role = new Role();
        role.setCode("VT-02");

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(organization);
        orgUser.setUser(user);
        orgUser.setRole(role);

        userDetails = new CustomUserDetails(user, orgUser, role);

        // Khởi tạo request hợp lệ
        validRequest = new UpdateProductionLotRequest();
        validRequest.setName("Lô nông sản ngon");
        validRequest.setFarmAreaId(UUID.randomUUID());
        validRequest.setProductCategoryId(UUID.randomUUID());
        validRequest.setExpectedQuantity(500.0);
        validRequest.setPlantingDate(LocalDate.now());
    }

    @Test
    void update_shouldReturn200_whenRequestIsValid() throws Exception {
        // Given
        UpdateProductionLotResponse response = UpdateProductionLotResponse.builder()
                .id(lotId)
                .name("Lô nông sản ngon")
                .farmAreaId(validRequest.getFarmAreaId())
                .productCategoryId(validRequest.getProductCategoryId())
                .expectedQuantity(500.0)
                .plantingDate(validRequest.getPlantingDate())
                .status("DRAFT")
                .updatedAt(LocalDateTime.now())
                .build();

        when(productionLotService.updateProductionLot(eq(lotId), any(UpdateProductionLotRequest.class), any(CustomUserDetails.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/production-lots/" + lotId)
                        .with(user(userDetails)) // Gán CustomUserDetails vào Authentication Principal
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Lô nông sản ngon"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void update_shouldReturn400_whenValidationFails() throws Exception {
        // Given: Tên lô trống và sản lượng âm
        UpdateProductionLotRequest invalidRequest = new UpdateProductionLotRequest();
        invalidRequest.setName("");
        invalidRequest.setFarmAreaId(UUID.randomUUID());
        invalidRequest.setProductCategoryId(UUID.randomUUID());
        invalidRequest.setExpectedQuantity(-10.0);
        invalidRequest.setPlantingDate(LocalDate.now());

        // When & Then
        mockMvc.perform(put("/api/v1/production-lots/" + lotId)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"));
    }

    @Test
    @WithMockUser(roles = "VT-06") // Vai trò Người tiêu dùng không có quyền sửa
    void update_shouldReturn403_whenUserHasNoPermission() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/production-lots/" + lotId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getDashboard_shouldReturn200AndData_whenUserHasRoleManager() throws Exception {
        setSecurityContextWithRole("VT-02");

        ProductionLotDashboardResponse response = ProductionLotDashboardResponse.builder()
                .summary(ProductionLotDashboardResponse.SummaryDto.builder()
                        .totalLots(10L)
                        .totalExpectedYield(1000.0)
                        .totalActualYield(900.0)
                        .build())
                .build();

        when(productionLotService.getDashboard(
                any(LocalDate.class), any(LocalDate.class), eq(orgId), eq("MONTH"), any(CustomUserDetails.class), any(String.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/production-lots/dashboard")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-07-31")
                        .param("organizationId", orgId.toString())
                        .param("groupBy", "MONTH")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.totalLots").value(10))
                .andExpect(jsonPath("$.data.summary.totalExpectedYield").value(1000.0));
    }

    @Test
    @WithMockUser(roles = "VT-03") // VT-03 không được phép truy cập
    void getDashboard_shouldReturn403Forbidden_whenRoleNotAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/production-lots/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboard_shouldReturn403Forbidden_whenServiceThrowsAccessDenied() throws Exception {
        setSecurityContextWithRole("VT-02");

        when(productionLotService.getDashboard(
                any(), any(), any(), any(), any(CustomUserDetails.class), any(String.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Từ chối truy cập"));

        mockMvc.perform(get("/api/v1/production-lots/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}