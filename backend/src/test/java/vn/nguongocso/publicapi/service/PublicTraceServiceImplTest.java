package vn.nguongocso.publicapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.alert.service.ScanAnomalyDetectionService;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.publicapi.dto.response.PublicInspectionResponse;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.impl.PublicTraceServiceImpl;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.trace.repository.RecallRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.SuspectDetectionService;

/**
 * Kiểm thử contract NCL-08-CN-007 & TASK-16:
 * - GET trace = đọc thuần túy (KHÔNG tạo ScanLog, KHÔNG kích hoạt đánh giá nghi vấn)
 * - POST scan = tạo ScanLog + kích hoạt đánh giá nghi vấn
 * - GET inspections = lấy danh sách kết quả kiểm nghiệm công khai
 */
@ExtendWith(MockitoExtension.class)
class PublicTraceServiceImplTest {

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private TraceCodeScanLogRepository traceCodeScanLogRepository;

    @Mock
    private ScanAnomalyDetectionService scanAnomalyDetectionService;

    @Mock
    private SuspectDetectionService suspectDetectionService;

    @Mock
    private RecallRepository recallRepository;

    @Mock
    private RecallRequestRepository recallRequestRepository;

    @Mock
    private ProductionLotCertificationRepository productionLotCertificationRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private InspectionCriterionResultRepository inspectionCriterionResultRepository;

    @Mock
    private ReverseGeocodingService reverseGeocodingService;

    private PublicTraceServiceImpl publicTraceService;

    private TraceCode traceCode;
    private Shipment shipment;
    private final String codeValue = "TEST123";

    @BeforeEach
    void setUp() {
        publicTraceService = new PublicTraceServiceImpl(
                traceCodeRepository,
                chainEventRepository,
                new ObjectMapper(),
                traceCodeScanLogRepository,
                scanAnomalyDetectionService,
                suspectDetectionService,
                recallRepository,
                recallRequestRepository,
                productionLotCertificationRepository,
                inspectionRequestRepository,
                inspectionCriterionResultRepository,
                reverseGeocodingService);

        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lo hang test");
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setProductionLot(null);

        traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setCodeValue(codeValue);
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        traceCode.setShipment(shipment);

        when(traceCodeRepository.findByCodeValue(codeValue))
                .thenReturn(Optional.of(traceCode));
        lenient().when(chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(shipment.getId()))
                .thenReturn(Collections.emptyList());
        // Stub này chỉ được dùng ở test POST /scan; dùng lenient để
        // không fail test GET lookup (vốn không gọi save ScanLog).
        lenient().when(traceCodeScanLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getPublicTrace_ShouldNotCreateScanLog_AndNotTriggerDetection() {
        PublicTraceResponse response = publicTraceService.getPublicTrace(
                codeValue, null, null, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals(codeValue, response.getCodeValue());

        // GET lookup KHÔNG tạo ScanLog, KHÔNG kích hoạt phát hiện nghi vấn.
        verify(traceCodeScanLogRepository, never()).save(any());
        verify(scanAnomalyDetectionService, never()).onScanRecorded(any());
    }

    @Test
    void recordPublicScan_ShouldCreateScanLog_AndTriggerDetection() {
        PublicTraceResponse response = publicTraceService.recordPublicScan(
                codeValue, null, null, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals(codeValue, response.getCodeValue());

        // POST scan tạo đúng 1 ScanLog và kích hoạt đánh giá nghi vấn.
        verify(traceCodeScanLogRepository).save(any());
        verify(suspectDetectionService).evaluateSuspicion(traceCode.getId());
        verify(scanAnomalyDetectionService).onScanRecorded(traceCode.getId());
    }

    @Test
    void getPublicInspections_WhenProductionLotExists_ShouldReturnInspections() {
        ProductionLot lot = new ProductionLot();
        lot.setId(UUID.randomUUID());
        lot.setName("Lô nông sản A");
        shipment.setProductionLot(lot);

        InspectionRequest req = InspectionRequest.builder()
                .id(UUID.randomUUID())
                .inspectionUnit("TT Kiểm nghiệm Chất lượng")
                .productionLot(lot)
                .build();

        InspectionCriterion criterion = InspectionCriterion.builder()
                .id(UUID.randomUUID())
                .criterionCode("PESTICIDE_RESIDUE")
                .criterionName("Dư lượng thuốc BVTV")
                .build();

        InspectionCriterionResult result = InspectionCriterionResult.builder()
                .id(UUID.randomUUID())
                .inspectionCriterion(criterion)
                .passed(true)
                .build();

        when(inspectionRequestRepository.findByProductionLot_IdOrderByCreatedAtDesc(lot.getId()))
                .thenReturn(List.of(req));
        when(inspectionCriterionResultRepository.findByInspectionCriterion_InspectionRequest_Id(req.getId()))
                .thenReturn(List.of(result));

        PublicInspectionResponse response = publicTraceService.getPublicInspections(codeValue);

        assertNotNull(response);
        assertEquals(lot.getId(), response.getProductionLotId());
        assertEquals("Lô nông sản A", response.getLotName());
        assertEquals(1, response.getInspections().size());
        assertEquals("Dư lượng thuốc BVTV", response.getInspections().get(0).getCriterionName());
        assertEquals(true, response.getInspections().get(0).getPassed());
    }

    @Test
    void getPublicTrace_WhenHarvestEventHasEarlyHarvest_ShouldIncludeEarlyHarvestInEventData() {
        ProductionLot lot = new ProductionLot();
        lot.setId(UUID.randomUUID());
        lot.setName("Lô nông sản A");
        shipment.setProductionLot(lot);

        vn.nguongocso.event.entity.ChainEvent harvestEvent = vn.nguongocso.event.entity.ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(vn.nguongocso.event.enums.ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + lot.getId() + "\",\"harvestDate\":\"2026-07-24\",\"quantity\":1500.0,\"earlyHarvest\":true,\"earlyHarvestReason\":\"Bão lụt khẩn cấp\",\"eligibleHarvestDate\":\"2026-07-28\"}")
                .recordedAt(java.time.LocalDateTime.now())
                .build();

        when(chainEventRepository.findByShipmentIsNullAndEventTypeIn(any()))
                .thenReturn(List.of(harvestEvent));

        PublicTraceResponse response = publicTraceService.getPublicTrace(codeValue, null, null, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals(1, response.getEvents().size());
        assertEquals(true, response.getEvents().get(0).getEventData().get("earlyHarvest"));
        // B-03: earlyHarvestReason không được expose ra Public QR cho người tiêu dùng
        assertNull(response.getEvents().get(0).getEventData().get("earlyHarvestReason"));
        assertEquals("2026-07-28", response.getEvents().get(0).getEventData().get("eligibleHarvestDate"));
    }

    @Test
    void getPublicTrace_WhenHarvestEventHasUnmatchedMaterials_ShouldIncludeUnmatchedInEventData() {
        ProductionLot lot = new ProductionLot();
        lot.setId(UUID.randomUUID());
        lot.setName("Lô nông sản B");
        shipment.setProductionLot(lot);

        vn.nguongocso.event.entity.ChainEvent harvestEvent = vn.nguongocso.event.entity.ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(vn.nguongocso.event.enums.ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + lot.getId() + "\",\"harvestDate\":\"2026-07-24\",\"quantity\":1500.0,\"earlyHarvest\":false,\"unmatchedMaterials\":[\"Vật tư mới\"]}")
                .recordedAt(java.time.LocalDateTime.now())
                .build();

        when(chainEventRepository.findByShipmentIsNullAndEventTypeIn(any()))
                .thenReturn(List.of(harvestEvent));

        PublicTraceResponse response = publicTraceService.getPublicTrace(codeValue, null, null, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals(1, response.getEvents().size());
        assertEquals("HARVEST", response.getEvents().get(0).getEventType());
        assertEquals(false, response.getEvents().get(0).getEventData().get("earlyHarvest"));
        assertNotNull(response.getEvents().get(0).getEventData().get("unmatchedMaterials"));
    }

    @Test
    void getPublicTrace_WhenLegacyHarvestEventWithoutEarlyHarvestFields_ShouldNotCrash() {
        ProductionLot lot = new ProductionLot();
        lot.setId(UUID.randomUUID());
        lot.setName("Lô nông sản C");
        shipment.setProductionLot(lot);

        vn.nguongocso.event.entity.ChainEvent legacyEvent = vn.nguongocso.event.entity.ChainEvent.builder()
                .id(UUID.randomUUID())
                .eventType(vn.nguongocso.event.enums.ChainEventType.HARVEST)
                .eventData("{\"productionLotId\":\"" + lot.getId() + "\",\"harvestDate\":\"2025-05-10\",\"quantity\":2000.0}")
                .recordedAt(java.time.LocalDateTime.now())
                .build();

        when(chainEventRepository.findByShipmentIsNullAndEventTypeIn(any()))
                .thenReturn(List.of(legacyEvent));

        PublicTraceResponse response = publicTraceService.getPublicTrace(codeValue, null, null, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals(1, response.getEvents().size());
        assertEquals("HARVEST", response.getEvents().get(0).getEventType());
        assertEquals(2000.0, response.getEvents().get(0).getEventData().get("quantity"));
    }
}