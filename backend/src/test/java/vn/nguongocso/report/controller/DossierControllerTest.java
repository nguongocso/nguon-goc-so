package vn.nguongocso.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.report.exception.DossierValidationException;
import vn.nguongocso.report.dto.response.DossierCheckResponse;
import vn.nguongocso.report.service.DossierService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
/**
 * Test controller DossierController.
 *
 * @author Triệu Văn Đại
 */
@WebMvcTest(DossierController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class DossierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DossierService dossierService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private PermissionChecker permissionChecker;

    private UUID shipmentId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        SecurityContextHolder.clearContext();

        // Mock thông tin xác thực cho Spring Security
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(UUID.randomUUID());
        when(userDetails.getOrganizationId()).thenReturn(UUID.randomUUID());
        when(userDetails.getRoleCode()).thenReturn("VT-02");

        // ✅ SỬ DỤNG doReturn().when() ĐỂ TRÁNH LỖI GENERIC COMPILER
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_VT-02")))
                .when(userDetails).getAuthorities();

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    @Test
    void checkEligibility_shouldReturnOk_whenEligible() throws Exception {
        DossierCheckResponse mockResponse = DossierCheckResponse.builder()
                .shipmentId(shipmentId)
                .eligible(true)
                .missingDocuments(Collections.emptyList())
                .build();

        when(dossierService.checkEligibility(eq(shipmentId), any(CustomUserDetails.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/dossier/check", shipmentId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.missingDocuments").isEmpty());
    }

    @Test
    void checkEligibility_shouldReturnBadRequest_whenMissingDocuments() throws Exception {
        List<String> errors = List.of(
                "Thiếu chứng từ bón phân (FERTILIZING)",
                "Thiếu chứng từ thu hoạch (HARVESTING)"
        );

        // Giả lập service ném ra lỗi thẩm định khi check
        when(dossierService.checkEligibility(eq(shipmentId), any(CustomUserDetails.class)))
                .thenThrow(new DossierValidationException("Không đủ điều kiện xuất hồ sơ truy xuất.", errors));

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/dossier/check", shipmentId)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Không đủ điều kiện xuất hồ sơ truy xuất."))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").value("Thiếu chứng từ bón phân (FERTILIZING)"))
                .andExpect(jsonPath("$.errors[1]").value("Thiếu chứng từ thu hoạch (HARVESTING)"));
    }

    @Test
    void exportDossierPdf_shouldReturnPdfStream_whenSuccessful() throws Exception {
        byte[] mockPdfBytes = new byte[]{1, 2, 3, 4}; // Giả lập dữ liệu file PDF

        when(dossierService.exportDossierPdf(eq(shipmentId), any(CustomUserDetails.class), any(String.class)))
                .thenReturn(mockPdfBytes);

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/dossier/export", shipmentId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(mockPdfBytes));
    }
}
