package vn.nguongocso.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.report.dto.response.LookupResponse;
import vn.nguongocso.report.service.PublicLookupService;
import vn.nguongocso.trace.enums.TraceCodeStatus;

import java.util.UUID;

@WebMvcTest(PublicLookupController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@WithMockUser // Mặc định cho Spring Security filter bypass
public class PublicLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicLookupService publicLookupService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final String codeValue = "NCL00000001";

    @Test
    void lookupCode_shouldReturnOk_whenValidCode() throws Exception {
        // Given
        LookupResponse response = LookupResponse.builder()
                .codeValue(codeValue)
                .status(TraceCodeStatus.ACTIVE)
                .shipment(LookupResponse.ShipmentInfo.builder()
                        .id(UUID.randomUUID())
                        .name("Lô hàng cà chua")
                        .build())
                .build();

        when(publicLookupService.lookupCode(eq(codeValue), any(), any(), any(), any(), any()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/public/api/v1/trace-codes/{codeValue}", codeValue)
                        .with(csrf())
                        .param("latitude", "21.0285")
                        .param("longitude", "105.8048")
                        .param("location", "Hà Nội"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codeValue").value(codeValue))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void lookupCode_shouldReturnNotFound_whenCodeNotExist() throws Exception {
        // Given
        when(publicLookupService.lookupCode(eq(codeValue), any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy mã truy xuất"));

        // When / Then
        mockMvc.perform(get("/public/api/v1/trace-codes/{codeValue}", codeValue)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy mã truy xuất"));
    }
}
