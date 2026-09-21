package vn.nguongocso.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Phản hồi danh sách cảnh báo tổng hợp có phân trang và khối thống kê
 * (NCL-08-CN-016).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateAlertPageResponse {
    private List<AggregateAlertItemResponse> items;

    private long totalElements;

    private int totalPages;

    private int currentPage;

    private int pageSize;

    private AggregateAlertCountResponse summaryCounts;
}
