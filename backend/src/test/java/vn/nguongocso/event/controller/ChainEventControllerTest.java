package vn.nguongocso.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import vn.nguongocso.event.dto.request.RecordMobileEventRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.event.dto.request.CorrectPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.OfflineSyncService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebMvcTest(ChainEventController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ChainEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChainEventService chainEventService;

        @MockitoBean
        private OfflineSyncService offlineSyncService;

        @MockitoBean
        private PermissionChecker permissionChecker;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private RecordHarvestEventRequest validRequest;
    private RecordMobileEventRequest validRequestMobile;
    private ChainEventResponse successResponse;

    // Thay thế @MockBean bằng @MockitoBean theo chuẩn Spring Boot 3.4.0+

    @BeforeEach
    void setUp() {
        UUID productionLotId = UUID.randomUUID();

        // 1. Tạo request hợp lệ
        validRequest = new RecordHarvestEventRequest();
        validRequest.setProductionLotId(productionLotId);
        validRequest.setHarvestDate(LocalDate.of(2026, 7, 24));
        validRequest.setQuantity(1500.0);
        validRequest.setLatitude(21.0285);
        validRequest.setLongitude(105.8542);
        validRequestMobile = new RecordMobileEventRequest();
        validRequestMobile.setProductionLotId(UUID.randomUUID());
        validRequestMobile.setEventType(ChainEventType.HARVEST);
        validRequestMobile.setRecordedAt(LocalDateTime.now().minusMinutes(10));
        validRequestMobile.setLatitude(21.0285);
        validRequestMobile.setLongitude(105.8542);
        validRequestMobile.setImages(List.of("https://picsum.photos/id/237/400/300.jpg"));
        validRequestMobile.setEventData(Map.of("quantity", 500.0, "harvestDate", "2026-07-31"));

        // 2. Thiết lập dữ liệu eventData
        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", productionLotId.toString());
        eventDataMap.put("harvestDate", "2026-07-24");
        eventDataMap.put("quantity", 1500.0);

        // 3. Thiết lập response thành công
        successResponse = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData(eventDataMap)
                .latitude(21.0285)
                .longitude(105.8542)
                .recordedAt(LocalDateTime.now())
                .recordedByName("Nguyễn Văn Ghi")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Case 1: Ghi nhận sự kiện thành công khi người dùng có quyền hợp lệ (VT-03 - EVENT_RECORDER)
     */
    @Test
    @WithMockUser(roles = "VT-03") // Map tới ROLE_VT-03
    void recordHarvest_Success_WithAuthorizedUser() throws Exception {
        // Given
        when(chainEventService.recordHarvestEvent(any(RecordHarvestEventRequest.class), any()))
                .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/harvest")
                        .with(csrf()) // Hỗ trợ bypass CSRF check khi chạy test Spring Security
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("HARVEST"))
                .andExpect(jsonPath("$.data.eventData.quantity").value(1500.0))
                .andExpect(jsonPath("$.data.recordedByName").value("Nguyễn Văn Ghi"));
    }

    /**
     * Case 2: Trả về 403 Forbidden khi người dùng không có vai trò phù hợp (ví dụ VT-06 - CONSUMER)
     */
    @Test
    @WithMockUser(roles = "VT-06") // Map tới ROLE_VT-06 (Khách hàng)
    void recordHarvest_ThrowForbidden_WithUnauthorizedUser() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/harvest")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Case 3: Trả về 400 Bad Request khi dữ liệu request đầu vào không hợp lệ (ví dụ thiếu ngày thu hoạch)
     */
    @Test
    @WithMockUser(roles = "VT-02") // Map tới ROLE_VT-02 (Quản lý HTX)
    void recordHarvest_ThrowBadRequest_WhenValidationFails() throws Exception {
        // Given
        RecordHarvestEventRequest invalidRequest = new RecordHarvestEventRequest();
        invalidRequest.setProductionLotId(UUID.randomUUID());
        invalidRequest.setQuantity(-10.0); // Invalid: Sản lượng phải lớn hơn 0 và thiếu ngày thu hoạch

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/harvest")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"));
    }

    /**
     * Case 4: Trả về lỗi nghiệp vụ khi Service ném ra BusinessException (ví dụ lô chưa duyệt)
     */
    @Test
    @WithMockUser(roles = "VT-03")
    void recordHarvest_ThrowBusinessException_WhenServiceFails() throws Exception {
        // Given
        when(chainEventService.recordHarvestEvent(any(RecordHarvestEventRequest.class), any()))
                .thenThrow(new BusinessException("Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch."));

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/harvest")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                // Tùy thuộc vào thiết kế GlobalExceptionHandler của dự án để bắt mã HTTP, thông thường ném BusinessException sẽ trả về 400 hoặc 409
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch."));
    }
    // ==========================================
    // UNIT TESTS CHO GHI SỰ KIỆN ĐÓNG GÓI (PACKAGING)
    // ==========================================

    /**
     * Case 5: Ghi nhận sự kiện đóng gói thành công khi người dùng có quyền (VT-03)
     */
    @Test
    @WithMockUser(roles = "VT-03")
    void recordPackaging_Success_WithAuthorizedUser() throws Exception {
        // Given
        UUID productionLotId = UUID.randomUUID();
        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(productionLotId);
        packagingRequest.setPackagingSpecification("Túi hút chân không 500g");
        packagingRequest.setPackagingDate(LocalDate.of(2026, 7, 25));
        packagingRequest.setLatitude(21.0285);
        packagingRequest.setLongitude(105.8542);

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", productionLotId.toString());
        eventDataMap.put("packagingSpecification", "Túi hút chân không 500g");
        eventDataMap.put("packagingDate", "2026-07-25");

        ChainEventResponse packagingResponse = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PACKAGING)
                .eventData(eventDataMap)
                .latitude(21.0285)
                .longitude(105.8542)
                .recordedAt(LocalDateTime.now())
                .recordedByName("Nguyễn Văn Ghi")
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventService.recordPackagingEvent(any(RecordPackagingEventRequest.class), any()))
                .thenReturn(packagingResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/packaging")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packagingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("PACKAGING"))
                .andExpect(jsonPath("$.data.eventData.packagingSpecification").value("Túi hút chân không 500g"))
                .andExpect(jsonPath("$.data.recordedByName").value("Nguyễn Văn Ghi"));
    }

    /**
     * Case 6: Trả về 403 Forbidden khi ghi nhận đóng gói với vai trò không được cấp quyền (VT-06)
     */
    @Test
    @WithMockUser(roles = "VT-06")
    void recordPackaging_ThrowForbidden_WithUnauthorizedUser() throws Exception {
        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(UUID.randomUUID());
        packagingRequest.setPackagingSpecification("Túi hút chân không 500g");
        packagingRequest.setPackagingDate(LocalDate.of(2026, 7, 25));

        mockMvc.perform(post("/api/v1/chain-events/packaging")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packagingRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Case 7: Trả về 400 Bad Request khi thiếu trường bắt buộc (thiếu quy cách đóng gói)
     */
    @Test
    @WithMockUser(roles = "VT-03")
    void recordPackaging_ThrowBadRequest_WhenValidationFails() throws Exception {
        RecordPackagingEventRequest invalidRequest = new RecordPackagingEventRequest();
        invalidRequest.setProductionLotId(UUID.randomUUID());
        invalidRequest.setPackagingSpecification(""); // Invalid: Bị trống
        invalidRequest.setPackagingDate(LocalDate.of(2026, 7, 25));

        mockMvc.perform(post("/api/v1/chain-events/packaging")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }


    // ==========================================
    // UNIT TESTS CHO ĐÍNH CHÍNH SỰ KIỆN ĐÓNG GÓI (CORRECT)
    // ==========================================

    /**
     * Case 8: Đính chính sự kiện đóng gói thành công khi người dùng có quyền (VT-02)
     */
    @Test
    @WithMockUser(roles = "VT-02")
    void correctPackaging_Success_WithAuthorizedUser() throws Exception {
        // Given
        UUID originalEventId = UUID.randomUUID();
        CorrectPackagingEventRequest correctRequest = new CorrectPackagingEventRequest();
        correctRequest.setPackagingSpecification("Túi hút chân không 1kg");
        correctRequest.setPackagingDate(LocalDate.of(2026, 7, 25));
        correctRequest.setCorrectionReason("Đính chính do nhập sai quy cách từ 500g sang 1kg");

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", UUID.randomUUID().toString());
        eventDataMap.put("packagingSpecification", "Túi hút chân không 1kg");
        eventDataMap.put("packagingDate", "2026-07-25");
        eventDataMap.put("correctionReason", "Đính chính do nhập sai quy cách từ 500g sang 1kg");
        eventDataMap.put("parentEventId", originalEventId.toString());

        ChainEventResponse correctResponse = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PACKAGING)
                .eventData(eventDataMap)
                .recordedAt(LocalDateTime.now())
                .recordedByName("Quản Lý HTX")
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventService.correctPackagingEvent(any(UUID.class), any(CorrectPackagingEventRequest.class), any()))
                .thenReturn(correctResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/packaging/{id}/correct", originalEventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventData.packagingSpecification").value("Túi hút chân không 1kg"))
                .andExpect(jsonPath("$.data.eventData.correctionReason").value("Đính chính do nhập sai quy cách từ 500g sang 1kg"))
                .andExpect(jsonPath("$.data.eventData.parentEventId").value(originalEventId.toString()));
    }

    /**
     * Case 9: Trả về 403 Forbidden khi đính chính sự kiện với vai trò không phù hợp (VT-06)
     */
    @Test
    @WithMockUser(roles = "VT-06")
    void correctPackaging_ThrowForbidden_WithUnauthorizedUser() throws Exception {
        UUID originalEventId = UUID.randomUUID();
        CorrectPackagingEventRequest correctRequest = new CorrectPackagingEventRequest();
        correctRequest.setPackagingSpecification("Túi hút chân không 1kg");
        correctRequest.setPackagingDate(LocalDate.of(2026, 7, 25));
        correctRequest.setCorrectionReason("Sửa sai");

        mockMvc.perform(post("/api/v1/chain-events/packaging/{id}/correct", originalEventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Case 10: Trả về 400 Bad Request khi đính chính thiếu lý do đính chính (correctionReason)
     */
    @Test
    @WithMockUser(roles = "VT-03")
    void correctPackaging_ThrowBadRequest_WhenValidationFails() throws Exception {
        UUID originalEventId = UUID.randomUUID();
        CorrectPackagingEventRequest invalidRequest = new CorrectPackagingEventRequest();
        invalidRequest.setPackagingSpecification("Túi hút chân không 1kg");
        invalidRequest.setPackagingDate(LocalDate.of(2026, 7, 25));
        invalidRequest.setCorrectionReason(""); // Invalid: Lý do bị rỗng

        mockMvc.perform(post("/api/v1/chain-events/packaging/{id}/correct", originalEventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
    @Test
    @WithMockUser(roles = {"VT-03"}) // EVENT_RECORDER
    void recordMobileEvent_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        // Given
        ChainEventResponse mockResponse = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .latitude(21.0285)
                .longitude(105.8542)
                .recordedAt(validRequestMobile.getRecordedAt())
                .recordedByName("Lê Văn Đồng")
                .createdAt(LocalDateTime.now())
                .build();

        // SỬA: Thay thế any(CustomUserDetails.class) bằng any() để bắt được cả giá trị null/mock principal
        when(chainEventService.recordMobileEvent(any(RecordMobileEventRequest.class), any()))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestMobile))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("HARVEST"))
                .andExpect(jsonPath("$.data.recordedByName").value("Lê Văn Đồng"));
    }

    @Test
    @WithMockUser(roles = {"VT-03"})
    void recordMobileEvent_ShouldReturnBadRequest_WhenMissingImages() throws Exception {
        // Given
        validRequestMobile.setImages(List.of()); // Gửi danh sách rỗng (vi phạm validation)

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestMobile))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                // SỬA: Kiểm tra thông báo lỗi chung của dự án và chi tiết lỗi trong trường images
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.errors.images").value("Sự kiện ghi nhận ngoài đồng yêu cầu tối thiểu một hình ảnh thực địa"));
    }


}
