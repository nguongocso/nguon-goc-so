package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating/updating a cultivation milestone catalog entry.
 * Story: NCL-09-CN-011
 */
@Getter
@Setter
public class CultivationMilestoneCatalogRequest {

    @NotBlank(message = "Tên mốc canh tác không được để trống")
    @Size(max = 150, message = "Tên mốc canh tác tối đa 150 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @NotBlank(message = "Loại hoạt động không được để trống")
    private String activityType;

    @Min(value = 1, message = "Số ngày dự kiến phải lớn hơn 0")
    private Integer expectedDaysFromPlanting;
}
