package vn.nguongocso.report.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response thông tin sản lượng của một mùa vụ trong kết quả so sánh.
 */
@Getter
@Builder
public class SeasonYieldItemResponse {
    /**
     * Năm mùa vụ.
     */
    private Integer year;

    /**
     * Mã mùa vụ.
     */
    private String seasonCode;

    /**
     * Tên mùa vụ.
     */
    private String seasonName;

    /**
     * Tổng số lô sản xuất.
     */
    private Long lotCount;

    /**
     * Tổng sản lượng thực tế.
     */
    private Double totalQuantity;

    /**
     * Chênh lệch sản lượng so với mùa vụ gốc.
     */
    private Double delta;

    /**
     * Tỷ lệ chênh lệch (%).
     */
    private Double deltaPercent;
}