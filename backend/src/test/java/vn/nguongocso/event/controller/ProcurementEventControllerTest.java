package vn.nguongocso.event.controller;

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
import vn.nguongocso.event.dto.request.RecordProcurementEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.ProcurementEventService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcurementEventController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ProcurementEventControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProcurementEventService procurementEventService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-04")
    void recordProcurement_shouldReturn201() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);
        request.setNotes("OK");

        ChainEventResponse response = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .shipmentId(shipmentId)
                .eventType(ChainEventType.PROCUREMENT)
                .build();

        when(procurementEventService.recordProcurementEvent(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/chain-events/procurement")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("PROCUREMENT"));
    }

    @Test
    @WithMockUser(roles = "VT-04")
    void recordProcurement_shouldReturn400_whenShipmentNotFound() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);

        when(procurementEventService.recordProcurementEvent(any(), any()))
                .thenThrow(new BusinessException("Không tìm thấy lô hàng."));

        mockMvc.perform(post("/api/v1/chain-events/procurement")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không tìm thấy lô hàng."));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void recordProcurement_shouldReturn403() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        RecordProcurementEventRequest request = new RecordProcurementEventRequest();
        request.setShipmentId(shipmentId);
        request.setReceivedQuantity(100L);

        mockMvc.perform(post("/api/v1/chain-events/procurement")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}