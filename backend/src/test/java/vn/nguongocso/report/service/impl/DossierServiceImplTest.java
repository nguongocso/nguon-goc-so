package vn.nguongocso.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.report.dto.response.GS1DossierExportResponse;
import vn.nguongocso.report.dto.response.GS1Event;
import vn.nguongocso.report.dto.response.Warning;
import vn.nguongocso.report.entity.DossierExportHistory;
import vn.nguongocso.report.exception.DossierValidationException;
import vn.nguongocso.report.repository.DossierExportHistoryRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Focused unit tests for GS1 dossier export (NCL-12-CN-003).
 *
 * <p>Uses Mockito only — no database required.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DossierServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private FarmLogRepository farmLogRepository;
    @Mock
    private FarmLogAttachmentRepository farmLogAttachmentRepository;
    @Mock
    private ChainEventRepository chainEventRepository;
    @Mock
    private DossierExportHistoryRepository exportHistoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganizationUserRepository organizationUserRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DossierServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    private UUID shipmentId;
    private UUID orgId;
    private Organization organization;
    private ProductionLot productionLot;
    private Shipment shipment;
    private CustomUserDetails vt02User;
    private CustomUserDetails vt06User;
    private User actor;

    @BeforeEach
    void setUp() {
        service = new DossierServiceImpl(
                shipmentRepository,
                farmLogRepository,
                farmLogAttachmentRepository,
                chainEventRepository,
                exportHistoryRepository,
                userRepository,
                organizationUserRepository,
                eventPublisher,
                objectMapper);

        shipmentId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        organization = Organization.builder()
                .organizationId(orgId)
                .name("HTX Chè Tân Cương")
                .code("HTX-TC")
                .build();

        ProductCategory category = ProductCategory.builder().name("Chè").build();
        productionLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Lô chè Tân Cương T8/2026")
                .status(ProductionLotStatus.PACKAGED)
                .productCategory(category)
                .expectedQuantity(500.0)
                .expectedQuantityUnit("kg")
                .organization(organization)
                .build();

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô chè xuất Trung");
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setTotalQuantity(500L);
        shipment.setProductionLot(productionLot);
        shipment.setOrganization(organization);

        actor = User.builder()
                .userId(UUID.randomUUID())
                .userName("actor")
                .fullName("Nguyễn Văn A")
                .build();

        vt02User = mock(CustomUserDetails.class);
        when(vt02User.getRoleCode()).thenReturn("VT-02");
        when(vt02User.getOrganizationId()).thenReturn(orgId);
        when(vt02User.getFullName()).thenReturn("Người Xuất");
        when(vt02User.getUsername()).thenReturn("exporter");
        when(vt02User.getUserId()).thenReturn(actor.getUserId());

        vt06User = mock(CustomUserDetails.class);
        when(vt06User.getRoleCode()).thenReturn("VT-06");
    }

    private void mockEligible() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        List<FarmLog> logs = new ArrayList<>();
        for (FarmActivityType type : List.of(FarmActivityType.PLANTING, FarmActivityType.FERTILIZING,
                FarmActivityType.PESTICIDE, FarmActivityType.HARVESTING)) {
            FarmLog log = FarmLog.builder()
                    .id(UUID.randomUUID())
                    .activityType(type)
                    .productionLotId(productionLot)
                    .build();
            logs.add(log);
            when(farmLogAttachmentRepository.findByFarmLogId(log.getId()))
                    .thenReturn(List.of(FarmLogAttachment.builder().id(UUID.randomUUID()).build()));
        }
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(productionLot.getId()))
                .thenReturn(logs);
    }

    private ChainEvent buildEvent(ChainEventType type, LocalDateTime recordedAt, Point location) {
        return ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(type)
                .eventData("{\"productionLotName\":\"Lô chè Tân Cương T8/2026\"}")
                .location(location)
                .recordedAt(recordedAt)
                .recordedBy(actor)
                .shipment(shipment)
                .isCorrection(false)
                .build();
    }

    // TC-01: multiple events, order ASC, four-dimension mapping
    @Test
    void exportGs1Dossier_withMultipleEvents_returnsOrderedEvents() {
        mockEligible();

        ChainEvent harvest = buildEvent(ChainEventType.HARVEST,
                LocalDateTime.of(2026, 8, 11, 10, 0),
                geometryFactory.createPoint(new Coordinate(105.8542, 21.0285)));
        ChainEvent transport = buildEvent(ChainEventType.TRANSPORT,
                LocalDateTime.of(2026, 8, 11, 11, 0), null);
        ChainEvent packaging = buildEvent(ChainEventType.PACKAGING,
                LocalDateTime.of(2026, 8, 11, 10, 30),
                geometryFactory.createPoint(new Coordinate(105.8542, 21.0285)));

        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId))
                .thenReturn(new ArrayList<>(List.of(transport, harvest, packaging)));

        GS1DossierExportResponse response = service.exportGs1Dossier(shipmentId, vt02User, true);

        assertEquals(shipmentId, response.getShipment().getId());
        assertEquals("Lô chè xuất Trung", response.getShipment().getName());
        assertEquals("Chè", response.getShipment().getProductCategory());
        assertEquals("kg", response.getShipment().getUnit());
        assertEquals("ACTIVATED", response.getShipment().getStatus());
        assertEquals(orgId, response.getShipment().getOrganization().getId());

        // Order: harvest (10:00), packaging (10:30), transport (11:00)
        List<GS1Event> events = response.getEvents();
        assertEquals(3, events.size());
        assertEquals(ChainEventType.HARVEST.name(), events.get(0).getEventType());
        assertEquals("Thu hoạch", events.get(0).getEventTypeLabel());
        assertEquals(ChainEventType.PACKAGING.name(), events.get(1).getEventType());
        assertEquals(ChainEventType.TRANSPORT.name(), events.get(2).getEventType());

        // Four dimensions
        GS1Event harvestEvent = events.get(0);
        assertEquals(harvest.getId(), harvestEvent.getEventId());
        assertEquals(LocalDateTime.of(2026, 8, 11, 10, 0), harvestEvent.getRecordedAt()); // when
        assertEquals("Nguyễn Văn A", harvestEvent.getRecordedBy()); // who
        assertEquals(21.0285, harvestEvent.getLocation().getLatitude()); // where
        assertEquals(105.8542, harvestEvent.getLocation().getLongitude());
        assertNull(harvestEvent.getLocation().getAddress()); // address not in domain
        assertEquals("Lô chè Tân Cương T8/2026", harvestEvent.getDetails().get("productionLotName")); // why

        // transport has no location -> null + warning
        GS1Event transportEvent = events.get(2);
        assertNull(transportEvent.getLocation());
        assertEquals(1, response.getWarnings().size());
        Warning warning = response.getWarnings().get(0);
        assertEquals(transport.getId(), warning.getEventId());
        assertEquals("location", warning.getField());

        // Mapping present by default
        assertNotNull(response.getMapping());
        assertEquals("eventTypeCode", response.getMapping().get("ChainEvent.eventType"));
        assertEquals("shipmentName", response.getMapping().get("Shipment.name"));

        // export metadata
        assertNotNull(response.getExportedAt());
        assertEquals("Người Xuất", response.getExportedBy());
        assertEquals("1.0.0", response.getSchemaVersion());

        // ActivityLog published
        ArgumentCaptor<ActivityLogEvent> captor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertEquals("GS1_DOSSIER_EXPORT", captor.getValue().getAction());
        assertEquals("Shipment", captor.getValue().getEntityType());
        assertEquals(shipmentId.toString(), captor.getValue().getEntityId());
        assertEquals(actor.getUserId(), captor.getValue().getUserId());
        assertEquals(orgId, captor.getValue().getOrganizationId());
    }

    // includeMapping=false
    @Test
    void exportGs1Dossier_includeMappingFalse_mappingIsNull() {
        mockEligible();
        ChainEvent harvest = buildEvent(ChainEventType.HARVEST,
                LocalDateTime.of(2026, 8, 11, 10, 0),
                geometryFactory.createPoint(new Coordinate(105.8542, 21.0285)));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId))
                .thenReturn(new ArrayList<>(List.of(harvest)));

        GS1DossierExportResponse response = service.exportGs1Dossier(shipmentId, vt02User, false);

        assertNull(response.getMapping());
        assertEquals(1, response.getEvents().size());
    }

    // TC-04: shipment without events -> 400
    @Test
    void exportGs1Dossier_noEvents_throwsBusinessException() {
        mockEligible();
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId))
                .thenReturn(new ArrayList<>());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.exportGs1Dossier(shipmentId, vt02User, true));
        assertEquals("Lô chưa có sự kiện nào để xuất hồ sơ.", ex.getMessage());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // TC-05: QTN-11 failure -> 400 with missing conditions
    @Test
    void checkEligibility_missingFarmingDocs_throwsDossierValidation() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        // Lot not PACKAGED/CLOSED + no farm logs
        productionLot.setStatus(ProductionLotStatus.DRAFT);
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(productionLot.getId()))
                .thenReturn(new ArrayList<>());

        DossierValidationException ex = assertThrows(DossierValidationException.class,
                () -> service.checkEligibility(shipmentId, vt02User));
        assertTrue(ex.getErrors().stream().anyMatch(m -> m.contains("chưa hoàn tất")));
        assertTrue(ex.getErrors().stream().anyMatch(m -> m.contains("PLANTING")));
        assertTrue(ex.getErrors().stream().anyMatch(m -> m.contains("FERTILIZING")));
        assertTrue(ex.getErrors().stream().anyMatch(m -> m.contains("PESTICIDE")));
        assertTrue(ex.getErrors().stream().anyMatch(m -> m.contains("HARVESTING")));
    }

    // Shipment not found -> 404
    @Test
    void exportGs1Dossier_shipmentNotFound_throwsResourceNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.exportGs1Dossier(shipmentId, vt02User, true));
    }

    // TC-03 / organization access violation -> 403
    @Test
    void exportGs1Dossier_vt02DifferentOrg_throwsAccessDenied() {
        CustomUserDetails otherOrgUser = mock(CustomUserDetails.class);
        when(otherOrgUser.getRoleCode()).thenReturn("VT-02");
        when(otherOrgUser.getOrganizationId()).thenReturn(UUID.randomUUID());

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThrows(AccessDeniedException.class,
                () -> service.exportGs1Dossier(shipmentId, otherOrgUser, true));
    }

    // TC-03: VT-06 -> 403 (not VT-02/VT-04)
    @Test
    void exportGs1Dossier_otherRole_throwsAccessDenied() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThrows(AccessDeniedException.class,
                () -> service.exportGs1Dossier(shipmentId, vt06User, true));
    }

    // TC-06: XML format
    @Test
    void exportGs1DossierXml_returnsWellFormedXml() {
        mockEligible();
        ChainEvent harvest = buildEvent(ChainEventType.HARVEST,
                LocalDateTime.of(2026, 8, 11, 10, 0),
                geometryFactory.createPoint(new Coordinate(105.8542, 21.0285)));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId))
                .thenReturn(new ArrayList<>(List.of(harvest)));

        String xml = service.exportGs1DossierXml(shipmentId, vt02User, true);

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<gs1Dossier>"));
        assertTrue(xml.contains("<shipment>"));
        assertTrue(xml.contains("<name>Lô chè xuất Trung</name>"));
        assertTrue(xml.contains("<eventType>HARVEST</eventType>"));
        assertTrue(xml.contains("<recordedBy>Nguyễn Văn A</recordedBy>"));
        assertTrue(xml.contains("<latitude>21.0285</latitude>"));
        assertTrue(xml.contains("<longitude>105.8542</longitude>"));
        assertTrue(xml.contains("<schemaVersion>1.0.0</schemaVersion>"));
        assertTrue(xml.contains("<mapping>"));
        assertTrue(xml.contains("<systemField>ChainEvent.eventType</systemField>"));
        assertTrue(xml.contains("<gs1Field>eventTypeCode</gs1Field>"));
        assertTrue(xml.contains("</gs1Dossier>"));
    }

    // TC-07: missing location and address -> location null + warning, export succeeds
    @Test
    void exportGs1Dossier_eventWithoutLocation_exportsWithWarning() {
        mockEligible();
        ChainEvent transport = buildEvent(ChainEventType.TRANSPORT,
                LocalDateTime.of(2026, 8, 11, 11, 0), null);
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipmentId))
                .thenReturn(new ArrayList<>(List.of(transport)));

        GS1DossierExportResponse response = service.exportGs1Dossier(shipmentId, vt02User, true);

        assertEquals(1, response.getEvents().size());
        assertNull(response.getEvents().get(0).getLocation());
        assertEquals(1, response.getWarnings().size());
        assertEquals("location", response.getWarnings().get(0).getField());
        assertEquals(transport.getId(), response.getWarnings().get(0).getEventId());
    }
}