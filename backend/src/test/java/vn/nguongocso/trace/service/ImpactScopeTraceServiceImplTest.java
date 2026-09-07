package vn.nguongocso.trace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.dto.response.ImpactScopeTraceResponse;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.ImpactScopeTraceServiceImpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImpactScopeTraceServiceImplTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private TraceCodeScanLogRepository traceCodeScanLogRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @InjectMocks
    private ImpactScopeTraceServiceImpl impactScopeTraceService;

    private UUID orgId;
    private Organization organization;
    private FarmArea farmArea;
    private ProductionLot productionLot;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("Hợp tác xã Nông nghiệp Đà Lạt");

        farmArea = new FarmArea();
        farmArea.setId(UUID.randomUUID());
        farmArea.setName("Vùng trồng Dâu tây Khu A");

        productionLot = new ProductionLot();
        productionLot.setId(UUID.randomUUID());
        productionLot.setName("LOT-2026-001");
        productionLot.setStatus(ProductionLotStatus.APPROVED);
        productionLot.setExpectedQuantity(5000.0);
        productionLot.setExpectedQuantityUnit("kg");
        productionLot.setOrganization(organization);
        productionLot.setFarmArea(farmArea);

        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getOrganizationId()).thenReturn(orgId);
        when(userDetails.getRoleCode()).thenReturn("VT-02");
    }

    @Test
    @DisplayName("TC-01: Truy vết từ Mã Lô sản xuất sinh 3 lô hàng và 2 tổ chức đã nhận")
    void testTC01_SuccessFlow_ProductionLotWithThreeShipments() {
        // Given
        String lotCode = "LOT-2026-001";
        when(productionLotRepository.findAll()).thenReturn(List.of(productionLot));

        Shipment ship1 = createShipment(productionLot, organization, "SHIP-8821", ShipmentStatus.ACTIVATED, 1500L);
        Shipment ship2 = createShipment(productionLot, organization, "SHIP-8822", ShipmentStatus.ACTIVATED, 2000L);
        Shipment ship3 = createShipment(productionLot, organization, "SHIP-8823", ShipmentStatus.DRAFT, 1350L);

        when(shipmentRepository.findByProductionLotId(productionLot.getId()))
                .thenReturn(List.of(ship1, ship2, ship3));

        TraceCode tc1 = new TraceCode();
        tc1.setStatus(TraceCodeStatus.ACTIVE);
        when(traceCodeRepository.findByShipmentId(ship1.getId())).thenReturn(List.of(tc1));
        when(traceCodeRepository.findByShipmentId(ship2.getId())).thenReturn(List.of(tc1));
        when(traceCodeRepository.findByShipmentId(ship3.getId())).thenReturn(Collections.emptyList());

        Organization buyerOrg1 = new Organization();
        buyerOrg1.setOrganizationId(UUID.randomUUID());
        buyerOrg1.setName("Siêu thị Co.opmart");

        Organization buyerOrg2 = new Organization();
        buyerOrg2.setOrganizationId(UUID.randomUUID());
        buyerOrg2.setName("Công ty WinCommerce");

        User buyerUser1 = new User();
        buyerUser1.setUserId(UUID.randomUUID());

        User buyerUser2 = new User();
        buyerUser2.setUserId(UUID.randomUUID());

        OrganizationUser ou1 = new OrganizationUser();
        ou1.setOrganization(buyerOrg1);
        OrganizationUser ou2 = new OrganizationUser();
        ou2.setOrganization(buyerOrg2);

        when(organizationUserRepository.findFirstByUser(buyerUser1)).thenReturn(Optional.of(ou1));
        when(organizationUserRepository.findFirstByUser(buyerUser2)).thenReturn(Optional.of(ou2));

        ChainEvent event1 = ChainEvent.builder()
                .shipment(ship1)
                .eventType(ChainEventType.PROCUREMENT)
                .recordedBy(buyerUser1)
                .recordedAt(LocalDateTime.now().minusDays(2))
                .build();

        ChainEvent event2 = ChainEvent.builder()
                .shipment(ship2)
                .eventType(ChainEventType.WAREHOUSE_RECEIPT)
                .recordedBy(buyerUser2)
                .recordedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(ship1.getId())).thenReturn(List.of(event1));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(ship2.getId())).thenReturn(List.of(event2));
        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(ship3.getId())).thenReturn(Collections.emptyList());

        // When
        ImpactScopeTraceResponse response = impactScopeTraceService.getImpactScopeTrace(lotCode, userDetails);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRootNodeType()).isEqualTo("PRODUCTION_LOT");
        assertThat(response.getProductionLot().getName()).isEqualTo("LOT-2026-001");
        assertThat(response.getFarmArea().getName()).isEqualTo("Vùng trồng Dâu tây Khu A");
        assertThat(response.getShipments()).hasSize(3);
        assertThat(response.getSummary().getTotalShipments()).isEqualTo(3);
        assertThat(response.getSummary().getTotalReceivingOrganizations()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-02: Truy vết từ Mã Tem QR đóng vai trò hạt nhân")
    void testTC02_SuccessFlow_TraceFromStampCode() {
        // Given
        String stampCode = "NCL0001";

        Shipment shipment = createShipment(productionLot, organization, "SHIP-8821", ShipmentStatus.ACTIVATED, 1500L);

        TraceCode traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setCodeValue(stampCode);
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        traceCode.setShipment(shipment);

        when(traceCodeRepository.findByCodeValue(stampCode)).thenReturn(Optional.of(traceCode));
        when(shipmentRepository.findByProductionLotId(productionLot.getId())).thenReturn(List.of(shipment));

        // When
        ImpactScopeTraceResponse response = impactScopeTraceService.getImpactScopeTrace(stampCode, userDetails);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRootNodeType()).isEqualTo("TRACE_CODE");
        assertThat(response.getProductionLot().getId()).isEqualTo(productionLot.getId());
        assertThat(response.getFarmArea().getName()).isEqualTo("Vùng trồng Dâu tây Khu A");
    }

    @Test
    @DisplayName("TC-03: Kịch bản Dữ liệu rỗng - Lô sản xuất chưa sinh lô hàng nào")
    void testTC03_EmptyShipments_ReturnsEmptyBranchWithMessage() {
        // Given
        String lotCode = "LOT-2026-001";
        when(productionLotRepository.findAll()).thenReturn(List.of(productionLot));
        when(shipmentRepository.findByProductionLotId(productionLot.getId())).thenReturn(Collections.emptyList());

        // When
        ImpactScopeTraceResponse response = impactScopeTraceService.getImpactScopeTrace(lotCode, userDetails);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getShipments()).isEmpty();
        assertThat(response.getSummary().getTotalShipments()).isEqualTo(0);
        assertThat(response.getSummary().getTotalActivatedStamps()).isEqualTo(0);
        assertThat(response.getSummary().getTotalReceivingOrganizations()).isEqualTo(0);
    }

    @Test
    @DisplayName("TC-04 & QTN-01: Ranh giới bảo mật - Chỉ hiển thị Tên tổ chức nhận và Thời điểm")
    void testTC04_DataIsolation_OnlyExposesReceivingOrgNameAndTimestamp() {
        // Given
        String lotCode = "LOT-2026-001";
        when(productionLotRepository.findAll()).thenReturn(List.of(productionLot));

        Shipment ship1 = createShipment(productionLot, organization, "SHIP-8821", ShipmentStatus.ACTIVATED, 1500L);
        when(shipmentRepository.findByProductionLotId(productionLot.getId())).thenReturn(List.of(ship1));

        Organization buyerOrg = new Organization();
        buyerOrg.setOrganizationId(UUID.randomUUID());
        buyerOrg.setName("Bên Mua Siêu Thị");

        User buyerUser = new User();
        buyerUser.setUserId(UUID.randomUUID());

        OrganizationUser ou = new OrganizationUser();
        ou.setOrganization(buyerOrg);

        when(organizationUserRepository.findFirstByUser(buyerUser)).thenReturn(Optional.of(ou));

        LocalDateTime now = LocalDateTime.now();
        ChainEvent procurementEvent = ChainEvent.builder()
                .shipment(ship1)
                .eventType(ChainEventType.PROCUREMENT)
                .eventData("{\"internalFinancialData\": \"CONFIDENTIAL_PRICE_100M\"}")
                .recordedBy(buyerUser)
                .recordedAt(now)
                .build();

        when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(ship1.getId())).thenReturn(List.of(procurementEvent));

        // When
        ImpactScopeTraceResponse response = impactScopeTraceService.getImpactScopeTrace(lotCode, userDetails);

        // Then
        assertThat(response.getShipments()).hasSize(1);
        var receivingOrgs = response.getShipments().get(0).getReceivingOrganizations();
        assertThat(receivingOrgs).hasSize(1);
        assertThat(receivingOrgs.get(0).getOrganizationName()).isEqualTo("Bên Mua Siêu Thị");
        assertThat(receivingOrgs.get(0).getReceivedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("TC-05: Kịch bản Dữ liệu không hợp lệ - Nhập mã không tồn tại")
    void testTC05_InvalidCode_ThrowsNotFoundException() {
        // Given
        String invalidCode = "INVALID-CODE-9999";
        when(traceCodeRepository.findByCodeValue(invalidCode)).thenReturn(Optional.empty());
        when(productionLotRepository.findAll()).thenReturn(Collections.emptyList());
        when(shipmentRepository.findEligibleShipments(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

        // When / Then
        assertThatThrownBy(() -> impactScopeTraceService.getImpactScopeTrace(invalidCode, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mã truy vết không tồn tại trên hệ thống");
    }

    private Shipment createShipment(ProductionLot lot, Organization org, String name, ShipmentStatus status, Long qty) {
        Shipment s = new Shipment();
        s.setId(UUID.randomUUID());
        s.setProductionLot(lot);
        s.setOrganization(org);
        s.setName(name);
        s.setStatus(status);
        s.setTotalQuantity(qty);
        s.setCreatedAt(LocalDateTime.now());
        return s;
    }
}
