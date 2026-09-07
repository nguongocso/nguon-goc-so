package vn.nguongocso.farm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.farm.dto.request.AssignProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CloseProductFeedbackRequest;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRecallRequest;
import vn.nguongocso.farm.dto.request.UpdateProductFeedbackProcessingRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.service.ProductFeedbackService;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.recall.dto.response.RecallRequestResponse;

@WebMvcTest(ProductFeedbackManagementController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class ProductFeedbackManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductFeedbackService productFeedbackService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-02")
    void getFeedbacks_shouldReturnList_whenAuthorized() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        ProductFeedbackResponse item = ProductFeedbackResponse.builder()
                .id(feedbackId)
                .content("Nghi ngờ chất lượng")
                .status(ProductFeedbackStatus.NEW)
                .severity(ProductFeedbackSeverity.INFORMATION)
                .build();
        PageResponse<ProductFeedbackResponse> pageResponse = new PageResponse<>(
                List.of(item), 0, 20, 1, 1, true, true);

        when(productFeedbackService.getFeedbacks(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/product-feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(feedbackId.toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("NEW"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void getFeedbackById_shouldReturnDetail_whenFound() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        ProductFeedbackResponse response = ProductFeedbackResponse.builder()
                .id(feedbackId)
                .content("Chi tiết phản ánh")
                .status(ProductFeedbackStatus.IN_PROGRESS)
                .severity(ProductFeedbackSeverity.QUALITY_SUSPECTED)
                .build();

        when(productFeedbackService.getFeedbackById(feedbackId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/product-feedbacks/{feedbackId}", feedbackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(feedbackId.toString()))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void assign_shouldReturnUpdatedFeedback_whenValid() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AssignProductFeedbackRequest request = new AssignProductFeedbackRequest();
        request.setAssignedToUserId(userId);

        ProductFeedbackResponse response = ProductFeedbackResponse.builder()
                .id(feedbackId)
                .assignedToUserId(userId)
                .status(ProductFeedbackStatus.IN_PROGRESS)
                .build();

        when(productFeedbackService.assign(eq(feedbackId), any(AssignProductFeedbackRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/product-feedbacks/{feedbackId}/assignment", feedbackId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignedToUserId").value(userId.toString()));

        verify(permissionChecker).check("product_feedback", "UPDATE");
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void updateProcessing_shouldReturnUpdatedFeedback_whenValid() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        UpdateProductFeedbackProcessingRequest request = new UpdateProductFeedbackProcessingRequest();
        request.setSeverity(ProductFeedbackSeverity.QUALITY_SUSPECTED);
        request.setProcessingContent("Đã kiểm tra lô hàng");
        request.setPublicResponse("Đang xử lý");

        ProductFeedbackResponse response = ProductFeedbackResponse.builder()
                .id(feedbackId)
                .severity(ProductFeedbackSeverity.QUALITY_SUSPECTED)
                .processingContent("Đã kiểm tra lô hàng")
                .status(ProductFeedbackStatus.IN_PROGRESS)
                .build();

        when(productFeedbackService.updateProcessing(eq(feedbackId), any(UpdateProductFeedbackProcessingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/product-feedbacks/{feedbackId}/processing", feedbackId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.severity").value("QUALITY_SUSPECTED"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void close_shouldReturnClosedFeedback_whenValid() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        CloseProductFeedbackRequest request = new CloseProductFeedbackRequest();
        request.setCloseReason("Đã giải quyết xong khiếu nại");

        ProductFeedbackResponse response = ProductFeedbackResponse.builder()
                .id(feedbackId)
                .status(ProductFeedbackStatus.CLOSED)
                .closeReason("Đã giải quyết xong khiếu nại")
                .build();

        when(productFeedbackService.close(eq(feedbackId), any(CloseProductFeedbackRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/product-feedbacks/{feedbackId}/close", feedbackId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void createRecallRequest_shouldReturnCreated_whenValid() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        UUID recallId = UUID.randomUUID();
        CreateProductFeedbackRecallRequest request = new CreateProductFeedbackRecallRequest();
        request.setReason("Nghi ngờ chất lượng sản phẩm nghiêm trọng");

        RecallRequestResponse response = RecallRequestResponse.builder()
                .id(recallId)
                .sourceFeedbackId(feedbackId)
                .status("PENDING")
                .build();

        when(productFeedbackService.createRecallRequest(eq(feedbackId), any(CreateProductFeedbackRecallRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/product-feedbacks/{feedbackId}/recall-requests", feedbackId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(recallId.toString()))
                .andExpect(jsonPath("$.data.sourceFeedbackId").value(feedbackId.toString()));
    }

    @Test
    @WithMockUser(roles = "VT-04")
    void getFeedbacks_shouldReturnForbidden_whenRoleNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/product-feedbacks"))
                .andExpect(status().isForbidden());
    }
}
