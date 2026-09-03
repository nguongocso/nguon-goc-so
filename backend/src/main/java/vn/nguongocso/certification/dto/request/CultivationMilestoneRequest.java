package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

/**
 * Request DTO cho tạo/cập nhật mốc canh tác.
 * - productCategoryId null = áp dụng cho toàn bộ loại nông sản
 * - standardId null        = áp dụng cho mọi tiêu chuẩn
 * Story: NCL-09-CN-011
 */
@Getter
@Setter
public class CultivationMilestoneRequest {

    @NotBlank(message = "Tên mốc canh tác không được để trống")
    @Size(max = 150, message = "Tên mốc canh tác tối đa 150 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @NotBlank(message = "Loại hoạt động không được để trống")
    private String activityType;

    @Min(value = 0, message = "Số ngày dự kiến phải lớn hơn hoặc bằng 0")
    private Integer expectedDaysFromPlanting;

    private UUID productCategoryId;

    private UUID standardId;

    @NotNull(message = "Phải xác định mốc có bắt buộc hay không")
    private Boolean isMandatory;
}
