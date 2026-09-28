package vn.nguongocso.report.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Thông tin sản lượng của một mùa vụ. */
@Getter
@Builder
public class SeasonYieldItemResponse {
    private Integer year;

    private String seasonCode;

    private String seasonName;

    private Long lotCount;

    private Double totalQuantity;

    private Double delta;

    private Double deltaPercent;
}
