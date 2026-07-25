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
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.dto.response.PackagingCheckResult;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.service.ProductionLotService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID lotId = UUID.randomUUID();

    private void setSecurityContextWithRole(String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getOrganizationId()).thenReturn(orgId);
        when(userDetails.getRoleCode()).thenReturn(roleCode);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getFullName()).thenReturn("Test User");

        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode)))
                .when(userDetails).getAuthorities();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ========== Existing Tests ==========

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

    // ========== Packaging Check Tests ==========

    @Test
    void checkPackaging_shouldReturnOk_whenLotIsHarvestedAndReady() throws Exception {
        setSecurityContextWithRole("VT-02");

        PackagingCheckResult result = PackagingCheckResult.builder()
                .lotId(lotId)
                .status(ProductionLotStatus.HARVESTED)
                .canPackage(true)
                .missingLogs(Collections.emptyList())
                .message("Lô sản xuất đã sẵn sàng để đóng gói")
                .build();

        when(productionLotService.checkPackagingReadiness(lotId)).thenReturn(result);

        mockMvc.perform(get("/api/v1/production-lots/{lotId}/packaging-check", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canPackage").value(true))
                .andExpect(jsonPath("$.data.missingLogs").isEmpty());
    }

    @Test
    void checkPackaging_shouldReturnOk_whenLotIsHarvestedAndNotReady() throws Exception {
        setSecurityContextWithRole("VT-02");

        PackagingCheckResult result = PackagingCheckResult.builder()
                .lotId(lotId)
                .status(ProductionLotStatus.HARVESTED)
                .canPackage(false)
                .missingLogs(List.of("FERTILIZING", "PESTICIDE"))
                .message("Thiếu nhật ký canh tác bắt buộc: FERTILIZING, PESTICIDE")
                .build();

        when(productionLotService.checkPackagingReadiness(lotId)).thenReturn(result);

        mockMvc.perform(get("/api/v1/production-lots/{lotId}/packaging-check", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canPackage").value(false))
                .andExpect(jsonPath("$.data.missingLogs.length()").value(2))
                .andExpect(jsonPath("$.data.missingLogs[0]").value("FERTILIZING"))
                .andExpect(jsonPath("$.data.missingLogs[1]").value("PESTICIDE"));
    }

    @Test
    void checkPackaging_shouldReturnBadRequest_whenLotNotHarvested() throws Exception {
        setSecurityContextWithRole("VT-02");

        when(productionLotService.checkPackagingReadiness(lotId))
                .thenThrow(new BusinessException("Lô sản xuất chưa sẵn sàng để đóng gói. Trạng thái phải là HARVESTED."));

        mockMvc.perform(get("/api/v1/production-lots/{lotId}/packaging-check", lotId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lô sản xuất chưa sẵn sàng để đóng gói. Trạng thái phải là HARVESTED."));
    }

    @Test
    void checkPackaging_shouldReturnBadRequest_whenLotNotFound() throws Exception {
        setSecurityContextWithRole("VT-02");

        when(productionLotService.checkPackagingReadiness(lotId))
                .thenThrow(new BusinessException("Không tìm thấy lô sản xuất"));

        mockMvc.perform(get("/api/v1/production-lots/{lotId}/packaging-check", lotId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không tìm thấy lô sản xuất"));
    }

    @Test
    void checkPackaging_shouldReturnForbidden_whenUnauthorizedRole() throws Exception {
        setSecurityContextWithRole("VT-03");

        mockMvc.perform(get("/api/v1/production-lots/{lotId}/packaging-check", lotId))
                .andExpect(status().isForbidden());
    }

    // ========== Package Lot Tests ==========

    @Test
    void packageLot_shouldReturnOk_whenAllRequiredLogsExist() throws Exception {
        setSecurityContextWithRole("VT-02");

        CreateProductionLotResponse response = CreateProductionLotResponse.builder()
                .id(lotId)
                .status(ProductionLotStatus.PACKAGED.name())
                .name("Test lot")
                .build();

        when(productionLotService.packageLot(eq(lotId), any(CustomUserDetails.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/production-lots/{lotId}/package", lotId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PACKAGED"));
    }

    @Test
    void packageLot_shouldReturnBadRequest_whenMissingRequiredLogs() throws Exception {
        setSecurityContextWithRole("VT-02");

        when(productionLotService.packageLot(eq(lotId), any(CustomUserDetails.class)))
                .thenThrow(new BusinessException("Không thể đóng gói. Thiếu nhật ký canh tác bắt buộc: FERTILIZING, PESTICIDE"));

        mockMvc.perform(post("/api/v1/production-lots/{lotId}/package", lotId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể đóng gói. Thiếu nhật ký canh tác bắt buộc: FERTILIZING, PESTICIDE"));
    }

    @Test
    void packageLot_shouldReturnForbidden_whenNotManager() throws Exception {
        setSecurityContextWithRole("VT-03");

        mockMvc.perform(post("/api/v1/production-lots/{lotId}/package", lotId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}