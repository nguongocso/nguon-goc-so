package vn.nguongocso.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordMobileEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class MobileChainEventServiceImplTest {

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventValidationService eventValidationService;

        @Mock
        private ApplicationEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private vn.nguongocso.farm.service.HarvestEligibilityService harvestEligibilityService;

    @InjectMocks
    private ChainEventServiceImpl chainEventService;

    private CustomUserDetails validUser;
    private ProductionLot productionLot;
    private Organization organization;
    private User actor;
    private UUID userId;

    @BeforeEach
    void setUp() {
        validUser = mock(CustomUserDetails.class);
        userId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        organization.setName("Hợp tác xã nông sản VietGAP");

        productionLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô xoài cát chu xuất khẩu")
                .organization(organization)
                .status(ProductionLotStatus.APPROVED)
                .build();

        actor = new User();
        actor.setUserId(userId);
        actor.setFullName("Lê Văn Đồng");
    }

    @Test
    void recordMobileEvent_Harvest_Success() throws JsonProcessingException {
        // Given
        RecordMobileEventRequest request = new RecordMobileEventRequest();
        request.setProductionLotId(productionLot.getId());
        request.setEventType(ChainEventType.HARVEST);
        request.setRecordedAt(LocalDateTime.now().minusMinutes(5));
        request.setLatitude(20.9854);
        request.setLongitude(105.7985);
        request.setImages(List.of("https://image.url/harvest1.jpg"));
        request.setDeviceSource("MOBILE");

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("quantity", 2500.0);
        eventData.put("harvestDate", "2026-07-31");
        request.setEventData(eventData);

        when(validUser.getRoleCode()).thenReturn("VT-03"); // EVENT_RECORDER
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));
        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                vn.nguongocso.farm.dto.response.HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(null)
                        .build());

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + productionLot.getId() + "\",\"deviceSource\":\"MOBILE\"}")
                .recordedAt(request.getRecordedAt())
                .recordedBy(actor)
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        // When
        ChainEventResponse response = chainEventService.recordMobileEvent(request, validUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(ChainEventType.HARVEST);
        assertThat(response.getEventData()).containsEntry("deviceSource", "MOBILE");
        assertThat(response.getEventData()).containsKey("images");
        assertThat(productionLot.getStatus()).isEqualTo(ProductionLotStatus.HARVESTED);
        assertThat(productionLot.getActualQuantity()).isEqualTo(2500.0);

        verify(productionLotRepository, times(1)).save(productionLot);
        verify(chainEventRepository, times(1)).save(any(ChainEvent.class));
        verifyNoInteractions(eventValidationService); // Thành công không cần ghi log thất bại
    }

    @Test
    void recordMobileEvent_ThrowException_WhenDifferentOrganization_AndLogFailed() {
        // Given
        RecordMobileEventRequest request = new RecordMobileEventRequest();
        request.setProductionLotId(productionLot.getId());
        request.setEventType(ChainEventType.HARVEST);
        request.setRecordedAt(LocalDateTime.now());
        request.setLatitude(20.9854);
        request.setLongitude(105.7985);
        request.setImages(List.of("https://image.url/harvest1.jpg"));
        request.setEventData(Map.of("quantity", 2000.0, "harvestDate", "2026-07-31"));

        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(UUID.randomUUID()); // Tổ chức khác

        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordMobileEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không thuộc tổ chức quản lý của lô sản xuất này.");

        // Organization validation now rejects directly without creating a failed-attempt log.
        verifyNoInteractions(eventValidationService);
        verifyNoMoreInteractions(chainEventRepository);
    }

    @Test
    void recordMobileEvent_ThrowException_WhenHarvestLotNotApproved() {
        // Given
        RecordMobileEventRequest request = new RecordMobileEventRequest();
        request.setProductionLotId(productionLot.getId());
        request.setEventType(ChainEventType.HARVEST);
        request.setRecordedAt(LocalDateTime.now());
        request.setLatitude(20.9854);
        request.setLongitude(105.7985);
        request.setImages(List.of("https://image.url/harvest1.jpg"));
        request.setEventData(Map.of("quantity", 1000.0, "harvestDate", "2026-07-31"));

        productionLot.setStatus(ProductionLotStatus.DRAFT); // Chưa được duyệt

        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordMobileEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.");

        verify(eventValidationService, times(1)).logFailedAttempt(
                any(UUID.class), anyString(), any(ChainEventType.class), anyString(), any(CustomUserDetails.class)
        );
    }
}
