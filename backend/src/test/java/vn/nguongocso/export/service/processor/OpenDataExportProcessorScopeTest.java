package vn.nguongocso.export.service.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenDataExportProcessorScopeTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ChainEventRepository chainEventRepository;
    @Mock
    private FarmLogRepository farmLogRepository;
    @Mock
    private ProductionLotCertificationRepository productionLotCertificationRepository;

    @Captor
    private ArgumentCaptor<Set<UUID>> orgCaptor;

    private OpenDataExportProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OpenDataExportProcessor(
                shipmentRepository,
                chainEventRepository,
                farmLogRepository,
                productionLotCertificationRepository);
    }

    @Test
    @DisplayName("getEventTypesByShipment: Chỉ truy vấn sự kiện unassigned của tổ chức thuộc lô hàng được xuất")
    void getEventTypesByShipment_shouldScopeUnassignedEventsToRelevantOrganization() {
        UUID orgId1 = UUID.randomUUID();
        UUID orgId2 = UUID.randomUUID();

        Organization org1 = new Organization();
        org1.setOrganizationId(orgId1);

        UUID lotId1 = UUID.randomUUID();
        ProductionLot lot1 = new ProductionLot();
        lot1.setId(lotId1);

        UUID shipmentId1 = UUID.randomUUID();
        Shipment shipment1 = new Shipment();
        shipment1.setId(shipmentId1);
        shipment1.setOrganization(org1);
        shipment1.setProductionLot(lot1);

        ChainEvent unassignedHarvestEvent = ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .recordedOrganizationId(orgId1)
                .eventData("{\"productionLotId\":\"" + lotId1 + "\"}")
                .build();

        when(chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(List.of(shipmentId1)))
                .thenReturn(List.of());

        when(chainEventRepository.findByShipmentIsNullAndEventTypeInAndRecordedOrganizationIdIn(
                any(), any()))
                .thenReturn(List.of(unassignedHarvestEvent));

        Map<UUID, Set<ChainEventType>> eventMap = processor.getEventTypesByShipment(List.of(shipment1));

        assertThat(eventMap).containsKey(shipmentId1);
        assertThat(eventMap.get(shipmentId1)).contains(ChainEventType.HARVEST);

        // Đảm bảo không quét toàn bộ bảng không có điều kiện lọc tổ chức
        verify(chainEventRepository, never()).findByShipmentIsNullAndEventTypeIn(any());

        // Đảm bảo chỉ gọi với orgId1, không chứa orgId2
        verify(chainEventRepository).findByShipmentIsNullAndEventTypeInAndRecordedOrganizationIdIn(any(), orgCaptor.capture());
        assertThat(orgCaptor.getValue()).contains(orgId1);
        assertThat(orgCaptor.getValue()).doesNotContain(orgId2);
    }

    @Test
    @DisplayName("getDocumentationExistence: Dùng batch query tìm lot có farm log thay vì gọi existsByProductionLotId N lần")
    void getDocumentationExistence_shouldUseBatchQuery() {
        UUID lotId1 = UUID.randomUUID();
        UUID lotId2 = UUID.randomUUID();

        ProductionLot lot1 = new ProductionLot();
        lot1.setId(lotId1);
        ProductionLot lot2 = new ProductionLot();
        lot2.setId(lotId2);

        Shipment shipment1 = new Shipment();
        shipment1.setId(UUID.randomUUID());
        shipment1.setProductionLot(lot1);

        Shipment shipment2 = new Shipment();
        shipment2.setId(UUID.randomUUID());
        shipment2.setProductionLot(lot2);

        when(farmLogRepository.findDistinctProductionLotIdsWithFarmLogsIn(Set.of(lotId1, lotId2)))
                .thenReturn(Set.of(lotId1));
        when(productionLotCertificationRepository.findByProductionLotIdIn(any()))
                .thenReturn(List.of());

        Map<UUID, Boolean> docMap = processor.getDocumentationExistence(List.of(shipment1, shipment2));

        assertThat(docMap.get(shipment1.getId())).isTrue();
        assertThat(docMap.get(shipment2.getId())).isFalse();

        // Không bao giờ gọi existsByProductionLotId riêng lẻ
        verify(farmLogRepository, never()).existsByProductionLotId(any());
        verify(farmLogRepository).findDistinctProductionLotIdsWithFarmLogsIn(Set.of(lotId1, lotId2));
    }
}
