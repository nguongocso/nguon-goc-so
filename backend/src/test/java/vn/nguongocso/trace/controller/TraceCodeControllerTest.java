package vn.nguongocso.trace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
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
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.ExportTraceCodesRequest;
import vn.nguongocso.trace.dto.response.HistoryEvent;
import vn.nguongocso.trace.dto.response.TraceCodeHistoryResponse;
import vn.nguongocso.trace.dto.response.TraceCodeSummaryResponse;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.service.TraceCodeStatusService;

/**
 * Unit test cho {@link TraceCodeController} (NCL-04-CN-008).
 */
@WebMvcTest(TraceCodeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TraceCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TraceCodeStatusService traceCodeStatusService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    @Test
    @WithMockUser(roles = "VT-02")
    @DisplayName("GET /shipments/{id}/trace-codes - Thành công với quyền VT-02")
    void getShipmentTraceCodes_success() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        TraceCodeSummaryResponse summary = TraceCodeSummaryResponse.builder()
                .id(UUID.randomUUID())
                .codeValue("HTX-001")
                .status(TraceCodeStatus.ACTIVE)
                .scanCount(2)
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<TraceCodeSummaryResponse> pageResponse = PageResponse.<TraceCodeSummaryResponse>builder()
                .items(List.of(summary))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(traceCodeStatusService.getTraceCodesByShipment(eq(shipmentId), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/trace-codes", shipmentId)
                        .param("status", "ACTIVE")
                        .param("search", "HTX")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].codeValue").value("HTX-001"))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].scanCount").value(2));
    }

    @Test
    @WithMockUser(roles = "VT-03")
    @DisplayName("GET /shipments/{id}/trace-codes - Bị từ chối (403) khi không phải VT-02")
    void getShipmentTraceCodes_forbidden_whenNotVT02() throws Exception {
        UUID shipmentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/trace-codes", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VT-02")
    @DisplayName("GET /trace-codes/{codeValue}/history - Thành công với quyền VT-02")
    void getTraceCodeHistory_success() throws Exception {
        TraceCodeHistoryResponse historyResponse = TraceCodeHistoryResponse.builder()
                .codeValue("HTX-001")
                .status(TraceCodeStatus.ACTIVE)
                .shipmentName("Lô hàng 01")
                .scanCount(1)
                .events(List.of(HistoryEvent.builder()
                        .type("CREATED")
                        .timestamp(LocalDateTime.now())
                        .details("Khởi tạo mã tem")
                        .actorName("Quản lý")
                        .build()))
                .build();

        when(traceCodeStatusService.getTraceCodeHistory(eq("HTX-001"), any()))
                .thenReturn(historyResponse);

        mockMvc.perform(get("/api/v1/trace-codes/{codeValue}/history", "HTX-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codeValue").value("HTX-001"))
                .andExpect(jsonPath("$.data.events[0].type").value("CREATED"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    @DisplayName("POST /shipments/{id}/trace-codes/export - Xuất file CSV thành công")
    void exportTraceCodes_success() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        byte[] csvBytes = "\ufeffSTT,Mã tem\n1,HTX-001".getBytes(StandardCharsets.UTF_8);

        when(traceCodeStatusService.exportTraceCodes(eq(shipmentId), any(), any()))
                .thenReturn(csvBytes);

        ExportTraceCodesRequest req = ExportTraceCodesRequest.builder()
                .status("ACTIVE")
                .build();

        mockMvc.perform(post("/api/v1/shipments/{shipmentId}/trace-codes/export", shipmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().bytes(csvBytes));
    }
}
