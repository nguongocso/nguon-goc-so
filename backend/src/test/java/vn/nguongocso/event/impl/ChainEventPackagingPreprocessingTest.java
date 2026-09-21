package vn.nguongocso.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.event.dto.request.CorrectPackagingEventRequest;
import vn.nguongocso.event.dto.request.CorrectPreprocessingEventRequest;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventHashService;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.event.service.impl.EventHashServiceImpl;
import vn.nguongocso.event.service.processor.CoopProcessingPackagingProcessor;
import vn.nguongocso.event.service.processor.CoopWarehouseEventProcessor;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.certification.service.MilestoneValidationService;

@ExtendWith(MockitoExtension.class)
class ChainEventPackagingPreprocessingTest {

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EventValidationService eventValidationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    @Mock
    private MilestoneValidationService milestoneValidationService;

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
        organization.setName("Hợp tác xã nông sản sạch");

        productionLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô lúa vụ đông")
                .organization(organization)
                .status(ProductionLotStatus.APPROVED)
                .build();

        actor = new User();
        actor.setUserId(userId);
        actor.setFullName("Nguyễn Văn Ghi");

        lenient().when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        EventHashService eventHashService = new EventHashServiceImpl(objectMapper);
        ChainEventHashRecorder chainEventHashRecorder = new ChainEventHashRecorder(chainEventRepository, eventHashService);
        CoopProcessingPackagingProcessor coopProcessingPackagingProcessor = new CoopProcessingPackagingProcessor(
                productionLotRepository, chainEventRepository, userRepository,
                eventValidationService, milestoneValidationService, eventPublisher,
                objectMapper, clock
        );
        CoopWarehouseEventProcessor coopWarehouseEventProcessor = new CoopWarehouseEventProcessor(
                productionLotRepository, null,
                chainEventRepository, chainEventHashRecorder, userRepository,
                eventValidationService, eventPublisher, milestoneValidationService,
                objectMapper, clock, coopProcessingPackagingProcessor, null
        );
        chainEventService = new ChainEventServiceImpl(
                chainEventRepository, chainEventHashRecorder, null,
                coopWarehouseEventProcessor, null, null, null
        );
    }

    @Test
    void recordPackagingEvent_Success() throws JsonProcessingException {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        productionLot.setStatus(ProductionLotStatus.HARVESTED);
        productionLot.setHarvestDate(LocalDate.of(2026, 7, 24));

        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(productionLot.getId());
        packagingRequest.setPackagingSpecification("Túi 500g");
        packagingRequest.setPackagingDate(LocalDate.of(2026, 7, 25));

        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));
        when(milestoneValidationService.validateMilestoneCompletion(any(ProductionLot.class)))
                .thenReturn(List.of());

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PACKAGING)
                .eventData("{\"productionLotId\":\"" + productionLot.getId() + "\",\"packagingSpecification\":\"Túi 500g\",\"packagingDate\":\"2026-07-25\"}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .createdAt(LocalDateTime.now())
                .isCorrection(false)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordPackagingEvent(packagingRequest, validUser);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(ChainEventType.PACKAGING);
        assertThat(productionLot.getStatus()).isEqualTo(ProductionLotStatus.PACKAGED);
        verify(productionLotRepository, times(1)).save(productionLot);
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    void recordPackagingEvent_ThrowException_WhenLotNotHarvested() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        productionLot.setStatus(ProductionLotStatus.APPROVED);

        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(productionLot.getId());
        packagingRequest.setPackagingSpecification("Túi 500g");
        packagingRequest.setPackagingDate(LocalDate.of(2026, 7, 25));

        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        assertThatThrownBy(() -> chainEventService.recordPackagingEvent(packagingRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ được ghi nhận sự kiện đóng gói cho lô đã thu hoạch hoặc đã sơ chế.");
    }

    @Test
    void recordPackagingEvent_Success_WhenPackagingDateIsToday() throws JsonProcessingException {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        LocalDate today = LocalDate.now(clock);
        productionLot.setStatus(ProductionLotStatus.HARVESTED);
        productionLot.setHarvestDate(today);

        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(productionLot.getId());
        packagingRequest.setPackagingSpecification("Túi 500g");
        packagingRequest.setPackagingDate(today);

        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));
        when(milestoneValidationService.validateMilestoneCompletion(any(ProductionLot.class)))
                .thenReturn(List.of());

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PACKAGING)
                .eventData("{\"productionLotId\":\"" + productionLot.getId() + "\",\"packagingSpecification\":\"Túi 500g\",\"packagingDate\":\"" + today + "\"}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .createdAt(LocalDateTime.now())
                .isCorrection(false)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordPackagingEvent(packagingRequest, validUser);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(ChainEventType.PACKAGING);
        assertThat(productionLot.getStatus()).isEqualTo(ProductionLotStatus.PACKAGED);
        verify(productionLotRepository, times(1)).save(productionLot);
    }

    @Test
    void recordPackagingEvent_ThrowException_WhenPackagingDateIsInFuture() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        LocalDate futureDate = LocalDate.now(clock).plusDays(1);
        productionLot.setStatus(ProductionLotStatus.HARVESTED);
        productionLot.setHarvestDate(LocalDate.now(clock));

        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(productionLot.getId());
        packagingRequest.setPackagingSpecification("Túi 500g");
        packagingRequest.setPackagingDate(futureDate);

        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        assertThatThrownBy(() -> chainEventService.recordPackagingEvent(packagingRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ngày đóng gói không được là ngày ở tương lai.");
    }


    @Test
    void recordPreprocessingEvent_Success_TC01() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        productionLot.setStatus(ProductionLotStatus.HARVESTED);
        productionLot.setHarvestDate(LocalDate.now().minusDays(1));
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        ChainEvent savedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PREPROCESSING)
                .eventData("{\"lossRate\":10.0}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .build();
        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(savedEvent);

        vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest prepRequest = new vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest();
        prepRequest.setProductionLotId(productionLot.getId());
        prepRequest.setInputQuantity(1000.0);
        prepRequest.setOutputQuantity(900.0);
        prepRequest.setGrade("Hạng A");
        prepRequest.setProcessingMethod("Rửa sạch, sấy bớt nước");
        prepRequest.setPreprocessingDate(LocalDate.now());

        ChainEventResponse response = chainEventService.recordPreprocessingEvent(prepRequest, validUser);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(ChainEventType.PREPROCESSING);
        assertThat(productionLot.getStatus()).isEqualTo(ProductionLotStatus.PREPROCESSED);
        assertThat(productionLot.getActualQuantity()).isEqualTo(900.0);
        verify(productionLotRepository, times(1)).save(productionLot);
        verify(chainEventRepository, times(1)).save(any(ChainEvent.class));
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    void recordPreprocessingEvent_InvalidOutputQuantity_TC02() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        productionLot.setStatus(ProductionLotStatus.HARVESTED);
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest prepRequest = new vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest();
        prepRequest.setProductionLotId(productionLot.getId());
        prepRequest.setInputQuantity(1000.0);
        prepRequest.setOutputQuantity(1200.0);
        prepRequest.setPreprocessingDate(LocalDate.now());

        assertThatThrownBy(() -> chainEventService.recordPreprocessingEvent(prepRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Khối lượng sau sơ chế không được lớn hơn khối lượng vào.");

        verify(eventValidationService, times(1)).logFailedAttempt(any(), any(), eq(ChainEventType.PREPROCESSING), anyString(), any());
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordPreprocessingEvent_WrongStatus_TC03() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        productionLot.setStatus(ProductionLotStatus.APPROVED);
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest prepRequest = new vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest();
        prepRequest.setProductionLotId(productionLot.getId());
        prepRequest.setInputQuantity(1000.0);
        prepRequest.setOutputQuantity(900.0);
        prepRequest.setPreprocessingDate(LocalDate.now());

        assertThatThrownBy(() -> chainEventService.recordPreprocessingEvent(prepRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ được ghi nhận sự kiện sơ chế cho lô đã thu hoạch.");

        verify(eventValidationService, times(1)).logFailedAttempt(any(), any(), eq(ChainEventType.PREPROCESSING), anyString(), any());
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordPreprocessingEvent_WrongOrganization_TC04() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(UUID.randomUUID());

        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest prepRequest = new vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest();
        prepRequest.setProductionLotId(productionLot.getId());
        prepRequest.setInputQuantity(1000.0);
        prepRequest.setOutputQuantity(900.0);
        prepRequest.setPreprocessingDate(LocalDate.now());

        assertThatThrownBy(() -> chainEventService.recordPreprocessingEvent(prepRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không thuộc tổ chức quản lý của lô sản xuất này.");

        verify(eventValidationService, times(1)).logFailedAttempt(any(), any(), eq(ChainEventType.PREPROCESSING), anyString(), any());
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void correctPreprocessingEvent_Success() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        UUID originalEventId = UUID.randomUUID();
        ChainEvent originalEvent = ChainEvent.builder()
                .id(originalEventId)
                .eventType(ChainEventType.PREPROCESSING)
                .eventData("{\"productionLotId\":\"" + productionLot.getId() + "\"}")
                .build();

        when(chainEventRepository.findById(originalEventId)).thenReturn(Optional.of(originalEvent));
        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        ChainEvent correctionSaved = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PREPROCESSING)
                .eventData("{\"lossRate\":8.0}")
                .parentEvent(originalEvent)
                .isCorrection(true)
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .build();
        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(correctionSaved);

        vn.nguongocso.event.dto.request.CorrectPreprocessingEventRequest correctReq = new vn.nguongocso.event.dto.request.CorrectPreprocessingEventRequest();
        correctReq.setInputQuantity(1000.0);
        correctReq.setOutputQuantity(920.0);
        correctReq.setGrade("Hạng A");
        correctReq.setPreprocessingDate(LocalDate.now());
        correctReq.setCorrectionReason("Nhập sai khối lượng ra từ 900 thành 920kg");

        ChainEventResponse response = chainEventService.correctPreprocessingEvent(originalEventId, correctReq, validUser);

        assertThat(response).isNotNull();
        assertThat(productionLot.getActualQuantity()).isEqualTo(920.0);
        verify(chainEventRepository, times(1)).save(any(ChainEvent.class));
    }


}
