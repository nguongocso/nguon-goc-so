package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu cập nhật loại nông sản.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductCategoryRequest {
    @NotBlank(message = "Tên loại nông sản không được để trống")
    @Size(max = 255, message = "Tên loại nông sản không vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "Nhóm hàng không được để trống")
    @Size(max = 100, message = "Tên nhóm hàng không vượt quá 100 ký tự")
    private String group;

    @Size(max = 1000, message = "Mô tả không vượt quá 1000 ký tự")
    private String description;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;

    @DecimalMin(value = "-99.9", message = "Nhiệt độ tối thiểu phải từ -99.9 đến 999.9 độ C")
    @DecimalMax(value = "999.9", message = "Nhiệt độ tối thiểu phải từ -99.9 đến 999.9 độ C")
    private Double tempMin;

    @DecimalMin(value = "-99.9", message = "Nhiệt độ tối đa phải từ -99.9 đến 999.9 độ C")
    @DecimalMax(value = "999.9", message = "Nhiệt độ tối đa phải từ -99.9 đến 999.9 độ C")
    private Double tempMax;

    @DecimalMin(value = "0.0", message = "Độ ẩm tối thiểu phải từ 0 đến 100%")
    @DecimalMax(value = "100.0", message = "Độ ẩm tối thiểu phải từ 0 đến 100%")
    private Double humidityMin;

    @DecimalMin(value = "0.0", message = "Độ ẩm tối đa phải từ 0 đến 100%")
    @DecimalMax(value = "100.0", message = "Độ ẩm tối đa phải từ 0 đến 100%")
    private Double humidityMax;
}
