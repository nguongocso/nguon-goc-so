package vn.nguongocso.report.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.response.AbnormalScanResponse;
import vn.nguongocso.report.dto.response.LookupStatisticsResponse;

/** Service thống kê tra cứu mã truy xuất. */
public interface LookupStatisticsService {

    /**
     * Lấy thống kê tra cứu mã truy xuất.
     */
    LookupStatisticsResponse getStatistics(
        LocalDate startDate,
        LocalDate endDate,
        UUID productionLotId,
        UUID shipmentId,
        UUID organizationId,
        String groupBy,
        CustomUserDetails currentUser
    );

    /**
     * Lấy danh sách tra cứu bất thường có phân trang.
     */
    Page<AbnormalScanResponse> getAbnormalScans(
        LocalDate startDate,
        LocalDate endDate,
        UUID productionLotId,
        UUID organizationId,
        Pageable pageable,
        CustomUserDetails currentUser
    );
}