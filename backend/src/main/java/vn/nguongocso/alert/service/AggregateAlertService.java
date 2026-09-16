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
     *
     * @param type           loại cảnh báo (null nếu lấy tất cả)
     * @param severity       mức khẩn cấp (HIGH, MEDIUM, null nếu lấy tất cả)
     * @param status         trạng thái (OPEN, RESOLVED, ALL)
     * @param organizationId ID tổ chức (chỉ áp dụng cho VT-01)
     * @param keyword        từ khóa tìm kiếm
     * @param fromDate       ngày bắt đầu
     * @param toDate         ngày kết thúc
     * @param pageable       thông tin phân trang
     * @return AggregateAlertPageResponse chứa danh sách và tóm tắt
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
     *
     * @param organizationId ID tổ chức (áp dụng cho VT-01 nếu muốn xem theo tổ chức)
     * @return AggregateAlertCountResponse
     */
    AggregateAlertCountResponse getAggregateAlertCounts(UUID organizationId);

    /**
     * Lấy số lượng cảnh báo chưa xử lý phục vụ thanh điều hướng (Header).
     *
     * @return UnviewedAlertCountResponse
     */
    UnviewedAlertCountResponse getUnviewedAlertCount();
}
