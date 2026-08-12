package vn.nguongocso.farm.dto.request;

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
}
