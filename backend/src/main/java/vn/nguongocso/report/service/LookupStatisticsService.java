package vn.nguongocso.report.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.response.AbnormalScanResponse;
import vn.nguongocso.report.dto.response.LookupStatisticsResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface LookupStatisticsService {
    LookupStatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate, UUID productionLotId, UUID shipmentId, UUID organizationId, String groupBy, CustomUserDetails currentUser);
    Page<AbnormalScanResponse> getAbnormalScans(LocalDate startDate, LocalDate endDate, UUID productionLotId, UUID organizationId, Pageable pageable, CustomUserDetails currentUser);
}
