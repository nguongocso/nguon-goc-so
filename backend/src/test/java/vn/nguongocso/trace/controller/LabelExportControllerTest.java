package vn.nguongocso.trace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.ExportLabelsRequest;
import vn.nguongocso.trace.service.LabelExportService;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LabelExportController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class LabelExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LabelExportService labelExportService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(UUID.randomUUID());
        when(userDetails.getOrganizationId()).thenReturn(UUID.randomUUID());
        when(userDetails.getRoleCode()).thenReturn("VT-02");

        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_VT-02")))
                .when(userDetails).getAuthorities();

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("exportLabels: Trả về 400 Bad Request khi count vượt quá 500 tem")
    void exportLabels_shouldReturnBadRequest_whenCountExceedsMax() throws Exception {
        UUID shipmentId = UUID.randomUUID();

        ExportLabelsRequest request = ExportLabelsRequest.builder()
                .startIndex(0)
                .count(501) // Vượt quá @Max(500)
                .labelSize("STANDARD")
                .build();

        mockMvc.perform(post("/api/v1/shipments/{shipmentId}/labels/export", shipmentId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
