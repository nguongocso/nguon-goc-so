package vn.nguongocso.report.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** Thông tin so sánh sản lượng giữa các mùa vụ. */
@Getter
@Builder
public class SeasonYieldComparisonResponse {

    private Boolean hasData;

    private String message;

    private Integer baselineYear;

    private String baselineSeasonCode;

    private String baselineSeasonName;

    private List<SeasonYieldItemResponse> seasons;
}