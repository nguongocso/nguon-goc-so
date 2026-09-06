package vn.nguongocso.event.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.response.ScanLookupResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.impl.ChainEventServiceImpl;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * Kiểm tra cờ storageEligible của scanLookup theo vai trò
 * (đúng luật tra cứu tay, backend chốt để QR và tay cùng kết quả):
 * VT-03 cùng tổ chức cần có TRANSPORT; VT-04 cần đã thu mua lô.
 */
@ExtendWith(MockitoExtension.class)
class ChainEventScanLookupTest {

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @InjectMocks
    private ChainEventServiceImpl chainEventService;

    private CustomUserDetails vt04User;
    private CustomUserDetails vt03User;
    private CustomUserDetails vt02User;
    private Organization shipmentOrg;
    private UUID vt04OrgId;
    private Shipment shipment;
    private TraceCode traceCode;
    private User procurementRecorder;
    private UUID procurementRecorderId;

    @BeforeEach
    void setUp() {
        vt04OrgId = UUID.randomUUID();

        shipmentOrg = new Organization();
        shipmentOrg.setOrganizationId(UUID.randomUUID());
        shipmentOrg.setName("HTX Demo");

        ProductionLot productionLot = ProductionLot.builder()
                .id(UUID.randomUUID())
                .name("Luu test")
                .organization(shipmentOrg)
                .status(ProductionLotStatus.APPROVED)
                .build();

        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setOrganization(shipmentOrg);
        shipment.setProductionLot(productionLot);
        shipment.setName("Luu hang test");
        shipment.setTotalQuantity(500L);
        shipment.setStatus(ShipmentStatus.ACTIVATED);

        traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setCodeValue("NKS-TEST-001");
        traceCode.setShipment(shipment);

        procurementRecorderId = UUID.randomUUID();
        procurementRecorder = new User();
        procurementRecorder.setUserId(procurementRecorderId);
        procurementRecorder.setFullName("Nhan vien thu mua");

        vt04User = mock(CustomUserDetails.class);
        lenient().when(vt04User.getRoleCode()).thenReturn("VT-04");
        lenient().when(vt04User.getOrganizationId()).thenReturn(vt04OrgId);

        vt03User = mock(CustomUserDetails.class);
        lenient().when(vt03User.getRoleCode()).thenReturn("VT-03");
        lenient().when(vt03User.getOrganizationId()).thenReturn(shipmentOrg.getOrganizationId());

        vt02User = mock(CustomUserDetails.class);
        lenient().when(vt02User.getRoleCode()).thenReturn("VT-02");
        lenient().when(vt02User.getOrganizationId()).thenReturn(shipmentOrg.getOrganizationId());

        when(traceCodeRepository.findByCodeValue("NKS-TEST-001"))
                .thenReturn(Optional.of(traceCode));
        when(chainEventRepository.findTopByShipmentIdOrderByRecordedAtDesc(shipment.getId()))
                .thenReturn(Optional.empty());
    }

    private ChainEvent event(ChainEventType type, User recorder) {
        return ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(type)
                .eventData("{}")
                .recordedAt(LocalDateTime.now())
                .recordedBy(recorder)
                .isCorrection(false)
                .build();
    }

    @Test
    void vt03_sameOrgWithTransport_eligible() {
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of(event(ChainEventType.TRANSPORT, procurementRecorder)));

        ScanLookupResponse res = chainEventService.scanLookup("NKS-TEST-001", vt03User);

        assertThat(res.getValid()).isTrue();
        assertThat(res.getStorageEligible()).isTrue();
    }

    @Test
    void vt03_sameOrgWithoutTransport_notEligible() {
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of());

        ScanLookupResponse res = chainEventService.scanLookup("NKS-TEST-001", vt03User);

        assertThat(res.getValid()).isTrue();
        assertThat(res.getStorageEligible()).isFalse();
    }

    @Test
    void vt04_ownProcurement_eligible() {
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of(event(ChainEventType.PROCUREMENT, procurementRecorder)));
        when(organizationUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(vt04OrgId, procurementRecorderId))
                .thenReturn(Optional.of(mock(OrganizationUser.class)));

        ScanLookupResponse res = chainEventService.scanLookup("NKS-TEST-001", vt04User);

        assertThat(res.getValid()).isTrue();
        assertThat(res.getStorageEligible()).isTrue();
    }

    @Test
    void vt04_noProcurement_notEligible() {
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of(event(ChainEventType.TRANSPORT, procurementRecorder)));

        ScanLookupResponse res = chainEventService.scanLookup("NKS-TEST-001", vt04User);

        assertThat(res.getStorageEligible()).isFalse();
    }

    @Test
    void vt04_procurementByOtherOrg_notEligible() {
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(List.of(event(ChainEventType.PROCUREMENT, procurementRecorder)));
        when(organizationUserRepository
                .findByOrganization_OrganizationIdAndUser_UserId(vt04OrgId, procurementRecorderId))
                .thenReturn(Optional.empty());

        ScanLookupResponse res = chainEventService.scanLookup("NKS-TEST-001", vt04User);

        assertThat(res.getStorageEligible()).isFalse();
    }

    @Test
    void otherRole_storageEligibleIsNull() {
        ScanLookupResponse res = chainEventService.scanLookup("NKS-TEST-001", vt02User);

        assertThat(res.getValid()).isTrue();
        assertThat(res.getStorageEligible()).isNull();
    }
}
