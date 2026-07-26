package vn.nguongocso.trace.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.trace.dto.response.CodeRangeResponse;
import vn.nguongocso.trace.service.CodeRangeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationCodeRangeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class OrganizationCodeRangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodeRangeService codeRangeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-02")
    void getOrganizationCodeRanges_shouldReturn200_whenVT02() throws Exception {
        UUID orgId = UUID.randomUUID();
        CodeRangeResponse cr1 = CodeRangeResponse.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .organizationName("HTX Xanh")
                .prefix("893001")
                .totalLimit(1000L)
                .usedCount(100L)
                .createdAt(LocalDateTime.now())
                .build();

        CodeRangeResponse cr2 = CodeRangeResponse.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .organizationName("HTX Xanh")
                .prefix("893002")
                .totalLimit(500L)
                .usedCount(50L)
                .createdAt(LocalDateTime.now())
                .build();

        when(codeRangeService.getCodeRangesForOrganization(any())).thenReturn(List.of(cr1, cr2));

        mockMvc.perform(get("/api/v1/organization/code-ranges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].prefix").value("893001"))
                .andExpect(jsonPath("$.data[1].prefix").value("893002"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void getOrganizationCodeRanges_shouldReturn200_whenEmpty() throws Exception {
        when(codeRangeService.getCodeRangesForOrganization(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/organization/code-ranges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    void getOrganizationCodeRanges_shouldReturn200_whenVT01() throws Exception {
        UUID orgId = UUID.randomUUID();
        CodeRangeResponse cr = CodeRangeResponse.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .organizationName("HTX Xanh")
                .prefix("893001")
                .totalLimit(1000L)
                .usedCount(100L)
                .build();

        when(codeRangeService.getCodeRangesForOrganization(any())).thenReturn(List.of(cr));

        mockMvc.perform(get("/api/v1/organization/code-ranges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].organizationId").value(orgId.toString()));
    }

    @Test
    @WithMockUser(roles = "VT-03")
    void getOrganizationCodeRanges_shouldReturn403_whenVT03() throws Exception {
        mockMvc.perform(get("/api/v1/organization/code-ranges"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrganizationCodeRanges_shouldReturn403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/organization/code-ranges"))
                .andExpect(status().isForbidden());
    }
}