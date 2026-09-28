package vn.nguongocso.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.ai.dto.query.CertificationStatusDto;
import vn.nguongocso.ai.dto.query.OrganizationAnalyticsDataDto;
import vn.nguongocso.ai.dto.query.ProductionLotSummaryDto;
import vn.nguongocso.ai.dto.query.RecentAlertsSummaryDto;
import vn.nguongocso.ai.dto.query.ShipmentSummaryDto;
import vn.nguongocso.ai.service.impl.AiDataQueryServiceImpl;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.trace.recall.repository.RecallCaseRepository;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Kiểm thử đơn vị cho dịch vụ trích xuất số liệu nghiệp vụ AiDataQueryService (TASK-AI-05).
 */
@ExtendWith(MockitoExtension.class)
class AiDataQueryServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private FarmAreaRepository farmAreaRepository;

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private RecallCaseRepository recallCaseRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentHandoverRepository shipmentHandoverRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private AiDataQueryServiceImpl aiDataQueryService;

    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
    }

    @Test
    @DisplayName("TASK-AI-05: Tổng hợp số lượng lô sản xuất và diện tích canh tác")
    void testGetProductionLotSummary_Success() {
        Object[] aggregateRow = new Object[]{12L, 5L, 45.5};
        when(productionLotRepository.getLotAggregateSummaryByOrgId(orgId))
                .thenReturn(List.<Object[]>of(aggregateRow));
        when(productionLotRepository.findUpcomingHarvestLotNames(eq(orgId), any(), any()))
                .thenReturn(List.of("Lô Xoài Cát Hòa Lộc (LOT-002)"));

        long startTime = System.currentTimeMillis();
        ProductionLotSummaryDto summary = aiDataQueryService.getProductionLotSummary(orgId);
        long executionTime = System.currentTimeMillis() - startTime;

        assertNotNull(summary);
        assertEquals(12, summary.getActiveLotsCount());
        assertEquals(5, summary.getHarvestedLotsCount());
        assertEquals(45.5, summary.getTotalAreaHectares());
        assertEquals(1, summary.getUpcomingHarvestLotNames().size());
        assertEquals("Lô Xoài Cát Hòa Lộc (LOT-002)", summary.getUpcomingHarvestLotNames().get(0));
        assertTrue(executionTime < 300, "Thời gian thực thi phải nhỏ hơn 300ms (AC đạt tiêu chuẩn)");
    }

    @Test
    @DisplayName("TASK-AI-05: Lấy danh sách chứng nhận sắp hết hiệu lực trong 30 ngày")
    void testGetCertificationStatus_ExpiringWithin30Days() {
        Standard standard = Standard.builder().name("VietGAP").build();
        Certification cert = Certification.builder()
                .code("VG-2024-001")
                .name("Chứng nhận VietGAP Xoài")
                .standard(standard)
                .expiryDate(LocalDate.now().plusDays(15))
                .build();

        when(certificationRepository.findExpiringCertifications(eq(orgId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(cert));

        List<CertificationStatusDto> certs = aiDataQueryService.getCertificationStatus(orgId);

        assertNotNull(certs);
        assertEquals(1, certs.size());
        assertEquals("VG-2024-001", certs.get(0).getCode());
        assertEquals("VietGAP", certs.get(0).getStandardName());
        assertEquals(15, certs.get(0).getDaysRemaining());
    }

    @Test
    @DisplayName("TASK-AI-05: Tổng hợp cảnh báo quét bất thường và vụ việc thu hồi")
    void testGetRecentAlertsSummary_Success() {
        when(alertRepository.countByOrganizationOrganizationIdAndTypeAndStatus(
                orgId, AlertType.SCAN_ANOMALY, AlertStatus.PENDING)).thenReturn(2L);
        when(recallCaseRepository.countByOrganizationIdAndStatus(
                orgId, RecallCaseStatus.OPEN)).thenReturn(0L);

        RecentAlertsSummaryDto alerts = aiDataQueryService.getRecentAlertsSummary(orgId);

        assertNotNull(alerts);
        assertEquals(2, alerts.getPendingScanAnomalyCount());
        assertEquals(0, alerts.getActiveRecallCasesCount());
    }

    @Test
    @DisplayName("TASK-AI-05: Tổng hợp tình trạng lô hàng đang lưu thông và bàn giao")
    void testGetShipmentSummary_Success() {
        when(shipmentRepository.countByOrganization_OrganizationIdAndStatus(
                orgId, ShipmentStatus.ACTIVATED)).thenReturn(3L);
        when(shipmentHandoverRepository.countByFromOrganizationOrganizationIdAndStatus(
                orgId, ShipmentHandoverStatus.PENDING_CONFIRMATION)).thenReturn(1L);

        ShipmentSummaryDto shipments = aiDataQueryService.getShipmentSummary(orgId);

        assertNotNull(shipments);
        assertEquals(3, shipments.getInTransitShipmentsCount());
        assertEquals(1, shipments.getPendingHandoverCount());
    }

    @Test
    @DisplayName("TASK-AI-05: Trích xuất toàn bộ dữ liệu thống kê của tổ chức")
    void testGetFullOrganizationAnalytics_Success() {
        Organization org = Organization.builder()
                .organizationId(orgId)
                .name("HTX Nông Nghiệp Xanh")
                .code("HTX-XANH")
                .build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        Object[] aggregateRow = new Object[]{8L, 3L, 20.0};
        when(productionLotRepository.getLotAggregateSummaryByOrgId(orgId))
                .thenReturn(List.<Object[]>of(aggregateRow));
        when(alertRepository.countByOrganizationOrganizationIdAndTypeAndStatus(any(), any(), any()))
                .thenReturn(1L);
        when(recallCaseRepository.countByOrganizationIdAndStatus(any(), any()))
                .thenReturn(0L);
        when(shipmentRepository.countByOrganization_OrganizationIdAndStatus(any(), any()))
                .thenReturn(2L);
        when(shipmentHandoverRepository.countByFromOrganizationOrganizationIdAndStatus(any(), any()))
                .thenReturn(0L);

        OrganizationAnalyticsDataDto analytics = aiDataQueryService.getFullOrganizationAnalytics(orgId);

        assertNotNull(analytics);
        assertEquals("HTX Nông Nghiệp Xanh", analytics.getOrganizationName());
        assertEquals("HTX-XANH", analytics.getOrganizationCode());
        assertEquals(8, analytics.getLotSummary().getActiveLotsCount());
        assertEquals(1, analytics.getAlertsSummary().getPendingScanAnomalyCount());
    }

    @Test
    @DisplayName("TASK-AI-07: Tổng hợp số liệu theo địa bàn cho Cán bộ Quản lý ngành VT-05")
    void testGetTerritoryAnalytics_Success() {
        Set<UUID> orgIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Object[] aggregateRow = new Object[]{25L, 10L, 120.0};
        when(productionLotRepository.getLotAggregateSummaryByOrgIds(orgIds))
                .thenReturn(List.<Object[]>of(aggregateRow));
        when(alertRepository.countByOrganizationOrganizationIdInAndTypeAndStatus(
                eq(orgIds), eq(AlertType.SCAN_ANOMALY), eq(AlertStatus.PENDING))).thenReturn(5L);

        OrganizationAnalyticsDataDto territoryData = aiDataQueryService.getTerritoryAnalytics(
                orgIds, "Tỉnh Đồng Tháp");

        assertNotNull(territoryData);
        assertEquals("Tỉnh Đồng Tháp", territoryData.getOrganizationName());
        assertEquals(25, territoryData.getLotSummary().getActiveLotsCount());
        assertEquals(120.0, territoryData.getLotSummary().getTotalAreaHectares());
        assertEquals(5, territoryData.getAlertsSummary().getPendingScanAnomalyCount());
    }
}
