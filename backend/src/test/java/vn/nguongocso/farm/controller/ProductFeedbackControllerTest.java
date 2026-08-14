package vn.nguongocso.farm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.service.ProductFeedbackService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductFeedbackController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ProductFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductFeedbackService productFeedbackService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void sendFeedback_shouldReturnOk_whenValidRequest() throws Exception {
        UUID lotId = UUID.randomUUID();
        CreateProductFeedbackRequest request = new CreateProductFeedbackRequest();
        request.setContent("Thông tin sản phẩm bị sai lệch");

        ProductFeedbackResponse response = ProductFeedbackResponse.builder()
                .id(UUID.randomUUID())
                .productionLotId(lotId)
                .productionLotName("Lô chè Long Cốc")
                .content(request.getContent())
                .build();

        when(productFeedbackService.createFeedback(eq(lotId), any(CreateProductFeedbackRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/public/production-lots/{productionLotId}/feedbacks", lotId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value(request.getContent()))
                .andExpect(jsonPath("$.data.productionLotName").value("Lô chè Long Cốc"));
    }

    @Test
    void sendFeedback_shouldReturnBadRequest_whenContentIsEmpty() throws Exception {
        UUID lotId = UUID.randomUUID();
        CreateProductFeedbackRequest request = new CreateProductFeedbackRequest();
        request.setContent(""); // Trống

        mockMvc.perform(post("/api/v1/public/production-lots/{productionLotId}/feedbacks", lotId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
