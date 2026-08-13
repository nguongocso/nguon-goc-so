package vn.nguongocso.trace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.dto.request.CreateCodeRangeRequest;
import vn.nguongocso.trace.dto.response.CodeRangeResponse;
import vn.nguongocso.trace.service.CodeRangeService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CodeRangeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CodeRangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CodeRangeService codeRangeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-01")
    void createCodeRange_shouldReturnOk_whenValid() throws Exception {
        CreateCodeRangeRequest request = new CreateCodeRangeRequest();
        request.setOrganizationId(UUID.randomUUID());
        request.setPrefix("893001");
        request.setTotalLimit(1000L);

        CodeRangeResponse response = CodeRangeResponse.builder()
                .id(UUID.randomUUID())
                .organizationId(request.getOrganizationId())
                .organizationName("HTX Xanh")
                .prefix("893001")
                .totalLimit(1000L)
                .usedCount(0L)
                .build();

        when(codeRangeService.createCodeRange(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/code-ranges")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prefix").value("893001"))
                .andExpect(jsonPath("$.data.totalLimit").value(1000));
    }

    @Test
    @WithMockUser(roles = "VT-01")
    void createCodeRange_shouldReturnBadRequest_whenPrefixExists() throws Exception {
        CreateCodeRangeRequest request = new CreateCodeRangeRequest();
        request.setOrganizationId(UUID.randomUUID());
        request.setPrefix("893001");
        request.setTotalLimit(1000L);

        when(codeRangeService.createCodeRange(any(), any()))
                .thenThrow(new BusinessException("Tiền tố mã đã tồn tại"));

        mockMvc.perform(post("/api/v1/admin/code-ranges")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tiền tố mã đã tồn tại"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void createCodeRange_shouldReturnForbidden_whenNotAdmin() throws Exception {
        CreateCodeRangeRequest request = new CreateCodeRangeRequest();
        request.setOrganizationId(UUID.randomUUID());
        request.setPrefix("893001");
        request.setTotalLimit(1000L);

        mockMvc.perform(post("/api/v1/admin/code-ranges")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}