package vn.nguongocso.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.notification.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.report.dto.response.LookupResponse;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.report.service.impl.PublicLookupServiceImpl;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class PublicLookupServiceTest {

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private TraceCodeScanLogRepository traceCodeScanLogRepository;

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private FarmLogAttachmentRepository farmLogAttachmentRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PublicLookupServiceImpl publicLookupService;

    private TraceCode traceCode;
    private Shipment shipment;
    private ProductionLot productionLot;
    private Organization organization;
    private final String codeValue = "NCL00000001";

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        organization.setName("HTX Tân Cương");

        productionLot = new ProductionLot();
        productionLot.setId(UUID.randomUUID());
        productionLot.setName("Lô Chè Tân Cương");
        productionLot.setOrganization(organization);

        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Lô hàng Chè Xuất Khẩu");
        shipment.setProductionLot(productionLot);
        shipment.setOrganization(organization);

        traceCode = new TraceCode();
        traceCode.setId(UUID.randomUUID());
        traceCode.setCodeValue(codeValue);
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        traceCode.setShipment(shipment);
        traceCode.setActivatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void lookupCode_shouldSuccess_whenCodeActive() {
        // Given
        when(traceCodeRepository.findByCodeValue(codeValue)).thenReturn(Optional.of(traceCode));
        when(traceCodeScanLogRepository.countByTraceCodeIdAndScannedAtAfter(any(), any())).thenReturn(0L);
        when(traceCodeScanLogRepository.findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(any())).thenReturn(Collections.emptyList());
        when(chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(any())).thenReturn(Collections.emptyList());

        // When
        LookupResponse response = publicLookupService.lookupCode(
                codeValue, 21.0285, 105.8048, "Hà Nội", "127.0.0.1", "Mozilla/5.0");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCodeValue()).isEqualTo(codeValue);
        assertThat(response.getStatus()).isEqualTo(TraceCodeStatus.ACTIVE);
        verify(traceCodeScanLogRepository, times(1)).save(any(TraceCodeScanLog.class));
    }

    @Test
    void lookupCode_shouldThrowNotFound_whenCodeNotExist() {
        // Given
        when(traceCodeRepository.findByCodeValue(codeValue)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> publicLookupService.lookupCode(codeValue, null, null, null, "127.0.0.1", "Browser"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy mã truy xuất");

        verify(traceCodeScanLogRepository, never()).save(any());
    }

    @Test
    void lookupCode_shouldThrowBusinessException_whenCodeInactive() {
        // Given
        traceCode.setStatus(TraceCodeStatus.INACTIVE);
        when(traceCodeRepository.findByCodeValue(codeValue)).thenReturn(Optional.of(traceCode));

        // When / Then
        assertThatThrownBy(() -> publicLookupService.lookupCode(codeValue, null, null, null, "127.0.0.1", "Browser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mã truy xuất chưa được kích hoạt");

        // Vẫn lưu lịch sử quét
        verify(traceCodeScanLogRepository, times(1)).save(any(TraceCodeScanLog.class));
    }

    @Test
    void lookupCode_shouldTriggerRateLimitAnomaly_whenScannedTooOften() {
        // Given
        when(traceCodeRepository.findByCodeValue(codeValue)).thenReturn(Optional.of(traceCode));
        // Giả lập đã quét 10 lần trong 24h
        when(traceCodeScanLogRepository.countByTraceCodeIdAndScannedAtAfter(any(), any())).thenReturn(10L);

        // When
        LookupResponse response = publicLookupService.lookupCode(
                codeValue, null, null, null, "127.0.0.1", "Browser");

        // Then
        assertThat(response).isNotNull();
        // Kiểm tra xem lượt quét mới có bị đánh dấu bất thường và gọi NotificationService hay không
        verify(traceCodeScanLogRepository).save(argThat(log -> log.getIsAbnormal() && log.getAbnormalReason().contains("quá giới hạn 10 lần")));
        verify(notificationService, times(1)).sendAlert(contains("bị quét quá nhiều lần"));
    }

    @Test
    void lookupCode_shouldTriggerImpossibleTravelAnomaly_whenScannedInDifferentPlaces() {
        // Given
        when(traceCodeRepository.findByCodeValue(codeValue)).thenReturn(Optional.of(traceCode));
        when(traceCodeScanLogRepository.countByTraceCodeIdAndScannedAtAfter(any(), any())).thenReturn(1L);

        // Giả lập lịch sử quét gần đây ở địa điểm khác trong vòng 1 giờ
        TraceCodeScanLog recentScan = TraceCodeScanLog.builder()
                .location("TP. Hồ Chí Minh")
                .latitude(BigDecimal.valueOf(10.8231))
                .longitude(BigDecimal.valueOf(106.6297))
                .scannedAt(LocalDateTime.now().minusMinutes(20))
                .build();
        when(traceCodeScanLogRepository.findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(any(), any()))
                .thenReturn(List.of(recentScan));

        // When (quét tại Hà Nội cách xa 1000+ km trong 20 phút)
        LookupResponse response = publicLookupService.lookupCode(
                codeValue, 21.0285, 105.8048, "Hà Nội", "127.0.0.1", "Browser");

        // Then
        assertThat(response).isNotNull();
        verify(traceCodeScanLogRepository).save(argThat(log -> log.getIsAbnormal() && log.getAbnormalReason().contains("cách xa nhau")));
        verify(notificationService, times(1)).sendAlert(contains("khoảng cách địa lý"));
    }
}
