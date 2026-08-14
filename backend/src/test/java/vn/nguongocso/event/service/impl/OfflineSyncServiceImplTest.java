package vn.nguongocso.event.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.OfflineEventSyncRequest;
import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.event.dto.response.OfflineEventSyncResponse;
import vn.nguongocso.event.dto.response.OfflineEventSyncResultDto;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.OfflineSyncLogRepository;
import vn.nguongocso.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class OfflineSyncServiceImplTest {

    @Mock
    private OfflineSyncEventProcessor offlineSyncEventProcessor;

    @Mock
    private OfflineSyncLogRepository offlineSyncLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OfflineSyncServiceImpl offlineSyncService;

    private CustomUserDetails currentUser;
    private OfflineEventSyncRequest syncRequest;
    private UUID syncId;

    @BeforeEach
    void setUp() {
        syncId = UUID.randomUUID();
        currentUser = mock(CustomUserDetails.class);
        // LOẠI BỎ: when(currentUser.getUserId()).thenReturn(UUID.randomUUID()); từ đây

        RecordOfflineEventDto event1 = new RecordOfflineEventDto();
        event1.setOfflineEventId(UUID.randomUUID());
        event1.setEventType(ChainEventType.HARVEST);

        RecordOfflineEventDto event2 = new RecordOfflineEventDto();
        event2.setOfflineEventId(UUID.randomUUID());
        event2.setEventType(ChainEventType.PACKAGING);

        syncRequest = new OfflineEventSyncRequest();
        syncRequest.setSyncId(syncId);
        syncRequest.setEvents(List.of(event1, event2));
    }

    @Test
    void syncOfflineEvents_AllSuccess_ReturnsCorrectStats() {
        // Given
        OfflineEventSyncResultDto res1 = OfflineEventSyncResultDto.builder()
                .offlineEventId(syncRequest.getEvents().get(0).getOfflineEventId())
                .status("SUCCESS")
                .build();
        OfflineEventSyncResultDto res2 = OfflineEventSyncResultDto.builder()
                .offlineEventId(syncRequest.getEvents().get(1).getOfflineEventId())
                .status("SUCCESS")
                .build();

        when(offlineSyncEventProcessor.processEvent(eq(syncRequest.getEvents().get(0)), eq(syncId), eq(currentUser)))
                .thenReturn(res1);
        when(offlineSyncEventProcessor.processEvent(eq(syncRequest.getEvents().get(1)), eq(syncId), eq(currentUser)))
                .thenReturn(res2);

        // When
        OfflineEventSyncResponse response = offlineSyncService.syncOfflineEvents(syncRequest, currentUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSyncId()).isEqualTo(syncId);
        assertThat(response.getTotalEvents()).isEqualTo(2);
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getDuplicateCount()).isEqualTo(0);
        assertThat(response.getFailedCount()).isEqualTo(0);
    }

    @Test
    void syncOfflineEvents_PartialSuccessAndFail_AppliesPartialCommit() {
        // Given
        RecordOfflineEventDto eventSuccess = syncRequest.getEvents().get(0);
        RecordOfflineEventDto eventFailed = syncRequest.getEvents().get(1);
        UUID mockUserId = UUID.randomUUID();

        // Định nghĩa mock cho currentUser.getUserId() riêng trong test case này
        lenient().when(currentUser.getUserId()).thenReturn(mockUserId);

        OfflineEventSyncResultDto resSuccess = OfflineEventSyncResultDto.builder()
                .offlineEventId(eventSuccess.getOfflineEventId())
                .status("SUCCESS")
                .build();

        // Mô phỏng: Sự kiện 1 thành công
        lenient().when(offlineSyncEventProcessor.processEvent(eq(eventSuccess), eq(syncId), eq(currentUser)))
                .thenReturn(resSuccess);
        // Mô phỏng: Sự kiện 2 ném ra lỗi (lô bị thu hồi)
        lenient().when(offlineSyncEventProcessor.processEvent(eq(eventFailed), eq(syncId), eq(currentUser)))
                .thenReturn(OfflineEventSyncResultDto.builder()
                        .offlineEventId(eventFailed.getOfflineEventId())
                        .status("FAILED")
                        .message("Lô hàng đã bị thu hồi, không thể ghi nhận sự kiện.")
                        .build());

        lenient().when(userRepository.findById(mockUserId))
                .thenReturn(Optional.of(new User()));

        // When
        OfflineEventSyncResponse response = offlineSyncService.syncOfflineEvents(syncRequest, currentUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalEvents()).isEqualTo(2);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailedCount()).isEqualTo(1);

        OfflineEventSyncResultDto failResult = response.getResults().stream()
                .filter(r -> r.getOfflineEventId().equals(eventFailed.getOfflineEventId()))
                .findFirst().orElseThrow();
        assertThat(failResult.getStatus()).isEqualTo("FAILED");
        assertThat(failResult.getMessage()).contains("Lô hàng đã bị thu hồi");
    }
}
