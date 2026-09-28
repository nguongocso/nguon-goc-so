package vn.nguongocso.event.service.processor;

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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.response.HarvestEligibilityResponse;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.HarvestEligibilityService;
import vn.nguongocso.organization.entity.Organization;

/**
 * Kiểm thử đơn vị cho HarvestEventProcessor (NCL-05-CN-008).
 * Xác thực nghiệp vụ ghi nhận sự kiện thu hoạch, kiểm tra điều kiện cách ly và quyền hạn vai trò.
 */
@ExtendWith(MockitoExtension.class)
class HarvestEventProcessorTest {

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
    private HarvestEligibilityService harvestEligibilityService;

    @Mock
    private Clock clock;

    private ChainEventServiceImpl chainEventService;

    private CustomUserDetails validUser;
    private ProductionLot productionLot;
    private Organization organization;
    private RecordHarvestEventRequest request;
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

        request = new RecordHarvestEventRequest();
        request.setProductionLotId(productionLot.getId());
        request.setHarvestDate(LocalDate.of(2026, 7, 24));
        request.setQuantity(1200.5);
        request.setLatitude(21.0285);
        request.setLongitude(105.8542);

        actor = new User();
        actor.setUserId(userId);
        actor.setFullName("Nguyễn Văn Ghi");

        lenient().when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        HarvestEventProcessor harvestEventProcessor = new HarvestEventProcessor(
                productionLotRepository, chainEventRepository, userRepository,
                harvestEligibilityService, eventValidationService, eventPublisher,
                objectMapper, clock
        );
        chainEventService = new ChainEventServiceImpl(
                chainEventRepository, null, harvestEventProcessor, null, null, null, null
        );
    }

    @Test
    void shouldRecordHarvestEventSuccessfullyWhenRequestIsValid() throws JsonProcessingException {
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
                .eventData("{\"productionLotId\":\"" + productionLot.getId()
                        + "\",\"harvestDate\":\"2026-07-24\",\"quantity\":1200.5}")
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
    void shouldThrowBusinessExceptionWhenEarlyHarvestRecordedByVT03() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

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
    void shouldThrowBusinessExceptionWhenEarlyHarvestByManagerVT02WithoutReason() {
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
    void shouldSucceedWhenEarlyHarvestByManagerVT02WithValidReason() {
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
                .eventData("{\"productionLotId\":\"" + productionLot.getId()
                        + "\",\"earlyHarvest\":true,\"earlyHarvestReason\":\"Thu hoạch gấp do bão lũ tràn về\"}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordHarvestEvent(request, validUser);

        assertThat(response).isNotNull();
        verify(chainEventRepository).save(any(ChainEvent.class));
    }

    @Test
    void shouldSucceedWithSoftWarningWhenUnmatchedMaterialDetected() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(false)
                        .eligibleHarvestDate(null)
                        .unmatchedMaterials(List.of("Chế phẩm lạ"))
                        .build());

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + productionLot.getId()
                        + "\",\"unmatchedMaterials\":[\"Chế phẩm lạ\"]}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordHarvestEvent(request, validUser);

        assertThat(response).isNotNull();
        verify(chainEventRepository).save(any(ChainEvent.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenEarlyHarvestByManagerVT02WithEmptyReason() {
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

        verify(eventValidationService).logFailedAttempt(
                eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void shouldThrowBusinessExceptionWhenEarlyHarvestByManagerVT02WithWhitespaceReason() {
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

        verify(eventValidationService).logFailedAttempt(
                eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void shouldThrowBusinessExceptionAndLogFailedAttemptWhenEarlyHarvestByAdminVT01() {
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

        verify(eventValidationService).logFailedAttempt(
                eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void shouldThrowBusinessExceptionAndLogFailedAttemptWhenPesticideLogMissingExecutedDate() {
        when(validUser.getRoleCode()).thenReturn("VT-02");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId()))
                .thenThrow(new BusinessException(
                        "Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện. "
                                + "Vui lòng bổ sung ngày trước khi thu hoạch."));

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện");

        verify(eventValidationService).logFailedAttempt(
                eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void shouldThrowBusinessExceptionWhenEarlyHarvestByRecorderVT03EvenWithReason() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        when(harvestEligibilityService.calculateHarvestEligibility(productionLot.getId())).thenReturn(
                HarvestEligibilityResponse.builder()
                        .determined(true)
                        .eligibleHarvestDate(LocalDate.of(2026, 7, 28))
                        .build());

        request.setEarlyHarvestReason("Lý do tự điền");

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Người ghi sự kiện không có quyền ghi đè thu hoạch sớm");

        verify(eventValidationService).logFailedAttempt(
                eq(request.getProductionLotId()), anyString(), eq(ChainEventType.HARVEST), anyString(), eq(validUser));
    }

    @Test
    void shouldSucceedWhenUnmatchedMaterialHarvestedByManagerVT02() {
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

        request.setEarlyHarvestReason(null);

        ChainEvent mockSavedEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + productionLot.getId()
                        + "\",\"earlyHarvest\":false,\"unmatchedMaterials\":[\"Thuốc trừ sâu sinh học thảo mộc\"]}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(actor)
                .build();

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        ChainEventResponse response = chainEventService.recordHarvestEvent(request, validUser);

        assertThat(response).isNotNull();
        verify(chainEventRepository).save(any(ChainEvent.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenRoleIsInvalid() {
        when(validUser.getRoleCode()).thenReturn("VT-06");

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ thành viên được cấp quyền trong tổ chức mới được ghi sự kiện.");

        verifyNoInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void shouldThrowBusinessExceptionWhenProductionLotNotFound() {
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất.");

        verifyNoMoreInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void shouldThrowBusinessExceptionWhenUserBelongsToDifferentOrganization() {
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
    void shouldThrowBusinessExceptionWhenProductionLotNotApproved() {
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
    void shouldThrowBusinessExceptionWhenHarvestDateIsInFuture() {
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
    void shouldThrowBusinessExceptionWhenHarvestDateIsBeforePlantingDate() {
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
}
