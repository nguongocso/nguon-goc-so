package vn.nguongocso.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response so sánh sản lượng giữa các mùa vụ.
 */
@Getter
@Builder
public class SeasonYieldComparisonResponse {
    /**
     * Có dữ liệu để so sánh hay không.
     */
    private Boolean hasData;

    /**
     * Thông báo khi không có dữ liệu.
     */
    private String message;

    /**
     * Năm mùa vụ gốc.
     */
    private Integer baselineYear;

    /**
     * Mã mùa vụ gốc.
     */
    private String baselineSeasonCode;

    /**
     * Tên mùa vụ gốc.
     */
    private String baselineSeasonName;

    /**
     * Danh sách kết quả so sánh.
     */
    private List<SeasonYieldItemResponse> seasons;
}