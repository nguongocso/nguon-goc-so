package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tọa độ một đỉnh của ranh giới vùng trồng. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatLngDto {

    @NotNull(message = "Vĩ độ không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng [-90, 90]")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng [-90, 90]")
    private Double latitude;

    @NotNull(message = "Kinh độ không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng [-180, 180]")
    @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng [-180, 180]")
    private Double longitude;
}
