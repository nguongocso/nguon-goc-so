package vn.nguongocso.alert.service;

import org.springframework.data.domain.Pageable;
import vn.nguongocso.alert.dto.response.AggregateAlertCountResponse;
import vn.nguongocso.alert.dto.response.AggregateAlertPageResponse;
import vn.nguongocso.alert.dto.response.UnviewedAlertCountResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dịch vụ tổng hợp cảnh báo từ 7 nguồn dữ liệu (NCL-08-CN-016).
 */
public interface AggregateAlertService {
    /**
     * Lấy danh sách cảnh báo tổng hợp có bộ lọc và phân trang.
     */
    AggregateAlertPageResponse getAggregateAlerts(
            String type,
            String severity,
            String status,
            UUID organizationId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable);

    /**
     * Lấy số liệu thống kê cảnh báo đang mở phân theo mức khẩn cấp và theo loại.
     */
    AggregateAlertCountResponse getAggregateAlertCounts(UUID organizationId);

    /**
     * Lấy số lượng cảnh báo chưa xử lý phục vụ thanh điều hướng (Header).
     */
    UnviewedAlertCountResponse getUnviewedAlertCount();
}
