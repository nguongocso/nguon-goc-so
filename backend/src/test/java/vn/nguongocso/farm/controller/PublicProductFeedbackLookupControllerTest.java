package vn.nguongocso.farm.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.PublicProductFeedbackLookupRequest;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackLookupResponse;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.service.ProductFeedbackService;

@WebMvcTest(PublicProductFeedbackLookupController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class PublicProductFeedbackLookupControllerTest {

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
    void lookup_shouldReturnOnlyPublicFields_withoutAuthentication() throws Exception {
        String lookupCode = "PA-7K2M-9Q4X-H8NP-3R5T";
        PublicProductFeedbackLookupRequest request = new PublicProductFeedbackLookupRequest();
        request.setLookupCode(lookupCode);
        when(productFeedbackService.lookupPublicFeedback(lookupCode)).thenReturn(
                PublicProductFeedbackLookupResponse.builder()
                        .status(ProductFeedbackStatus.IN_PROGRESS)
                        .publicResponse("Đơn vị phụ trách đang xác minh.")
                        .build());

        mockMvc.perform(post("/api/v1/public/product-feedbacks/lookup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.publicResponse").value("Đơn vị phụ trách đang xác minh."))
                .andExpect(jsonPath("$.data.processingContent").doesNotExist())
                .andExpect(jsonPath("$.data.assignedToUserId").doesNotExist())
                .andExpect(jsonPath("$.data.productionLotId").doesNotExist());
    }

    @Test
    void lookup_shouldReturnNotFound_withGenericMessage() throws Exception {
        PublicProductFeedbackLookupRequest request = new PublicProductFeedbackLookupRequest();
        request.setLookupCode("PA-7K2M-9Q4X-H8NP-3R5T");
        when(productFeedbackService.lookupPublicFeedback(request.getLookupCode()))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy phản ánh"));

        mockMvc.perform(post("/api/v1/public/product-feedbacks/lookup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy phản ánh"));
    }

    @Test
    void lookup_shouldRejectBlankCode() throws Exception {
        PublicProductFeedbackLookupRequest request = new PublicProductFeedbackLookupRequest();
        request.setLookupCode(" ");

        mockMvc.perform(post("/api/v1/public/product-feedbacks/lookup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
