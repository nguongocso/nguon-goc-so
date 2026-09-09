package vn.nguongocso.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import org.mockito.InjectMocks;
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
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventHashService;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;
import vn.nguongocso.farm.service.HarvestEligibilityService;

import vn.nguongocso.trace.repository.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
class ChainEventServiceImplTest {

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EventValidationService eventValidationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EventHashService eventHashService;

    @Mock
    private HarvestEligibilityService harvestEligibilityService;

    @Mock
    private Clock clock;

    @Mock
    private vn.nguongocso.certification.service.MilestoneValidationService milestoneValidationService;

    @InjectMocks
    private ChainEventServiceImpl chainEventService;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    private CustomUserDetails validUser;
    private ProductionLot productionLot;
    private Organization organization;
    private RecordHarvestEventRequest request;
    private User actor;
    private UUID userId;

    private TraceCode traceCode;
    private Shipment shipment;
    private RecordTransportEventRequest transportRequest;

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

        request = new RecordHarvestEventRequest();
        request.setProductionLotId(productionLot.getId());
        request.setHarvestDate(LocalDate.of(2026, 7, 24));
        request.setQuantity(1200.5);
        request.setLatitude(21.0285);
        request.setLongitude(105.8542);

        actor = new User();
        actor.setUserId(userId);
        actor.setFullName("Nguyễn Văn Ghi");

        // ===== Transport event =====
        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setOrganization(organization);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setCodeValue("HX00000029");
        traceCode.setShipment(shipment);

        transportRequest = new RecordTransportEventRequest();
        transportRequest.setCodeValue("HX00000029");
        transportRequest.setFromLocation("Xã Long Cốc, huyện Tân Sơn, Phú Thọ");
        transportRequest.setToLocation("Kho trung chuyển Việt Trì, Phú Thọ");
        transportRequest.setTransportTime(LocalDateTime.of(2026, 7, 24, 9, 0, 0));

        lenient().when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    void recordHarvestEvent_Success() throws JsonProcessingException {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));
        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(null)
                        .build());

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + productionLot.getId() + "\",\"harvestDate\":\"2026-07-24\",\"quantity\":1200.5}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .createdAt(LocalDateTime.now())
                .isCorrection(false)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordHarvestEvent(request, validUser);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(ChainEventType.HARVEST);
        assertThat(response.getEventData()).containsEntry("productionLotId", productionLot.getId().toString());
        assertThat(response.getEventData()).containsEntry("quantity", 1200.5);
        assertThat(response.getRecordedByName()).isEqualTo("Nguyễn Văn Ghi");

        assertThat(productionLot.getStatus()).isEqualTo(ProductionLotStatus.HARVESTED);
        assertThat(productionLot.getHarvestDate()).isEqualTo(request.getHarvestDate());
        assertThat(productionLot.getActualQuantity()).isEqualTo(request.getQuantity());

        verify(productionLotRepository, times(1)).save(productionLot);
        verify(chainEventRepository, times(1)).save(any(ChainEvent.class));
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    void recordHarvestEvent_EarlyHarvest_ByRecorderVT03_ShouldThrowBusinessException() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        // Lô có ngày đủ điều kiện là 2026-07-28 trong khi request harvestDate là 2026-07-24
        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(LocalDate.of(2026, 7, 28))
                        .build());

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Người ghi sự kiện không có quyền ghi đè thu hoạch sớm");
    }

    @Test
    void recordHarvestEvent_EarlyHarvest_ByManagerVT02_WithoutReason_ShouldThrowBusinessException() {
        when(validUser.getRoleCode()).thenReturn("VT-02");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(LocalDate.of(2026, 7, 28))
                        .build());

        request.setEarlyHarvestReason(null);

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quản lý cần nhập lý do ghi đè bắt buộc");
    }

    @Test
    void recordHarvestEvent_EarlyHarvest_ByManagerVT02_WithReason_ShouldSucceed() {
        when(validUser.getRoleCode()).thenReturn("VT-02");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(LocalDate.of(2026, 7, 28))
                        .build());

        request.setEarlyHarvestReason("Thu hoạch gấp do bão lũ tràn về");

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + productionLot.getId() + "\",\"earlyHarvest\":true,\"earlyHarvestReason\":\"Thu hoạch gấp do bão lũ tràn về\"}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordHarvestEvent(request, validUser);

        assertThat(response).isNotNull();
        verify(chainEventRepository).save(any(ChainEvent.class));
    }

    @Test
    void recordHarvestEvent_UnmatchedMaterial_SoftWarning_ShouldSucceed() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        // Gate 1 Option A: determined = false do vật tư ngoài danh mục -> Soft warning, allow harvest
        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(false)
                        .eligibleHarvestDate(null)
                        .unmatchedMaterials(List.of("Chế phẩm lạ"))
                        .build());

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + productionLot.getId() + "\",\"unmatchedMaterials\":[\"Chế phẩm lạ\"]}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordHarvestEvent(request, validUser);

        assertThat(response).isNotNull();
        verify(chainEventRepository).save(any(ChainEvent.class));
    }

    @Test
    void recordHarvestEvent_EarlyHarvest_ByManagerVT02_EmptyReason_ShouldThrowBusinessException() {
        when(validUser.getRoleCode()).thenReturn("VT-02");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(LocalDate.of(2026, 7, 28))
                        .build());

        request.setEarlyHarvestReason("");

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quản lý cần nhập lý do ghi đè bắt buộc");

        verify(eventValidationService).logFailedAttempt(eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void recordHarvestEvent_EarlyHarvest_ByManagerVT02_WhitespaceReason_ShouldThrowBusinessException() {
        when(validUser.getRoleCode()).thenReturn("VT-02");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(LocalDate.of(2026, 7, 28))
                        .build());

        request.setEarlyHarvestReason("   \t  \n  ");

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quản lý cần nhập lý do ghi đè bắt buộc");

        verify(eventValidationService).logFailedAttempt(eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void recordHarvestEvent_EarlyHarvest_ByAdminVT01_ShouldThrowBusinessExceptionAndLogFailedAttempt() {
        when(validUser.getRoleCode()).thenReturn("VT-01");
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(LocalDate.of(2026, 7, 28))
                        .build());

        request.setEarlyHarvestReason("Chỉ đạo thu hoạch sớm từ Admin HTX");

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch sớm");

        verify(eventValidationService).logFailedAttempt(eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void recordHarvestEvent_WhenPesticideLogMissingExecutedDate_ShouldThrowBusinessExceptionAndLogFailedAttempt() {
        when(validUser.getRoleCode()).thenReturn("VT-02");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId()))
                .thenThrow(new BusinessException("Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện. Vui lòng bổ sung ngày trước khi thu hoạch."));

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện");

        verify(eventValidationService).logFailedAttempt(eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void recordHarvestEvent_EarlyHarvest_ByRecorderVT03_EvenWithReason_ShouldThrowBusinessException() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(LocalDate.of(2026, 7, 28))
                        .build());

        // Dù VT-03 cố tình truyền reason
        request.setEarlyHarvestReason("Lý do tự điền");

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Người ghi sự kiện không có quyền ghi đè thu hoạch sớm");

        verify(eventValidationService).logFailedAttempt(eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void recordHarvestEvent_UnmatchedMaterial_ByManagerVT02_ShouldSucceed() {
        when(validUser.getRoleCode()).thenReturn("VT-02");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(false)
                        .eligibleHarvestDate(null)
                        .unmatchedMaterials(List.of("Thuốc trừ sâu sinh học thảo mộc"))
                        .build());

        // Không cần truyền earlyHarvestReason khi determined = false
        request.setEarlyHarvestReason(null);

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + productionLot.getId() + "\",\"earlyHarvest\":false,\"unmatchedMaterials\":[\"Thuốc trừ sâu sinh học thảo mộc\"]}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordHarvestEvent(request, validUser);

        assertThat(response).isNotNull();
        verify(chainEventRepository).save(any(ChainEvent.class));
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenRoleIsInvalid() {
        when(validUser.getRoleCode()).thenReturn("VT-06");

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ thành viên được cấp quyền trong tổ chức mới được ghi sự kiện.");

        verifyNoInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenProductionLotNotFound() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất.");

        verifyNoMoreInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenDifferentOrganization() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(UUID.randomUUID());

        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không thuộc tổ chức quản lý của lô sản xuất này.");

        verifyNoMoreInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenProductionLotNotApproved() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        productionLot.setStatus(ProductionLotStatus.DRAFT);

        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.");

        verifyNoMoreInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenHarvestDateIsInFuture() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        LocalDate futureDate = LocalDate.now(clock).plusDays(1);
        productionLot.setStatus(ProductionLotStatus.APPROVED);

        RecordHarvestEventRequest harvestReq = new RecordHarvestEventRequest();
        harvestReq.setProductionLotId(productionLot.getId());
        harvestReq.setHarvestDate(futureDate);
        harvestReq.setQuantity(100.0);

        when(productionLotRepository.findById(harvestReq.getProductionLotId())).thenReturn(Optional.of(productionLot));

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(harvestReq, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ngày thu hoạch không được là ngày ở tương lai.");

        verify(eventValidationService).logFailedAttempt(
                harvestReq.getProductionLotId(),
                productionLot.getName(),
                ChainEventType.HARVEST,
                "Ngày thu hoạch không được là ngày ở tương lai.",
                validUser);
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenHarvestDateIsBeforePlantingDate() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        productionLot.setStatus(ProductionLotStatus.APPROVED);
        productionLot.setPlantingDate(LocalDate.of(2026, 6, 1));

        RecordHarvestEventRequest harvestReq = new RecordHarvestEventRequest();
        harvestReq.setProductionLotId(productionLot.getId());
        harvestReq.setHarvestDate(LocalDate.of(2026, 5, 20));
        harvestReq.setQuantity(100.0);

        when(productionLotRepository.findById(harvestReq.getProductionLotId())).thenReturn(Optional.of(productionLot));

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(harvestReq, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ngày thu hoạch phải sau hoặc bằng ngày gieo trồng của lô.");

        verify(eventValidationService).logFailedAttempt(
                harvestReq.getProductionLotId(),
                productionLot.getName(),
                ChainEventType.HARVEST,
                "Ngày thu hoạch phải sau hoặc bằng ngày gieo trồng của lô.",
                validUser);
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
    void recordTransportEvent_Success() throws JsonProcessingException {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        String expectedJson = "{\"fromLocation\":\"Xã Long Cốc, huyện Tân Sơn, Phú Thọ\",\"toLocation\":\"Kho trung chuyển Việt Trì, Phú Thọ\"}";
        doReturn(expectedJson).when(objectMapper).writeValueAsString(any(Map.class));

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(ChainEventType.TRANSPORT)
                .eventData(expectedJson)
                .recordedAt(transportRequest.getTransportTime())
                .recordedBy(actor)
                .isCorrection(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventRepository.findTopByShipmentIdOrderByCreatedAtDesc(shipment.getId()))
                .thenReturn(Optional.empty());
        when(eventHashService.calculateHash(any(ChainEvent.class), any(String.class)))
                .thenReturn("mockHash");
        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordTransportEvent(transportRequest, validUser);

        assertThat(response).isNotNull();
        assertThat(response.getShipmentId()).isEqualTo(shipment.getId());
        assertThat(response.getEventType()).isEqualTo(ChainEventType.TRANSPORT);
        assertThat(response.getEventData())
                .containsEntry("fromLocation", "Xã Long Cốc, huyện Tân Sơn, Phú Thọ")
                .containsEntry("toLocation", "Kho trung chuyển Việt Trì, Phú Thọ");
        assertThat(response.getRecordedAt()).isEqualTo(transportRequest.getTransportTime());
        assertThat(response.getRecordedByName()).isEqualTo("Nguyễn Văn Ghi");

        verify(chainEventRepository, times(1)).save(any(ChainEvent.class));
        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenRoleIsInvalid() {
        when(validUser.getRoleCode()).thenReturn("VT-06");

        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không có quyền ghi sự kiện vận chuyển.");

        verifyNoInteractions(traceCodeRepository);
        verifyNoInteractions(chainEventRepository);
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenTraceCodeNotFound() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mã lô hàng không tồn tại.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenShipmentIsNull() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        traceCode.setShipment(null);
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));

        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mã truy xuất chưa được gắn với lô hàng.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenShipmentRecalled() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        shipment.setStatus(ShipmentStatus.RECALLED);
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));

        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lô hàng đã bị thu hồi, không thể ghi sự kiện vận chuyển.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordTransportEvent_ThrowException_WhenShipmentNotActivated() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId()); 
        shipment.setStatus(ShipmentStatus.DRAFT);
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));

        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lô hàng chưa được kích hoạt, không thể ghi sự kiện vận chuyển.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenOrganizationMismatch() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(UUID.randomUUID());
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));

        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không có quyền ghi sự kiện cho lô hàng của tổ chức này.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }

    // =========================================================================
    // TEST CASES FOR NCL-11-CN-001 SƠ CHẾ VÀ PHÂN LOẠI
    // =========================================================================

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

    // ==========================================
    // NCL-05-CN-011: SỰ KIỆN KHO HỢP TÁC XÃ (ENTRY & EXIT)
    // ==========================================

    @Test
    void recordWarehouseEntryAndExit_Success_TC01() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lô hàng Vải Thiều 1");
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        vn.nguongocso.farm.entity.ProductCategory category = vn.nguongocso.farm.entity.ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Chè Ô Long")
                .maxStorageDays(5)
                .build();
        productionLot.setProductCategory(category);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        // 1. Ghi nhập kho
        vn.nguongocso.event.dto.request.RecordWarehouseEntryRequest entryReq = new vn.nguongocso.event.dto.request.RecordWarehouseEntryRequest();
        entryReq.setShipmentId(shipment.getId());
        entryReq.setEntryTime(LocalDateTime.of(2026, 9, 1, 8, 0));
        entryReq.setWarehouseName("Kho lạnh HTX số 1");
        entryReq.setStorageCondition("Nhiệt độ 5°C");

        ChainEvent entrySaved = ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_ENTRY)
                .eventData("{\"shipmentId\":\"" + shipment.getId() + "\",\"warehouseName\":\"Kho lạnh HTX số 1\",\"entryTime\":\"2026-09-01T08:00:00\"}")
                .recordedAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .recordedBy(actor)
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of())
                .thenReturn(List.of(entrySaved));
        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(entrySaved);

        vn.nguongocso.event.dto.response.CoopWarehouseEventResponse entryResp = chainEventService.recordWarehouseEntryEvent(entryReq, validUser);

        assertThat(entryResp).isNotNull();
        assertThat(entryResp.getEventType()).isEqualTo(ChainEventType.WAREHOUSE_ENTRY);
        assertThat(entryResp.getWarehouseName()).isEqualTo("Kho lạnh HTX số 1");

        // 2. Ghi xuất kho (3 ngày sau)
        vn.nguongocso.event.dto.request.RecordWarehouseExitRequest exitReq = new vn.nguongocso.event.dto.request.RecordWarehouseExitRequest();
        exitReq.setShipmentId(shipment.getId());
        exitReq.setExitTime(LocalDateTime.of(2026, 9, 4, 8, 0));
        exitReq.setDestination("Xe vận chuyển công ty A");

        ChainEvent exitSaved = ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_EXIT)
                .eventData("{\"shipmentId\":\"" + shipment.getId() + "\",\"storageDurationDays\":3}")
                .recordedAt(LocalDateTime.of(2026, 9, 4, 8, 0))
                .recordedBy(actor)
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(exitSaved);

        vn.nguongocso.event.dto.response.CoopWarehouseEventResponse exitResp = chainEventService.recordWarehouseExitEvent(exitReq, validUser);

        assertThat(exitResp).isNotNull();
        assertThat(exitResp.getEventType()).isEqualTo(ChainEventType.WAREHOUSE_EXIT);
        assertThat(exitResp.getStorageDurationDays()).isEqualTo(3L);
        assertThat(exitResp.getIsStorageExceeded()).isFalse();
    }

    @Test
    void recordWarehouseExit_ThrowException_WhenNoEntryEvent_TC02() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lô hàng Vải Thiều 1");
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId())).thenReturn(List.of());

        vn.nguongocso.event.dto.request.RecordWarehouseExitRequest exitReq = new vn.nguongocso.event.dto.request.RecordWarehouseExitRequest();
        exitReq.setShipmentId(shipment.getId());
        exitReq.setExitTime(LocalDateTime.of(2026, 9, 4, 8, 0));

        assertThatThrownBy(() -> chainEventService.recordWarehouseExitEvent(exitReq, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa được ghi nhận nhập kho HTX. Vui lòng ghi sự kiện nhập kho trước khi xuất kho.");
    }

    @Test
    void recordWarehouseExit_Warning_WhenStorageExceeded_TC03() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lô hàng Vải Thiều 1");
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        vn.nguongocso.farm.entity.ProductCategory category = vn.nguongocso.farm.entity.ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Rau Cải Thìa")
                .maxStorageDays(2) // Ngưỡng tối đa 2 ngày
                .build();
        productionLot.setProductCategory(category);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        ChainEvent entrySaved = ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_ENTRY)
                .eventData("{\"shipmentId\":\"" + shipment.getId() + "\",\"warehouseName\":\"Kho HTX\",\"entryTime\":\"2026-09-01T08:00:00\"}")
                .recordedAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .recordedBy(actor)
                .build();

        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of(entrySaved));

        ChainEvent exitSaved = ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_EXIT)
                .eventData("{\"isStorageExceeded\":true}")
                .recordedAt(LocalDateTime.of(2026, 9, 4, 8, 0)) // 3 ngày sau > 2 ngày
                .recordedBy(actor)
                .build();
        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(exitSaved);

        vn.nguongocso.event.dto.request.RecordWarehouseExitRequest exitReq = new vn.nguongocso.event.dto.request.RecordWarehouseExitRequest();
        exitReq.setShipmentId(shipment.getId());
        exitReq.setExitTime(LocalDateTime.of(2026, 9, 4, 8, 0));

        vn.nguongocso.event.dto.response.CoopWarehouseEventResponse exitResp = chainEventService.recordWarehouseExitEvent(exitReq, validUser);

        assertThat(exitResp).isNotNull();
        assertThat(exitResp.getStorageDurationDays()).isEqualTo(3L);
        assertThat(exitResp.getIsStorageExceeded()).isTrue();
        assertThat(exitResp.getWarningMessage()).contains("vượt quá ngưỡng bảo quản cho phép (2 ngày)");
    }

    @Test
    void recordWarehouseEntry_ThrowException_WhenAlreadyInWarehouse_TC04() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lô hàng Vải Thiều 1");
        shipment.setOrganization(organization);
        shipment.setProductionLot(productionLot);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        ChainEvent existingEntry = ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(ChainEventType.WAREHOUSE_ENTRY)
                .recordedAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .build();

        // Đã có 1 nhập kho và chưa xuất kho
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of(existingEntry));

        vn.nguongocso.event.dto.request.RecordWarehouseEntryRequest entryReq = new vn.nguongocso.event.dto.request.RecordWarehouseEntryRequest();
        entryReq.setShipmentId(shipment.getId());
        entryReq.setEntryTime(LocalDateTime.of(2026, 9, 2, 8, 0));
        entryReq.setWarehouseName("Kho HTX số 2");

        assertThatThrownBy(() -> chainEventService.recordWarehouseEntryEvent(entryReq, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang trong kho HTX, vui lòng ghi xuất kho trước khi nhập kho mới.");
    }
}
