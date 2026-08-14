package vn.nguongocso.trace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.trace.dto.request.CreateShipmentRequest;
import vn.nguongocso.trace.dto.response.ShipmentResponse;
import vn.nguongocso.trace.dto.response.TraceCodeResponse;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.service.ShipmentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@WebMvcTest(ShipmentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@WithMockUser(roles = "USER") // Áp dụng cho tất cả các test, đảm bảo SecurityContext có user
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShipmentService shipmentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    private final UUID productionLotId = UUID.randomUUID();
    private final UUID shipmentId = UUID.randomUUID();

    // ==================== TEST THÀNH CÔNG ====================

    @Test
    void createShipment_ShouldReturnCreated_WhenRequestValid() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLotId);
        request.setName("Lô hàng cà chua số 1");
        request.setTotalQuantity(50L);
        request.setPackagingInfo("Đóng thùng 10kg");

        ShipmentResponse response = buildSuccessResponse();

        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(shipmentId.toString()))
                .andExpect(jsonPath("$.data.name").value("Lô hàng cà chua số 1"))
                .andExpect(jsonPath("$.data.totalQuantity").value(50))
                .andExpect(jsonPath("$.data.packagingInfo").value("Đóng thùng 10kg"))
                .andExpect(jsonPath("$.data.status").value("CODE_PRINTED"))
                .andExpect(jsonPath("$.data.traceCodes.length()").value(2));
    }

    // ==================== TEST VALIDATION LỖI ====================

    @Test
    void createShipment_ShouldReturnBadRequest_WhenProductionLotIdMissing() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setName("Lô hàng cà chua số 1");
        request.setTotalQuantity(50L);

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createShipment_ShouldReturnBadRequest_WhenNameMissing() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLotId);
        request.setTotalQuantity(50L);

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createShipment_ShouldReturnBadRequest_WhenTotalQuantityZeroOrNegative() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLotId);
        request.setName("Lô hàng cà chua số 1");
        request.setTotalQuantity(0L);

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== TEST SERVICE NÉM BUSINESS EXCEPTION ====================

    @Test
    void createShipment_ShouldReturnBadRequest_WhenProductionLotNotFound() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLotId);
        request.setName("Lô hàng cà chua số 1");
        request.setTotalQuantity(50L);
        request.setPackagingInfo("Đóng thùng 10kg");

        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
                .thenThrow(new BusinessException("Không tìm thấy lô sản xuất."));

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Không tìm thấy lô sản xuất."));
    }

    @Test
    void createShipment_ShouldReturnBadRequest_WhenCodeRangeLimitExceeded() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setProductionLotId(productionLotId);
        request.setName("Lô hàng cà chua số 1");
        request.setTotalQuantity(50L);
        request.setPackagingInfo("Đóng thùng 10kg");

        when(shipmentService.createShipment(any(CreateShipmentRequest.class)))
                .thenThrow(new BusinessException("Số lượng tem vượt quá hạn mức dải mã còn lại."));

        mockMvc.perform(post("/api/v1/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Số lượng tem vượt quá hạn mức dải mã còn lại."));
    }

    // ==================== TEST KÍCH HOẠT TEM ====================

    @Test
    void activateStamps_shouldReturnOk_whenValidId() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        ShipmentResponse response = ShipmentResponse.builder()
                .id(shipmentId)
                .name("Lô hàng đã kích hoạt")
                .status(ShipmentStatus.ACTIVATED)
                .build();

        when(shipmentService.activateShipmentStamps(shipmentId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/shipments/{id}/activate", shipmentId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVATED"))
                .andExpect(jsonPath("$.data.id").value(shipmentId.toString()));
    }

    @Test
    void activateStamps_shouldReturnBadRequest_whenBusinessException() throws Exception {
        UUID shipmentId = UUID.randomUUID();
        when(shipmentService.activateShipmentStamps(shipmentId))
                .thenThrow(new BusinessException("Tem đã được kích hoạt trước đó."));

        mockMvc.perform(post("/api/v1/shipments/{id}/activate", shipmentId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tem đã được kích hoạt trước đó."));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void getShipmentById_ShouldReturnOk_WhenShipmentExists() throws Exception {
        ShipmentResponse response = buildSuccessResponse();
        when(shipmentService.getShipmentById(shipmentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shipments/{id}", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(shipmentId.toString()))
                .andExpect(jsonPath("$.data.name").value("Lô hàng cà chua số 1"));
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void getShipmentById_ShouldReturnNotFound_WhenShipmentDoesNotExist() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(shipmentService.getShipmentById(randomId))
                .thenThrow(new BusinessException("Không tìm thấy lô hàng với ID: " + randomId));

        mockMvc.perform(get("/api/v1/shipments/{id}", randomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy lô hàng với ID: " + randomId));
    }

    // ==================== HELPER ====================

    private ShipmentResponse buildSuccessResponse() {
        TraceCodeResponse traceCode1 = TraceCodeResponse.builder()
                .id(UUID.randomUUID())
                .codeValue("NCL00000001")
                .status(TraceCodeStatus.INACTIVE)
                .build();

        TraceCodeResponse traceCode2 = TraceCodeResponse.builder()
                .id(UUID.randomUUID())
                .codeValue("NCL00000002")
                .status(TraceCodeStatus.INACTIVE)
                .build();

        return ShipmentResponse.builder()
                .id(shipmentId)
                .productionLotId(productionLotId)
                .productionLotName("Lô cà chua vụ Đông")
                .name("Lô hàng cà chua số 1")
                .totalQuantity(50L)
                .packagingInfo("Đóng thùng 10kg")
                .status(ShipmentStatus.CODE_PRINTED)
                .traceCodes(List.of(traceCode1, traceCode2))
                .createdByName("Nguyễn Văn A")
                .createdAt(LocalDateTime.now())
                .build();
    }
}