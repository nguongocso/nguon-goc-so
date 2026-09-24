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
    /** Tổng số lô sản xuất (loại trừ đã hủy/tiêu hủy). */
    @Builder.Default
    private long totalLotsCount = 0;

    /** Số lượng lô đang trong giai đoạn canh tác (APPROVED). */
    @Builder.Default
    private long activeLotsCount = 0;

    /** Số lượng lô đã thu hoạch hoặc đã đóng gói hoàn tất (HARVESTED, PREPROCESSED, PACKAGED, CLOSED). */
    @Builder.Default
    private long harvestedLotsCount = 0;

    /** Số lượng lô đã đóng gói thành phẩm sẵn sàng xuất bán (PACKAGED). */
    @Builder.Default
    private long packagedLotsCount = 0;

    /** Tổng diện tích canh tác (tính theo hecta). */
    @Builder.Default
    private double totalAreaHectares = 0.0;

    /** Danh sách tên hoặc mã các lô sắp đến ngày thu hoạch dự kiến gần nhất. */
    @Builder.Default
    private List<String> upcomingHarvestLotNames = List.of();

    /** Danh sách thông tin mô tả chi tiết các lô sản xuất gần nhất. */
    @Builder.Default
    private List<String> recentLotDetails = List.of();
}
