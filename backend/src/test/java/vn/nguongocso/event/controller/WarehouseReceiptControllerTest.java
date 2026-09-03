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
import vn.nguongocso.event.dto.request.WarehouseReceiptRequest;
import vn.nguongocso.event.dto.response.WarehouseReceiptResponse;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.WarehouseReceiptService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseReceiptController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class WarehouseReceiptControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private WarehouseReceiptService warehouseReceiptService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    // TC-01: Successful flow — discrepancy = 0
    @Test
    @WithMockUser(roles = "VT-04")
    void recordWarehouseReceipt_shouldReturn201_whenNoDiscrepancy() throws Exception {
        WarehouseReceiptRequest request = new WarehouseReceiptRequest();
        request.setCodeValue("89300900000006");
        request.setReceivedQuantity(500.0);
        request.setConditionNote("Hàng còn nguyên vẹn");

        UUID shipmentId = UUID.randomUUID();
        WarehouseReceiptResponse response = WarehouseReceiptResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.WAREHOUSE_RECEIPT)
                .shipmentId(shipmentId)
                .shipmentName("Lô chè Tân Cương T8/2026")
                .declaredQuantity(500.0)
                .receivedQuantity(500.0)
                .discrepancy(0.0)
                .discrepancyPercent(0.0)
                .isDiscrepancyExceeded(false)
                .reasonRequired(false)
                .conditionNote("Hàng còn nguyên vẹn")
                .receiptDate(LocalDate.now())
                .recordedAt(LocalDateTime.now())
                .recordedBy("Nguyễn Văn B")
                .build();

        when(warehouseReceiptService.recordWarehouseReceipt(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/chain-events/warehouse-receipt")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("WAREHOUSE_RECEIPT"))
                .andExpect(jsonPath("$.data.discrepancy").value(0.0))
                .andExpect(jsonPath("$.data.isDiscrepancyExceeded").value(false));
    }

    // TC-02: Discrepancy exceeds threshold, with reason — success
    @Test
    @WithMockUser(roles = "VT-04")
    void recordWarehouseReceipt_shouldReturn201_whenDiscrepancyExceededWithReason() throws Exception {
        WarehouseReceiptRequest request = new WarehouseReceiptRequest();
        request.setCodeValue("89300900000006");
        request.setReceivedQuantity(400.0);
        request.setConditionNote("Hàng bị ẩm ướt");
        request.setReason("Hàng bị hư hỏng trong quá trình vận chuyển");

        UUID shipmentId = UUID.randomUUID();
        WarehouseReceiptResponse response = WarehouseReceiptResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.WAREHOUSE_RECEIPT)
                .shipmentId(shipmentId)
                .shipmentName("Lô chè Tân Cương T8/2026")
                .declaredQuantity(500.0)
                .receivedQuantity(400.0)
                .discrepancy(-100.0)
                .discrepancyPercent(-20.0)
                .isDiscrepancyExceeded(true)
                .reasonRequired(true)
                .reason("Hàng bị hư hỏng trong quá trình vận chuyển")
                .conditionNote("Hàng bị ẩm ướt")
                .receiptDate(LocalDate.now())
                .recordedAt(LocalDateTime.now())
                .recordedBy("Nguyễn Văn B")
                .notificationSent(true)
                .build();

        when(warehouseReceiptService.recordWarehouseReceipt(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/chain-events/warehouse-receipt")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.discrepancyPercent").value(-20.0))
                .andExpect(jsonPath("$.data.isDiscrepancyExceeded").value(true))
                .andExpect(jsonPath("$.data.reasonRequired").value(true))
                .andExpect(jsonPath("$.data.notificationSent").value(true));
    }

    // TC-02b: Discrepancy exceeds threshold, reason missing — reject
    @Test
    @WithMockUser(roles = "VT-04")
    void recordWarehouseReceipt_shouldReturn400_whenDiscrepancyExceededWithoutReason() throws Exception {
        WarehouseReceiptRequest request = new WarehouseReceiptRequest();
        request.setCodeValue("89300900000006");
        request.setReceivedQuantity(400.0);

        when(warehouseReceiptService.recordWarehouseReceipt(any(), any()))
                .thenThrow(new BusinessException("Chênh lệch số lượng vượt ngưỡng cho phép (2%). Vui lòng cung cấp lý do chênh lệch."));

        mockMvc.perform(post("/api/v1/chain-events/warehouse-receipt")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chênh lệch số lượng vượt ngưỡng cho phép (2%). Vui lòng cung cấp lý do chênh lệch."));
    }

    // TC-03: Invalid quantity (0) — should be rejected by validation
    @Test
    @WithMockUser(roles = "VT-04")
    void recordWarehouseReceipt_shouldReturn400_whenQuantityIsZero() throws Exception {
        WarehouseReceiptRequest request = new WarehouseReceiptRequest();
        request.setCodeValue("89300900000006");
        request.setReceivedQuantity(0.0);

        mockMvc.perform(post("/api/v1/chain-events/warehouse-receipt")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // TC-04: Unauthorized role (VT-02 instead of VT-04)
    @Test
    @WithMockUser(roles = "VT-02")
    void recordWarehouseReceipt_shouldReturn403_whenWrongRole() throws Exception {
        WarehouseReceiptRequest request = new WarehouseReceiptRequest();
        request.setCodeValue("89300900000006");
        request.setReceivedQuantity(500.0);

        mockMvc.perform(post("/api/v1/chain-events/warehouse-receipt")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // Lot not found
    @Test
    @WithMockUser(roles = "VT-04")
    void recordWarehouseReceipt_shouldReturn404_whenTraceCodeNotFound() throws Exception {
        WarehouseReceiptRequest request = new WarehouseReceiptRequest();
        request.setCodeValue("nonexistent");
        request.setReceivedQuantity(500.0);

        when(warehouseReceiptService.recordWarehouseReceipt(any(), any()))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Mã lô hàng không tồn tại."));

        mockMvc.perform(post("/api/v1/chain-events/warehouse-receipt")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Mã lô hàng không tồn tại."));
    }

    // Unauthenticated
    @Test
    void recordWarehouseReceipt_shouldReturn403_whenNotAuthenticated() throws Exception {
        WarehouseReceiptRequest request = new WarehouseReceiptRequest();
        request.setCodeValue("89300900000006");
        request.setReceivedQuantity(500.0);

        mockMvc.perform(post("/api/v1/chain-events/warehouse-receipt")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}