package vn.nguongocso.event.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordMobileEventRequest;
import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.OfflineEventSyncResultDto;
import vn.nguongocso.event.entity.OfflineSyncLog;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.OfflineSyncLogRepository;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
class OfflineSyncEventProcessorTest {

    @Mock
    private OfflineSyncLogRepository offlineSyncLogRepository;

    @Mock
    private ChainEventService chainEventService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private EventValidationService eventValidationService;

    @InjectMocks
    private OfflineSyncEventProcessor eventProcessor;

    private CustomUserDetails currentUser;
    private RecordOfflineEventDto offlineEventDto;
    private UUID syncId;
    private User actor;

    @BeforeEach
    void setUp() {
        syncId = UUID.randomUUID();
        currentUser = mock(CustomUserDetails.class);
        when(currentUser.getUserId()).thenReturn(UUID.randomUUID());

        actor = new User();
        actor.setUserId(currentUser.getUserId());
        actor.setFullName("Nguyễn Văn Ghi");

        offlineEventDto = new RecordOfflineEventDto();
        offlineEventDto.setOfflineEventId(UUID.randomUUID());
        offlineEventDto.setProductionLotId(UUID.randomUUID());
        offlineEventDto.setEventType(ChainEventType.HARVEST);
        offlineEventDto.setRecordedAt(LocalDateTime.now().minusHours(1));
        offlineEventDto.setLatitude(21.0285);
        offlineEventDto.setLongitude(105.8542);
        offlineEventDto.setImages(List.of("https://image.url/offline.jpg"));
        offlineEventDto.setEventData(new HashMap<>(Map.of("quantity", 1000.0, "harvestDate", "2026-08-01")));
    }

    @Test
    void processEvent_Duplicate_ReturnsDuplicateStatus() {
        // Given
        OfflineSyncLog existingLog = OfflineSyncLog.builder()
                .offlineEventId(offlineEventDto.getOfflineEventId())
                .status("SUCCESS")
                .build();
        when(offlineSyncLogRepository.findByOfflineEventId(offlineEventDto.getOfflineEventId()))
                .thenReturn(Optional.of(existingLog));

        // When
        OfflineEventSyncResultDto result = eventProcessor.processEvent(offlineEventDto, syncId, currentUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DUPLICATE");
        assertThat(result.getMessage()).contains("Sự kiện đã được đồng bộ trước đó");
        verifyNoInteractions(chainEventService);
    }

    @Test
    void processEvent_RecalledShipment_ThrowsBusinessException() {
        // Given
        offlineEventDto.setEventType(ChainEventType.TRANSPORT);
        offlineEventDto.getEventData().put("codeValue", "893001000001");
        offlineEventDto.getEventData().put("fromLocation", "Kho A");
        offlineEventDto.getEventData().put("toLocation", "Kho B");
        Shipment recalledShipment = new Shipment();
        recalledShipment.setId(offlineEventDto.getProductionLotId());
        recalledShipment.setName("Lô hàng xuất khẩu 01");
        recalledShipment.setStatus(ShipmentStatus.RECALLED);

        when(offlineSyncLogRepository.findByOfflineEventId(offlineEventDto.getOfflineEventId()))
                .thenReturn(Optional.empty());
        doThrow(new BusinessException("Lô hàng đã bị thu hồi, không thể ghi nhận sự kiện."))
                .when(chainEventService)
                .recordTransportEvent(any(), eq(currentUser));

        // When
        OfflineEventSyncResultDto result = eventProcessor.processEvent(offlineEventDto, syncId, currentUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).contains("Lô hàng đã bị thu hồi, không thể ghi nhận sự kiện.");

        verify(eventValidationService).logFailedAttempt(any(), any(), any(), any(), any());
        verify(chainEventService).recordTransportEvent(any(), eq(currentUser));
    }

    @Test
    void processEvent_Success_SavesLogAndReturnsSuccess() {
        // Given
        when(offlineSyncLogRepository.findByOfflineEventId(offlineEventDto.getOfflineEventId()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(currentUser.getUserId()))
                .thenReturn(Optional.of(actor));

        ChainEventResponse mockEventResponse = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .build();
        lenient().when(chainEventService.recordMobileEvent(any(RecordMobileEventRequest.class), eq(currentUser)))
                .thenReturn(mockEventResponse);

        // When
        OfflineEventSyncResultDto result = eventProcessor.processEvent(offlineEventDto, syncId, currentUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getOfflineEventId()).isEqualTo(offlineEventDto.getOfflineEventId()); // ✅ sửa

        verify(offlineSyncLogRepository).save(any(OfflineSyncLog.class));
    }
}
