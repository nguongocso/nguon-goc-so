package vn.nguongocso.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.event.dto.request.OfflineEventSyncRequest;
import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.event.dto.response.OfflineEventSyncResponse;
import vn.nguongocso.event.dto.response.OfflineEventSyncResultDto;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.OfflineSyncService;
import vn.nguongocso.permission.service.PermissionChecker;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebMvcTest(ChainEventController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class OfflineSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock các service được inject vào ChainEventController
    @MockitoBean
    private OfflineSyncService offlineSyncService;

    @MockitoBean
    private ChainEventService chainEventService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private PermissionChecker permissionChecker;

    private OfflineEventSyncRequest validRequest;
    private OfflineEventSyncResponse successResponse;
    private UUID syncId;

    @BeforeEach
    void setUp() {
        syncId = UUID.randomUUID();

        // 1. Chuẩn bị sự kiện hợp lệ trong Request
        RecordOfflineEventDto eventDto = new RecordOfflineEventDto();
        eventDto.setOfflineEventId(UUID.randomUUID());
        eventDto.setProductionLotId(UUID.randomUUID());
        eventDto.setEventType(ChainEventType.HARVEST);
        eventDto.setRecordedAt(LocalDateTime.now().minusMinutes(30));
        eventDto.setLatitude(20.9854);
        eventDto.setLongitude(105.7985);
        eventDto.setImages(List.of("https://image.url/pic.jpg"));
        eventDto.setEventData(new HashMap<>(Map.of("quantity", 500.0, "harvestDate", "2026-08-01")));

        validRequest = new OfflineEventSyncRequest();
        validRequest.setSyncId(syncId);
        validRequest.setEvents(List.of(eventDto));

        // 2. Chuẩn bị Response thành công từ Service
        OfflineEventSyncResultDto resultDto = OfflineEventSyncResultDto.builder()
                .offlineEventId(eventDto.getOfflineEventId())
                .status("SUCCESS")
                .eventId(UUID.randomUUID())
                .message("Đồng bộ sự kiện thành công.")
                .build();

        successResponse = OfflineEventSyncResponse.builder()
                .syncId(syncId)
                .totalEvents(1)
                .successCount(1)
                .duplicateCount(0)
                .failedCount(0)
                .results(List.of(resultDto))
                .build();
    }

    @Test
    @WithMockUser(roles = "VT-03") // Đóng vai Người ghi sự kiện
    void syncOfflineEvents_AuthorizedUser_Returns200AndStats() throws Exception {
        // Given
        when(offlineSyncService.syncOfflineEvents(any(OfflineEventSyncRequest.class), any()))
                .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/sync")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.syncId").value(syncId.toString()))
                .andExpect(jsonPath("$.data.totalEvents").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.results[0].status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(roles = "GUEST") // Vai trò không hợp lệ
    void syncOfflineEvents_UnauthorizedRole_Returns403Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/sync")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VT-03")
    void syncOfflineEvents_InvalidPayload_Returns400BadRequest() throws Exception {
        // Given: Request thiếu trường syncId bắt buộc
        OfflineEventSyncRequest invalidRequest = new OfflineEventSyncRequest();
        invalidRequest.setEvents(List.of()); // danh sách rỗng

        // When & Then
        mockMvc.perform(post("/api/v1/chain-events/sync")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
