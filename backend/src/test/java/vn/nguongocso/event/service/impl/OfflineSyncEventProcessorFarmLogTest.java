package vn.nguongocso.event.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
import vn.nguongocso.event.dto.request.RecordOfflineEventDto;
import vn.nguongocso.event.dto.response.OfflineEventSyncResultDto;
import vn.nguongocso.event.entity.OfflineSyncLog;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.OfflineSyncLogRepository;
import vn.nguongocso.event.service.ChainEventService;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.FarmLogService;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Kiểm thử đồng bộ nhật ký canh tác ngoại tuyến (NCL-10-CN-012).
 */
@ExtendWith(MockitoExtension.class)
class OfflineSyncEventProcessorFarmLogTest {

    @Mock
    private OfflineSyncLogRepository offlineSyncLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChainEventService chainEventService;

    @Mock
    private EventValidationService eventValidationService;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private FarmLogService farmLogService;

    @Mock
    private PermissionChecker permissionChecker;

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
        actor.setFullName("Người ghi sự kiện");

        offlineEventDto = new RecordOfflineEventDto();
        offlineEventDto.setOfflineEventId(UUID.randomUUID());
        offlineEventDto.setProductionLotId(UUID.randomUUID());
        offlineEventDto.setEventType(ChainEventType.FARM_LOG);
        offlineEventDto.setRecordedAt(LocalDateTime.now().minusHours(1));
        offlineEventDto.setDeviceSource("WEB");
        offlineEventDto.setImages(null);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("activityType", "FERTILIZING");
        eventData.put("material", "NPK 16-16-8");
        eventData.put("quantity", 25.0);
        eventData.put("unit", "kg");
        eventData.put("executedDate", LocalDate.now().minusDays(1).toString());
        eventData.put("notes", "Bón phân lần 1, ghi khi ngoại tuyến");
        offlineEventDto.setEventData(eventData);
    }

    @Test
    void processEvent_FarmLogSuccess_DelegatesToFarmLogService() {
        // Given
        when(offlineSyncLogRepository.findByOfflineEventId(offlineEventDto.getOfflineEventId()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(currentUser.getUserId()))
                .thenReturn(Optional.of(actor));
        UUID createdId = UUID.randomUUID();
        when(farmLogService.create(any())).thenReturn(FarmLogResponse.builder().id(createdId).build());

        // When
        OfflineEventSyncResultDto result = eventProcessor.processEvent(offlineEventDto, syncId, currentUser);

        // Then
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getEventId()).isEqualTo(createdId);
        verify(permissionChecker).check("FARM_LOG", "CREATE");
        verify(farmLogService).create(any());
        verify(offlineSyncLogRepository).save(any(OfflineSyncLog.class));
        verifyNoInteractions(chainEventService);
    }

    @Test
    void processEvent_FarmLogDuplicate_SkipsCreation() {
        // Given: gửi cùng offlineEventId lần 2 khi lần 1 đã SUCCESS
        OfflineSyncLog existingLog = OfflineSyncLog.builder()
                .offlineEventId(offlineEventDto.getOfflineEventId())
                .status("SUCCESS")
                .build();
        when(offlineSyncLogRepository.findByOfflineEventId(offlineEventDto.getOfflineEventId()))
                .thenReturn(Optional.of(existingLog));

        // When
        OfflineEventSyncResultDto result = eventProcessor.processEvent(offlineEventDto, syncId, currentUser);

        // Then
        assertThat(result.getStatus()).isEqualTo("DUPLICATE");
        verifyNoInteractions(farmLogService);
    }

    @Test
    void processEvent_FarmLogMissingActivityType_ReturnsFailed() {
        // Given
        offlineEventDto.getEventData().remove("activityType");
        when(offlineSyncLogRepository.findByOfflineEventId(offlineEventDto.getOfflineEventId()))
                .thenReturn(Optional.empty());

        // When
        OfflineEventSyncResultDto result = eventProcessor.processEvent(offlineEventDto, syncId, currentUser);

        // Then
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).contains("Vui lòng chọn loại hoạt động");
        verifyNoInteractions(farmLogService);
        verify(eventValidationService).logFailedAttempt(any(), any(), any(), any(), any());
    }

    @Test
    void processEvent_FarmLogBusinessError_LogsFailed() {
        // Given: lô đã hủy / mất quyền — FarmLogService ném lỗi nghiệp vụ
        when(offlineSyncLogRepository.findByOfflineEventId(offlineEventDto.getOfflineEventId()))
                .thenReturn(Optional.empty());
        doThrow(new BusinessException("Lô sản xuất đã bị hủy, không thể thao tác nhật ký canh tác."))
                .when(farmLogService).create(any());

        // When
        OfflineEventSyncResultDto result = eventProcessor.processEvent(offlineEventDto, syncId, currentUser);

        // Then
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).contains("đã bị hủy");
        verify(eventValidationService).logFailedAttempt(any(), any(), any(), any(), any());
    }

    @Test
    void processEvent_FarmLogMissingCreatePermission_ReturnsFailed() {
        // Given: ma trận quyền tổ chức tắt FARM_LOG/CREATE — parity với ghi trực tuyến (QTN-07)
        when(offlineSyncLogRepository.findByOfflineEventId(offlineEventDto.getOfflineEventId()))
                .thenReturn(Optional.empty());
        doThrow(new BusinessException("Bạn không có quyền thực hiện chức năng này."))
                .when(permissionChecker).check("FARM_LOG", "CREATE");

        // When
        OfflineEventSyncResultDto result = eventProcessor.processEvent(offlineEventDto, syncId, currentUser);

        // Then
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).contains("Bạn không có quyền thực hiện chức năng này.");
        verifyNoInteractions(farmLogService);
        verify(eventValidationService).logFailedAttempt(any(), any(), any(), any(), any());
    }
}
