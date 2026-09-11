package vn.nguongocso.trace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.trace.dto.request.CancelHandoverRequest;
import vn.nguongocso.trace.dto.response.HandoverResponse;
import vn.nguongocso.trace.dto.response.HandoverSummaryResponse;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.service.ShipmentHandoverService;

@WebMvcTest(HandoverController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class HandoverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShipmentHandoverService handoverService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "buyer@example.com", roles = {"VT-04"})
    void testListHandovers_Success_VT04() throws Exception {
        UUID id = UUID.randomUUID();
        HandoverSummaryResponse summary = HandoverSummaryResponse.builder()
                .id(id)
                .shipmentId(UUID.randomUUID())
                .shipmentName("Lô cam Vinh")
                .fromOrganizationName("HTX Nông Nghiệp Hòa Bình")
                .toOrganizationName("Công ty Thực Phẩm Sạch")
                .quantity(500L)
                .unit("kg")
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<HandoverSummaryResponse> pageResponse = PageResponse.<HandoverSummaryResponse>builder()
                .items(List.of(summary))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(handoverService.listForCurrentOrganization(anyString(), anyString(), anyInt(), anyInt(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/handovers")
                        .param("status", "PENDING")
                        .param("search", "cam")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].shipmentName").value("Lô cam Vinh"))
                .andExpect(jsonPath("$.data.items[0].fromOrganizationName").value("HTX Nông Nghiệp Hòa Bình"));
    }

    @Test
    @WithMockUser(username = "coop@example.com", roles = {"VT-02"})
    void testListHandovers_Success_VT02() throws Exception {
        UUID id = UUID.randomUUID();
        HandoverSummaryResponse summary = HandoverSummaryResponse.builder()
                .id(id)
                .shipmentId(UUID.randomUUID())
                .shipmentName("Lô cam Vinh")
                .fromOrganizationName("HTX Nông Nghiệp Hòa Bình")
                .toOrganizationName("Công ty Thực Phẩm Sạch")
                .quantity(500L)
                .unit("kg")
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<HandoverSummaryResponse> pageResponse = PageResponse.<HandoverSummaryResponse>builder()
                .items(List.of(summary))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(handoverService.listForCurrentOrganization(anyString(), anyString(), anyInt(), anyInt(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/handovers")
                        .param("status", "PENDING")
                        .param("search", "cam")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].shipmentName").value("Lô cam Vinh"))
                .andExpect(jsonPath("$.data.items[0].toOrganizationName").value("Công ty Thực Phẩm Sạch"));
    }

    @Test
    @WithMockUser(username = "farmer@example.com", roles = {"VT-03"})
    void testListHandovers_Forbidden_VT03() throws Exception {
        mockMvc.perform(get("/api/v1/handovers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = {"VT-04"})
    void testGetById_Success() throws Exception {
        UUID id = UUID.randomUUID();
        HandoverResponse response = HandoverResponse.builder()
                .id(id)
                .shipmentName("Lô cam Vinh")
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .build();

        when(handoverService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/handovers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = {"VT-04"})
    void testAcceptHandover_Success() throws Exception {
        UUID id = UUID.randomUUID();
        HandoverResponse response = HandoverResponse.builder()
                .id(id)
                .status(ShipmentHandoverStatus.ACCEPTED)
                .build();

        when(handoverService.accept(id)).thenReturn(response);

        mockMvc.perform(post("/api/v1/handovers/{id}/accept", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = {"VT-04"})
    void testRejectHandover_Success() throws Exception {
        UUID id = UUID.randomUUID();
        CancelHandoverRequest request = new CancelHandoverRequest();
        request.setReason("Hàng hóa bị dập nát");

        HandoverResponse response = HandoverResponse.builder()
                .id(id)
                .status(ShipmentHandoverStatus.REJECTED)
                .cancelReason("Hàng hóa bị dập nát")
                .build();

        when(handoverService.reject(eq(id), any(CancelHandoverRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/handovers/{id}/reject", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}
