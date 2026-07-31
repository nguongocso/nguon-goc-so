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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.report.dto.response.AbnormalScanResponse;
import vn.nguongocso.report.dto.response.LookupStatisticsResponse;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.report.service.impl.LookupStatisticsServiceImpl;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.farm.entity.ProductionLot;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class LookupStatisticsServiceTest {

    @Mock
    private TraceCodeScanLogRepository traceCodeScanLogRepository;

    @InjectMocks
    private LookupStatisticsServiceImpl lookupStatisticsService;

    private CustomUserDetails adminUser;
    private CustomUserDetails htxUser;
    private final UUID htxOrgId = UUID.randomUUID();
    private final UUID otherOrgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adminUser = mock(CustomUserDetails.class);
        when(adminUser.getRoleCode()).thenReturn("VT-01");

        htxUser = mock(CustomUserDetails.class);
        when(htxUser.getRoleCode()).thenReturn("VT-02");
        when(htxUser.getOrganizationId()).thenReturn(htxOrgId);
    }

    @Test
    void getStatistics_shouldSuccess_forAdmin() {
        // Given
        when(traceCodeScanLogRepository.countScans(any(), any(), any(), any(), any())).thenReturn(100L);
        when(traceCodeScanLogRepository.countUniqueCodes(any(), any(), any(), any(), any())).thenReturn(40L);
        when(traceCodeScanLogRepository.countAbnormalScans(any(), any(), any(), any(), any())).thenReturn(5L);
        when(traceCodeScanLogRepository.getStatsByLocation(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(traceCodeScanLogRepository.getStatsByProductionLot(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(traceCodeScanLogRepository.getScannedAtList(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

        // When
        LookupStatisticsResponse response = lookupStatisticsService.getStatistics(
                null, null, null, null, otherOrgId, "MONTH", adminUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSummary().getTotalScans()).isEqualTo(100L);
        assertThat(response.getSummary().getTotalUniqueCodes()).isEqualTo(40L);
        assertThat(response.getSummary().getAbnormalScansCount()).isEqualTo(5L);
    }

    @Test
    void getStatistics_shouldThrowForbidden_whenHtxUserAccessOtherOrg() {
        // When / Then
        assertThatThrownBy(() -> lookupStatisticsService.getStatistics(
                null, null, null, null, otherOrgId, "MONTH", htxUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không có quyền truy cập dữ liệu của tổ chức khác");
    }

    @Test
    void getAbnormalScans_shouldReturnPage_whenAuthorized() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        ProductionLot productionLot = new ProductionLot();
        productionLot.setName("Lô Chè A");

        Shipment shipment = new Shipment();
        shipment.setProductionLot(productionLot);

        TraceCode traceCode = new TraceCode();
        traceCode.setCodeValue("NCL0001");
        traceCode.setShipment(shipment);

        TraceCodeScanLog scanLog = TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .traceCode(traceCode)
                .isAbnormal(true)
                .scannedAt(LocalDateTime.now())
                .abnormalReason("Vượt ngưỡng")
                .build();

        Page<TraceCodeScanLog> logPage = new PageImpl<>(List.of(scanLog), pageable, 1);
        when(traceCodeScanLogRepository.findAbnormalScans(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(logPage);

        // When
        Page<AbnormalScanResponse> response = lookupStatisticsService.getAbnormalScans(
                null, null, null, htxOrgId, pageable, htxUser);

        // Then
        assertThat(response).isNotEmpty();
        assertThat(response.getContent().getFirst().getCodeValue()).isEqualTo("NCL0001");
        assertThat(response.getContent().getFirst().getReason()).isEqualTo("Vượt ngưỡng");
    }
}
