package vn.nguongocso.farm.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu thiết lập hoặc cập nhật ranh giới vùng trồng.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFarmAreaBoundaryRequest {
    @Valid
    @NotNull(message = "Danh sách tọa độ không được để trống")
    @Size(min = 3, max = 500, message = "Ranh giới vùng trồng phải có từ 3 đến 500 đỉnh")
    private List<LatLngDto> points;

    @Builder.Default
    private Boolean confirmed = false;
}
