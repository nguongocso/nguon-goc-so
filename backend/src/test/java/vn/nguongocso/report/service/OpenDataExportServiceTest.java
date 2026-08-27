package vn.nguongocso.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.service.AreaScopeResult;
import vn.nguongocso.organization.service.AreaScopeService;
import vn.nguongocso.report.service.impl.OpenDataExportServiceImpl;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests cho OpenDataExportService.
 */
@ExtendWith(MockitoExtension.class)
public class OpenDataExportServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private FarmLogAttachmentRepository farmLogAttachmentRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private ReportAccessLogService reportAccessLogService;

    @Mock
    private AreaScopeService areaScopeService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OpenDataExportServiceImpl openDataExportService;

    private CustomUserDetails regulatorDetails;
    private CustomUserDetails nonRegulatorDetails;

    private UUID userId;
    private UUID orgId;
    private String ipAddress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        ipAddress = "192.168.1.10";

        regulatorDetails = mock(CustomUserDetails.class);
        lenient().when(regulatorDetails.getUserId()).thenReturn(userId);
        lenient().when(regulatorDetails.getOrganizationId()).thenReturn(orgId);
        lenient().when(regulatorDetails.getRoleCode()).thenReturn("VT-05");

        nonRegulatorDetails = mock(CustomUserDetails.class);
        lenient().when(nonRegulatorDetails.getUserId()).thenReturn(userId);
        lenient().when(nonRegulatorDetails.getOrganizationId()).thenReturn(orgId);
        lenient().when(nonRegulatorDetails.getRoleCode()).thenReturn("VT-03"); // Recorder

        // Mặc định cho phép xem toàn bộ tổ chức (giữ hành vi cũ cho test hiện có)
        lenient().when(areaScopeService.resolveOrganizationsForReports(any(), any()))
                .thenReturn(AreaScopeResult.all());
    }

    @Test
    void exportOpenData_shouldThrowAccessDenied_whenUserIsNotRegulator() {
        // When & Then
        assertThatThrownBy(() -> openDataExportService.exportOpenData(
                "Phú Thọ", null, LocalDate.now().minusMonths(1), LocalDate.now(), "JSON", nonRegulatorDetails, ipAddress
        )).isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Bạn không có quyền thực hiện chức năng này");

        verify(reportAccessLogService, times(1)).logAccess(
                eq(userId), eq(orgId), eq(orgId), eq("OPEN_DATA_EXPORT"), eq(false), eq(ipAddress)
        );
    }

    @Test
    void exportOpenData_shouldThrowBusinessException_whenNoOrganizationsFound() {
        // Given
        String region = "Phú Thọ";
        when(organizationRepository.findByAddressContainingIgnoreCase(region)).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> openDataExportService.exportOpenData(
                region, null, LocalDate.now().minusMonths(1), LocalDate.now(), "JSON", regulatorDetails, ipAddress
        )).isInstanceOf(BusinessException.class)
          .hasMessageContaining("Không có dữ liệu mở đủ điều kiện để xuất trong phạm vi đã chọn.");

        verify(reportAccessLogService, times(1)).logAccess(
                eq(userId), eq(orgId), eq(orgId), eq("OPEN_DATA_EXPORT"), eq(false), eq(ipAddress)
        );
    }

    @Test
    void exportOpenData_shouldThrowBusinessException_whenNoEligibleLotsFound() {
        // Given
        String region = "Phú Thọ";
        Organization org = Organization.builder().organizationId(orgId).name("HTX A").build();
        when(organizationRepository.findByAddressContainingIgnoreCase(region)).thenReturn(List.of(org));

        LocalDate from = LocalDate.now().minusMonths(1);
        LocalDate to = LocalDate.now();
        when(productionLotRepository.findEligibleLotsForExport(
                anyList(), eq(from), eq(to), anyList()
        )).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> openDataExportService.exportOpenData(
                region, null, from, to, "JSON", regulatorDetails, ipAddress
        )).isInstanceOf(BusinessException.class)
          .hasMessageContaining("Không có dữ liệu mở đủ điều kiện để xuất trong phạm vi đã chọn.");
    }

    @Test
    void exportOpenData_shouldThrowBusinessException_whenLotsExistButFailQTN11() {
        // Given
        String region = "Phú Thọ";
        Organization org = Organization.builder().organizationId(orgId).name("HTX A").build();
        when(organizationRepository.findByAddressContainingIgnoreCase(region)).thenReturn(List.of(org));

        LocalDate from = LocalDate.now().minusMonths(1);
        LocalDate to = LocalDate.now();

        UUID lotId = UUID.randomUUID();
        ProductionLot lot = ProductionLot.builder()
                .id(lotId)
                .name("Lot A")
                .organization(org)
                .status(ProductionLotStatus.PACKAGED)
                .build();
        when(productionLotRepository.findEligibleLotsForExport(
                anyList(), eq(from), eq(to), anyList()
        )).thenReturn(List.of(lot));

        // Logs exist but attachments are missing (fails QTN-11)
        FarmLog plantingLog = FarmLog.builder().id(UUID.randomUUID()).productionLotId(lot).activityType(FarmActivityType.PLANTING).build();
        when(farmLogRepository.findByProductionLotId_IdInOrderByExecutedDateAsc(anyList()))
                .thenReturn(List.of(plantingLog));
        when(farmLogAttachmentRepository.findByFarmLogIdIn(anyList()))
                .thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> openDataExportService.exportOpenData(
                region, null, from, to, "JSON", regulatorDetails, ipAddress
        )).isInstanceOf(BusinessException.class)
          .hasMessageContaining("Không có dữ liệu mở đủ điều kiện để xuất trong phạm vi đã chọn.");
    }

    @Test
    void exportOpenData_shouldSucceedInJSON_whenDataIsEligible() throws Exception {
        // Given
        String region = "Phú Thọ";
        Organization org = Organization.builder()
                .organizationId(orgId)
                .name("HTX A")
                .address("Phú Thọ")
                .build();
        when(organizationRepository.findByAddressContainingIgnoreCase(region)).thenReturn(List.of(org));

        LocalDate from = LocalDate.now().minusMonths(1);
        LocalDate to = LocalDate.now();

        UUID lotId = UUID.randomUUID();
        ProductCategory category = ProductCategory.builder().id(UUID.randomUUID()).name("Chè").build();
        Point locationPoint = mock(Point.class);
        when(locationPoint.getX()).thenReturn(105.1);
        when(locationPoint.getY()).thenReturn(21.1);

        FarmArea farmArea = FarmArea.builder()
                .id(UUID.randomUUID())
                .name("Vùng 1")
                .area(BigDecimal.TEN)
                .location(locationPoint)
                .build();

        ProductionLot lot = ProductionLot.builder()
                .id(lotId)
                .name("Lot A")
                .organization(org)
                .productCategory(category)
                .farmArea(farmArea)
                .expectedQuantity(100.0)
                .expectedQuantityUnit("kg")
                .actualQuantity(95.0)
                .plantingDate(from)
                .harvestDate(to)
                .status(ProductionLotStatus.CLOSED)
                .build();
        when(productionLotRepository.findEligibleLotsForExport(
                anyList(), eq(from), eq(to), anyList()
        )).thenReturn(List.of(lot));

        // 4 Farm logs for QTN-11
        UUID log1 = UUID.randomUUID();
        UUID log2 = UUID.randomUUID();
        UUID log3 = UUID.randomUUID();
        UUID log4 = UUID.randomUUID();
        FarmLog l1 = FarmLog.builder().id(log1).productionLotId(lot).activityType(FarmActivityType.PLANTING).build();
        FarmLog l2 = FarmLog.builder().id(log2).productionLotId(lot).activityType(FarmActivityType.FERTILIZING).build();
        FarmLog l3 = FarmLog.builder().id(log3).productionLotId(lot).activityType(FarmActivityType.PESTICIDE).build();
        FarmLog l4 = FarmLog.builder().id(log4).productionLotId(lot).activityType(FarmActivityType.HARVESTING).build();

        when(farmLogRepository.findByProductionLotId_IdInOrderByExecutedDateAsc(anyList()))
                .thenReturn(List.of(l1, l2, l3, l4));

        FarmLogAttachment att = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l1).fileName("file.jpg").build();
        FarmLogAttachment att2 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l2).fileName("file2.jpg").build();
        FarmLogAttachment att3 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l3).fileName("file3.jpg").build();
        FarmLogAttachment att4 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l4).fileName("file4.jpg").build();

        when(farmLogAttachmentRepository.findByFarmLogIdIn(anyList()))
                .thenReturn(List.of(att, att2, att3, att4));

        // Shipment and events
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Shipment A");
        shipment.setTotalQuantity(50);
        shipment.setProductionLot(lot);
        shipment.setOrganization(org);
        shipment.setCreatedAt(LocalDateTime.now());

        when(shipmentRepository.findByProductionLotIdIn(anyList())).thenReturn(List.of(shipment));

        ChainEvent event = ChainEvent.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .eventType(ChainEventType.PACKAGING)
                .recordedAt(LocalDateTime.now())
                .build();

        when(chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(anyList())).thenReturn(List.of(event));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        com.fasterxml.jackson.databind.ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);

        // When
        byte[] result = openDataExportService.exportOpenData(region, null, from, to, "JSON", regulatorDetails,
                ipAddress);

        // Then
        assertThat(result).isNotNull();
        verify(reportAccessLogService, times(1)).logAccess(
                eq(userId), eq(orgId), eq(orgId), eq("OPEN_DATA_EXPORT"), eq(true), eq(ipAddress)
        );
    }

    @Test
    void exportOpenData_shouldSucceedInXML_whenDataIsEligible() throws Exception {
        // Given
        String region = "Phú Thọ";
        Organization org = Organization.builder()
                .organizationId(orgId)
                .name("HTX A")
                .address("Phú Thọ")
                .build();
        when(organizationRepository.findByAddressContainingIgnoreCase(region)).thenReturn(List.of(org));

        LocalDate from = LocalDate.now().minusMonths(1);
        LocalDate to = LocalDate.now();

        UUID lotId = UUID.randomUUID();
        ProductCategory category = ProductCategory.builder().id(UUID.randomUUID()).name("Chè").build();

        ProductionLot lot = ProductionLot.builder()
                .id(lotId)
                .name("Lot A")
                .organization(org)
                .productCategory(category)
                .expectedQuantity(100.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.CLOSED)
                .build();
        when(productionLotRepository.findEligibleLotsForExport(
                anyList(), eq(from), eq(to), anyList()
        )).thenReturn(List.of(lot));

        UUID log1 = UUID.randomUUID();
        UUID log2 = UUID.randomUUID();
        UUID log3 = UUID.randomUUID();
        UUID log4 = UUID.randomUUID();
        FarmLog l1 = FarmLog.builder().id(log1).productionLotId(lot).activityType(FarmActivityType.PLANTING).build();
        FarmLog l2 = FarmLog.builder().id(log2).productionLotId(lot).activityType(FarmActivityType.FERTILIZING).build();
        FarmLog l3 = FarmLog.builder().id(log3).productionLotId(lot).activityType(FarmActivityType.PESTICIDE).build();
        FarmLog l4 = FarmLog.builder().id(log4).productionLotId(lot).activityType(FarmActivityType.HARVESTING).build();

        when(farmLogRepository.findByProductionLotId_IdInOrderByExecutedDateAsc(anyList()))
                .thenReturn(List.of(l1, l2, l3, l4));

        FarmLogAttachment att = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l1).fileName("file.jpg").build();
        FarmLogAttachment att2 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l2).fileName("file2.jpg").build();
        FarmLogAttachment att3 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l3).fileName("file3.jpg").build();
        FarmLogAttachment att4 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l4).fileName("file4.jpg").build();

        when(farmLogAttachmentRepository.findByFarmLogIdIn(anyList()))
                .thenReturn(List.of(att, att2, att3, att4));

        when(shipmentRepository.findByProductionLotIdIn(anyList())).thenReturn(Collections.emptyList());

        // When
        byte[] result = openDataExportService.exportOpenData(region, null, from, to, "XML", regulatorDetails,
                ipAddress);

        // Then
        assertThat(result).isNotNull();
        String xmlContent = new String(result, StandardCharsets.UTF_8);
        assertThat(xmlContent).startsWith("<?xml");
        assertThat(xmlContent).contains("<OpenDataExport");
        assertThat(xmlContent).contains("<LotCode>Lot A</LotCode>");
    }

    @Test
    void exportOpenData_shouldSucceedInCSV_whenDataIsEligible() throws Exception {
        // Given
        String region = "Phú Thọ";
        Organization org = Organization.builder()
                .organizationId(orgId)
                .name("HTX A")
                .address("Phú Thọ")
                .build();
        when(organizationRepository.findByAddressContainingIgnoreCase(region)).thenReturn(List.of(org));

        LocalDate from = LocalDate.now().minusMonths(1);
        LocalDate to = LocalDate.now();

        UUID lotId = UUID.randomUUID();
        ProductCategory category = ProductCategory.builder().id(UUID.randomUUID()).name("Chè").build();

        ProductionLot lot = ProductionLot.builder()
                .id(lotId)
                .name("Lot A, \"Special\"")
                .organization(org)
                .productCategory(category)
                .expectedQuantity(100.0)
                .expectedQuantityUnit("kg")
                .status(ProductionLotStatus.CLOSED)
                .build();
        when(productionLotRepository.findEligibleLotsForExport(
                anyList(), eq(from), eq(to), anyList()
        )).thenReturn(List.of(lot));

        UUID log1 = UUID.randomUUID();
        UUID log2 = UUID.randomUUID();
        UUID log3 = UUID.randomUUID();
        UUID log4 = UUID.randomUUID();
        FarmLog l1 = FarmLog.builder().id(log1).productionLotId(lot).activityType(FarmActivityType.PLANTING).build();
        FarmLog l2 = FarmLog.builder().id(log2).productionLotId(lot).activityType(FarmActivityType.FERTILIZING).build();
        FarmLog l3 = FarmLog.builder().id(log3).productionLotId(lot).activityType(FarmActivityType.PESTICIDE).build();
        FarmLog l4 = FarmLog.builder().id(log4).productionLotId(lot).activityType(FarmActivityType.HARVESTING).build();

        when(farmLogRepository.findByProductionLotId_IdInOrderByExecutedDateAsc(anyList()))
                .thenReturn(List.of(l1, l2, l3, l4));

        FarmLogAttachment att = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l1).fileName("file.jpg").build();
        FarmLogAttachment att2 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l2).fileName("file2.jpg").build();
        FarmLogAttachment att3 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l3).fileName("file3.jpg").build();
        FarmLogAttachment att4 = FarmLogAttachment.builder().id(UUID.randomUUID()).farmLog(l4).fileName("file4.jpg").build();

        when(farmLogAttachmentRepository.findByFarmLogIdIn(anyList()))
                .thenReturn(List.of(att, att2, att3, att4));

        when(shipmentRepository.findByProductionLotIdIn(anyList())).thenReturn(Collections.emptyList());

        // When
        byte[] result = openDataExportService.exportOpenData(region, null, from, to, "CSV", regulatorDetails,
                ipAddress);

        // Then
        assertThat(result).isNotNull();
        String csvContent = new String(result, StandardCharsets.UTF_8);
        assertThat(csvContent).contains("lotId,lotCode");
        assertThat(csvContent).contains("\"Lot A, \"\"Special\"\"\"");
    }
}
