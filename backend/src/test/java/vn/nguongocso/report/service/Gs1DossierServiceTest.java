package vn.nguongocso.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.report.dto.response.Gs1DossierExportResponse;
import vn.nguongocso.report.dto.response.Gs1Event;
import vn.nguongocso.report.repository.DossierExportHistoryRepository;
import vn.nguongocso.report.service.impl.DossierServiceImpl;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Test service xuất hồ sơ GS1 (NCL-12-CN-003).
 */
@ExtendWith(MockitoExtension.class)
public class Gs1DossierServiceTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private FarmLogRepository farmLogRepository;
    @Mock private FarmLogAttachmentRepository farmLogAttachmentRepository;
    @Mock private ChainEventRepository chainEventRepository;
    @Mock private DossierExportHistoryRepository exportHistoryRepository;
    @Mock private vn.nguongocso.auth.repository.UserRepository userRepository;
    @Mock private OrganizationUserRepository organizationUserRepository;
    @Mock private TraceCodeRepository traceCodeRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private DossierServiceImpl dossierService;

    private UUID shipmentId;
    private Shipment shipment;
    private ProductionLot productionLot;
    private Organization org;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        org = Organization.builder().organizationId(UUID.randomUUID()).name("HTX Long Cốc").code("HTX-LC").build();
        productionLot = ProductionLot.builder()
                .id(UUID.randomUUID()).name("Lô chè giống mới").organization(org)
                .productCategory(ProductCategory.builder().id(UUID.randomUUID()).name("Chè").build())
                .status(ProductionLotStatus.CLOSED).expectedQuantity(1000.0).expectedQuantityUnit("kg")
                .build();
        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng xuất khẩu");
        shipment.setProductionLot(productionLot);
        shipment.setOrganization(org);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setTotalQuantity(500L);
        userDetails = mock(CustomUserDetails.class);
    }

    private void mockEligibleQtN11() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userDetails.getRoleCode()).thenReturn("VT-02");
        when(userDetails.getOrganizationId()).thenReturn(org.getOrganizationId());
        List<FarmLog> logs = List.of(
                farmLog(FarmActivityType.PLANTING), farmLog(FarmActivityType.FERTILIZING),
                farmLog(FarmActivityType.PESTICIDE), farmLog(FarmActivityType.HARVESTING));
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(productionLot.getId())).thenReturn(logs);
        when(farmLogAttachmentRepository.findByFarmLogId(any(UUID.class)))
                .thenReturn(Collections.singletonList(FarmLogAttachment.builder().fileName("doc.pdf").build()));
    }

    @Test
    void export_success_mapsEventAndLogsAudit() {
        mockEligibleQtN11();
        when(userDetails.getFullName()).thenReturn("Nguyễn Văn A");
        ChainEvent ev = ChainEvent.builder()
                .id(UUID.randomUUID()).eventType(ChainEventType.HARVEST)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 10, 0))
                .eventData("{\"quantity\":500}")
                .location(new GeometryFactory().createPoint(new Coordinate(105.8542, 21.0285)))
                .build();
        when(chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId)).thenReturn(List.of(ev));
        TraceCode tc = new TraceCode();
        tc.setCodeValue("TANCUONG00000059");
        when(traceCodeRepository.findByShipmentId(shipmentId)).thenReturn(List.of(tc));

        Gs1DossierExportResponse response = dossierService.exportGs1Dossier(shipmentId, "json", true, userDetails, "127.0.0.1");

        assertThat(response.getShipment().getCodeValues()).containsExactly("TANCUONG00000059");
        Gs1Event gs1Event = response.getEvents().get(0);
        assertThat(gs1Event.getEventType()).isEqualTo("HARVEST");
        assertThat(gs1Event.getEventTypeLabel()).isEqualTo("Thu hoạch");
        assertThat(gs1Event.getLocation().getLatitude()).isEqualTo(21.0285);
        assertThat(gs1Event.getDetails()).containsEntry("quantity", 500);
        assertThat(response.getMapping()).containsEntry("ChainEvent.id", "eventIdentifier");
        assertThat(response.getExportedBy()).isEqualTo("Nguyễn Văn A");
        verify(eventPublisher, times(1)).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    void export_missingLocation_addsWarning() {
        mockEligibleQtN11();
        ChainEvent ev = ChainEvent.builder()
                .id(UUID.randomUUID()).eventType(ChainEventType.TRANSPORT)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 11, 0)).location(null).build();
        when(chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId)).thenReturn(List.of(ev));

        Gs1DossierExportResponse response = dossierService.exportGs1Dossier(shipmentId, "json", true, userDetails, "127.0.0.1");

        assertThat(response.getEvents().get(0).getLocation()).isNull();
        assertThat(response.getWarnings()).anySatisfy(w -> assertThat(w.getField()).isEqualTo("location"));
    }

    @Test
    void export_noEvents_throws() {
        mockEligibleQtN11();
        when(chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> dossierService.exportGs1Dossier(shipmentId, "json", true, userDetails, "127.0.0.1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Lô chưa có sự kiện nào");
    }

    @Test
    void export_invalidFormat_throws() {
        assertThatThrownBy(() -> dossierService.exportGs1Dossier(shipmentId, "pdf", true, userDetails, "127.0.0.1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Định dạng xuất không được hỗ trợ");
    }

    @Test
    void export_includeMappingFalse_mappingNull() {
        mockEligibleQtN11();
        ChainEvent ev = ChainEvent.builder()
                .id(UUID.randomUUID()).eventType(ChainEventType.PROCUREMENT)
                .recordedAt(LocalDateTime.of(2026, 8, 11, 11, 30)).build();
        when(chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId)).thenReturn(List.of(ev));

        Gs1DossierExportResponse response = dossierService.exportGs1Dossier(shipmentId, "json", false, userDetails, "127.0.0.1");

        assertThat(response.getMapping()).isNull();
        assertThat(response.getEvents()).hasSize(1);
    }

    @Test
    void export_shipmentNotFound_throws() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dossierService.exportGs1Dossier(shipmentId, "json", true, userDetails, "127.0.0.1"))
                .isInstanceOf(vn.nguongocso.exception.ResourceNotFoundException.class);
    }

    @Test
    void export_wrongRole_throwsAccessDenied() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(userDetails.getRoleCode()).thenReturn("VT-05");

        assertThatThrownBy(() -> dossierService.exportGs1Dossier(shipmentId, "json", true, userDetails, "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private FarmLog farmLog(FarmActivityType type) {
        return FarmLog.builder().id(UUID.randomUUID()).activityType(type).executedDate(LocalDate.now()).build();
    }
}