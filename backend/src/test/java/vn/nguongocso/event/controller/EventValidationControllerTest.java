package vn.nguongocso.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import vn.nguongocso.event.dto.response.LotValidationResponse;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventValidationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class EventValidationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private EventValidationService eventValidationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private PermissionChecker permissionChecker;

    @Test
    @WithMockUser(roles = "VT-02")
    void validateLot_Success() throws Exception {
        UUID lotId = UUID.randomUUID();
        LotValidationResponse response = LotValidationResponse.builder()
                .lotId(lotId)
                .eventType(ChainEventType.PACKAGING.name())
                .valid(true)
                .message("Lô sản xuất hợp lệ")
                .build();

        when(eventValidationService.validateLot(eq(lotId), eq(ChainEventType.PACKAGING), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/chain-events/validate-lot")
                        .param("lotId", lotId.toString())
                        .param("eventType", "PACKAGING")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void validateLot_MissingParam_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/chain-events/validate-lot")
                        .param("eventType", "PACKAGING") // missing lotId
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Thiếu tham số truy vấn bắt buộc 'lotId'"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void validateLot_TypeMismatch_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/chain-events/validate-lot")
                        .param("lotId", "invalid-uuid-string")
                        .param("eventType", "PACKAGING")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tham số 'lotId' có giá trị không hợp lệ (yêu cầu kiểu UUID)"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void validateLot_NonExistentPath_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/chain-events/non-existent-path")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Đường dẫn API hoặc tài nguyên không tồn tại"));
    }
}
