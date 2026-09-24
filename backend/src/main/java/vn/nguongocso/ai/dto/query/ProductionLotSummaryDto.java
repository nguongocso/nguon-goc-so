package vn.nguongocso.ai.dto.query;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO chứa thông tin tổng hợp về lô sản xuất và diện tích canh tác.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionLotSummaryDto {
    /** Số lượng lô đang trong giai đoạn canh tác (APPROVED). */
    private long activeLotsCount;

    /** Số lượng lô đã thu hoạch hoàn tất (HARVESTED). */
    private long harvestedLotsCount;

    /** Tổng diện tích canh tác (tính theo hecta). */
    private double totalAreaHectares;

    /** Danh sách tên hoặc mã các lô sắp đến ngày thu hoạch dự kiến gần nhất. */
    private List<String> upcomingHarvestLotNames;
}
