package vn.nguongocso.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordTransportEventRequest;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.event.service.EventValidationService;

@ExtendWith(MockitoExtension.class)
class ChainEventServiceImplTest {

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

    @InjectMocks
    private ChainEventServiceImpl chainEventService;
    
    @Mock
    private TraceCodeRepository traceCodeRepository;

    private CustomUserDetails validUser; // Mocked UserDetails
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
        // Mock CustomUserDetails
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
    }

    @Test
    void recordHarvestEvent_Success() throws JsonProcessingException {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03"); // EVENT_RECORDER
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

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

        // When
        ChainEventResponse response = chainEventService.recordHarvestEvent(request, validUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(ChainEventType.HARVEST);
        assertThat(response.getEventData()).containsEntry("productionLotId", productionLot.getId().toString());
        assertThat(response.getEventData()).containsEntry("quantity", 1200.5);
        assertThat(response.getRecordedByName()).isEqualTo("Nguyễn Văn Ghi");

        // Kiểm tra thay đổi trạng thái của lô sản xuất
        assertThat(productionLot.getStatus()).isEqualTo(ProductionLotStatus.HARVESTED);
        assertThat(productionLot.getHarvestDate()).isEqualTo(request.getHarvestDate());
        assertThat(productionLot.getActualQuantity()).isEqualTo(request.getQuantity());

        verify(productionLotRepository, times(1)).save(productionLot);
        verify(chainEventRepository, times(1)).save(any(ChainEvent.class));
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenRoleIsInvalid() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-06"); // CONSUMER - Vai trò không được quyền

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ thành viên được cấp quyền trong tổ chức mới được ghi sự kiện.");

        verifyNoInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenProductionLotNotFound() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất.");

        verifyNoMoreInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenDifferentOrganization() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(UUID.randomUUID()); // Tổ chức khác tổ chức của Lô

        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không thuộc tổ chức quản lý của lô sản xuất này.");

        verifyNoMoreInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordHarvestEvent_ThrowException_WhenProductionLotNotApproved() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        productionLot.setStatus(ProductionLotStatus.DRAFT); // Chưa được duyệt

        when(productionLotRepository.findById(request.getProductionLotId())).thenReturn(Optional.of(productionLot));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordHarvestEvent(request, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.");

        verifyNoMoreInteractions(productionLotRepository);
        verifyNoInteractions(chainEventRepository);
    }
    @Test
    void recordPackagingEvent_Success() throws JsonProcessingException {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        productionLot.setStatus(ProductionLotStatus.HARVESTED); // Đã thu hoạch
        productionLot.setHarvestDate(LocalDate.of(2026, 7, 24));

        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(productionLot.getId());
        packagingRequest.setPackagingSpecification("Túi 500g");
        packagingRequest.setPackagingDate(LocalDate.of(2026, 7, 25));

        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

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

        // When
        ChainEventResponse response = chainEventService.recordPackagingEvent(packagingRequest, validUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(ChainEventType.PACKAGING);
        assertThat(productionLot.getStatus()).isEqualTo(ProductionLotStatus.PACKAGED);
        verify(productionLotRepository, times(1)).save(productionLot);
    }

    @Test
    void recordPackagingEvent_ThrowException_WhenLotNotHarvested() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());

        productionLot.setStatus(ProductionLotStatus.APPROVED); // Chưa thu hoạch

        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(productionLot.getId());
        packagingRequest.setPackagingSpecification("Túi 500g");
        packagingRequest.setPackagingDate(LocalDate.of(2026, 7, 25));

        when(productionLotRepository.findById(productionLot.getId())).thenReturn(Optional.of(productionLot));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordPackagingEvent(packagingRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ được ghi nhận sự kiện đóng gói cho lô đã thu hoạch.");
    }
    
    @Test
    void recordTransportEvent_Success() throws JsonProcessingException {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId());
        when(validUser.getUserId()).thenReturn(userId);

        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        // Mock JSON serialization (ObjectMapper spy)
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

        when(chainEventRepository.save(any(ChainEvent.class))).thenReturn(mockSavedEvent);

        // When
        ChainEventResponse response = chainEventService.recordTransportEvent(transportRequest, validUser);

        // Then
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
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenRoleIsInvalid() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-06"); // CONSUMER

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không có quyền ghi sự kiện vận chuyển.");

        verifyNoInteractions(traceCodeRepository);
        verifyNoInteractions(chainEventRepository);
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenTraceCodeNotFound() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mã lô hàng không tồn tại.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenShipmentIsNull() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        traceCode.setShipment(null);
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mã truy xuất chưa được gắn với lô hàng.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }
    
    @Test
    void recordTransportEvent_ThrowException_WhenShipmentRecalled() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId()); // ✅ Thêm dòng này
        shipment.setStatus(ShipmentStatus.RECALLED);
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lô hàng đã bị thu hồi, không thể ghi sự kiện vận chuyển.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }

    @Test
    void recordTransportEvent_ThrowException_WhenShipmentNotActivated() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(organization.getOrganizationId()); 
        shipment.setStatus(ShipmentStatus.DRAFT);
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lô hàng chưa được kích hoạt, không thể ghi sự kiện vận chuyển.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }
    
    
    @Test
    void recordTransportEvent_ThrowException_WhenOrganizationMismatch() {
        // Given
        when(validUser.getRoleCode()).thenReturn("VT-03");
        when(validUser.getOrganizationId()).thenReturn(UUID.randomUUID()); // khác với shipment
        when(traceCodeRepository.findByCodeValue(transportRequest.getCodeValue()))
                .thenReturn(Optional.of(traceCode));

        // When & Then
        assertThatThrownBy(() -> chainEventService.recordTransportEvent(transportRequest, validUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không thuộc tổ chức quản lý của lô hàng.");

        verify(traceCodeRepository, times(1)).findByCodeValue(transportRequest.getCodeValue());
        verifyNoInteractions(chainEventRepository);
    }

}
