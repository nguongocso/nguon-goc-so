package vn.nguongocso.export.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import vn.nguongocso.export.enums.ProfileFieldGroup;

/**
 * DTO đại diện cho một trường được chọn trong yêu cầu tạo / cập nhật mẫu hồ sơ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldSelectionDto {

    /** Mã định danh trường (ví dụ: organization.name) */
    @NotBlank(message = "Mã trường (fieldKey) không được để trống")
    private String fieldKey;

    /** Nhóm trường */
    @NotNull(message = "Nhóm trường (fieldGroup) không được để trống")
    private ProfileFieldGroup fieldGroup;

    /** Tên hiển thị tùy biến (tùy chọn) */
    private String displayName;

    /** Thứ tự sắp xếp hiển thị */
    private Integer sortOrder;
}
