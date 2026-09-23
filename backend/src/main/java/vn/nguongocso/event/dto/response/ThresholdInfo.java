package vn.nguongocso.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Thông tin ngưỡng bảo quản của loại nông sản.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
public class ThresholdInfo {
    private Double tempMin;
    private Double tempMax;
    private Double humidityMin;
    private Double humidityMax;
}