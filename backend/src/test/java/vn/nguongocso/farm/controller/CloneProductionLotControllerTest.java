package vn.nguongocso.farm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.UUID;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.certification.service.InspectionExpiryService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.farm.dto.request.CloneProductionLotRequest;
import vn.nguongocso.farm.dto.response.CloneProductionLotPreviewResponse;
import vn.nguongocso.farm.dto.response.CloneProductionLotResponse;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotImportHistoryRepository;
import vn.nguongocso.farm.service.ProductionLotImportService;
import vn.nguongocso.farm.service.ProductionLotService;
import vn.nguongocso.permission.service.PermissionChecker;

/**
 * Test phân quyền cho tạo lô sản xuất từ mẫu vụ trước (NCL-02-CN-007).
 *
 * <p>
 * Chỉ Quản lý hợp tác xã (VT-02) được xem trước và tạo lô từ mẫu.
 * </p>
 */
@WebMvcTest(ProductionLotController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class CloneProductionLotControllerTest {

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

    /** Mock thêm sau khi merge develop: controller có thêm endpoint kiểm tra hạn kiểm nghiệm (NCL-11-CN-004). */
    @MockitoBean
    private InspectionExpiryService inspectionExpiryService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private CustomUserDetails buildUserDetails(String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getOrganizationId()).thenReturn(orgId);
        when(userDetails.getRoleCode()).thenReturn(roleCode);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getFullName()).thenReturn("Test User");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode)))
                .when(userDetails).getAuthorities();
        return userDetails;
    }

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clonePreview_shouldReturnOk_whenUserIsVt02() throws Exception {
        CustomUserDetails vt02 = buildUserDetails("VT-02");
        UUID sourceLotId = UUID.randomUUID();
        CloneProductionLotPreviewResponse preview = CloneProductionLotPreviewResponse.builder()
                .sourceLotId(sourceLotId)
                .sourceLotName("Lo mau")
                .build();
        when(productionLotService.getClonePreview(eq(sourceLotId), any(CustomUserDetails.class)))
                .thenReturn(preview);

        mockMvc.perform(get("/api/v1/production-lots/{sourceLotId}/clone-preview", sourceLotId)
                .with(user(vt02)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceLotId").value(sourceLotId.toString()));
    }

    @Test
    void clone_shouldReturnOk_whenUserIsVt02() throws Exception {
        CustomUserDetails vt02 = buildUserDetails("VT-02");
        UUID sourceLotId = UUID.randomUUID();
        UUID newLotId = UUID.randomUUID();

        CloneProductionLotRequest request = new CloneProductionLotRequest();
        request.setName("Lo moi");
        request.setExpectedQuantity(100.0);
        request.setExpectedQuantityUnit("kg");

        CreateProductionLotResponse lot = CreateProductionLotResponse.builder()
                .id(newLotId)
                .name("Lo moi")
                .status(ProductionLotStatus.DRAFT.name())
                .build();
        CloneProductionLotResponse response = CloneProductionLotResponse.builder()
                .lot(lot)
                .copiedCertifications(Collections.emptyList())
                .skippedCertifications(Collections.emptyList())
                .warnings(Collections.emptyList())
                .build();
        when(productionLotService.cloneProductionLot(eq(sourceLotId), any(CloneProductionLotRequest.class),
                any(CustomUserDetails.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/production-lots/{sourceLotId}/clone", sourceLotId)
                .with(user(vt02))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lot.id").value(newLotId.toString()))
                .andExpect(jsonPath("$.data.lot.status").value("DRAFT"));
    }

    @Test
    void clonePreview_shouldForbid_whenUserIsNotVt02() throws Exception {
        CustomUserDetails vt03 = buildUserDetails("VT-03");
        UUID sourceLotId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/production-lots/{sourceLotId}/clone-preview", sourceLotId)
                .with(user(vt03)))
                .andExpect(status().isForbidden());
        verify(productionLotService, never()).getClonePreview(any(UUID.class), any(CustomUserDetails.class));
    }

    @Test
    void clone_shouldForbid_whenUserIsNotVt02() throws Exception {
        CustomUserDetails vt03 = buildUserDetails("VT-03");
        UUID sourceLotId = UUID.randomUUID();

        CloneProductionLotRequest request = new CloneProductionLotRequest();
        request.setName("Lo moi");
        request.setExpectedQuantity(100.0);
        request.setExpectedQuantityUnit("kg");

        mockMvc.perform(post("/api/v1/production-lots/{sourceLotId}/clone", sourceLotId)
                .with(user(vt03))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        verify(productionLotService, never()).cloneProductionLot(any(UUID.class),
                any(CloneProductionLotRequest.class), any(CustomUserDetails.class));
    }

    @Test
    void clone_shouldForbid_whenUserIsVt04() throws Exception {
        CustomUserDetails vt04 = buildUserDetails("VT-04");
        UUID sourceLotId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/production-lots/{sourceLotId}/clone-preview", sourceLotId)
                .with(user(vt04)))
                .andExpect(status().isForbidden());

        Authentication auth = new UsernamePasswordAuthenticationToken(vt04, null, vt04.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CloneProductionLotRequest request = new CloneProductionLotRequest();
        request.setName("Lo moi");
        request.setExpectedQuantity(100.0);
        request.setExpectedQuantityUnit("kg");

        mockMvc.perform(post("/api/v1/production-lots/{sourceLotId}/clone", sourceLotId)
                .with(user(vt04))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
