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
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;
import vn.nguongocso.event.dto.request.RecordWarehouseEntryRequest;
import vn.nguongocso.event.dto.request.RecordWarehouseExitRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.CoopWarehouseEventResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventHashService;
import vn.nguongocso.event.service.EventValidationService;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.event.service.impl.EventHashServiceImpl;
import vn.nguongocso.event.service.processor.CoopTransportEventProcessor;
import vn.nguongocso.event.service.processor.CoopWarehouseEventProcessor;
import vn.nguongocso.event.service.recorder.ChainEventHashRecorder;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.certification.service.MilestoneValidationService;

@ExtendWith(MockitoExtension.class)
class ChainEventWarehouseServiceTest {

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
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private EventHashService eventHashService;

    @Mock
    private Clock clock;

    @Mock
    private MilestoneValidationService milestoneValidationService;

    private ChainEventServiceImpl chainEventService;

    private CustomUserDetails validUser;
    private Organization organization;
    private ProductionLot productionLot;
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

        actor = new User();
        actor.setUserId(userId);
        actor.setFullName("Nguyễn Văn Ghi");

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

        ChainEventHashRecorder chainEventHashRecorder = new ChainEventHashRecorder(chainEventRepository, eventHashService);
        CoopTransportEventProcessor coopTransportEventProcessor = new CoopTransportEventProcessor(
                traceCodeRepository, userRepository, chainEventHashRecorder,
                eventValidationService, eventPublisher, objectMapper);
        CoopWarehouseEventProcessor coopWarehouseEventProcessor = new CoopWarehouseEventProcessor(
                productionLotRepository, shipmentRepository,
                chainEventRepository, chainEventHashRecorder, userRepository,
                eventValidationService, eventPublisher, milestoneValidationService,
                objectMapper, clock, null, coopTransportEventProcessor
        );
        chainEventService = new ChainEventServiceImpl(
                chainEventRepository, chainEventHashRecorder, null,
                coopWarehouseEventProcessor, null, null, null
        );
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
