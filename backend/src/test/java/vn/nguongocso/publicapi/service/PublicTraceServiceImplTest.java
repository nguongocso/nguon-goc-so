package vn.nguongocso.publicapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}