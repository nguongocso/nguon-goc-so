package vn.nguongocso.export.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * DTO yêu cầu cập nhật mẫu hồ sơ truy xuất theo đối tác.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileTemplateRequest {

    /** Tên mẫu hồ sơ */
    @NotBlank(message = "Tên mẫu hồ sơ không được để trống")
    @Size(max = 255, message = "Tên mẫu hồ sơ không được vượt quá 255 ký tự")
    private String name;

    /** Tên đối tác áp dụng */
    @Size(max = 255, message = "Tên đối tác không được vượt quá 255 ký tự")
    private String partnerName;

    /** Mô tả mục đích sử dụng mẫu */
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    /** Cờ đặt làm mẫu mặc định của tổ chức */
    @Builder.Default
    @JsonProperty("isDefault")
    @JsonAlias({"isDefault", "default", "is_default"})
    private Boolean isDefault = false;

    /** Danh sách các trường được cấu hình trong mẫu */
    @NotEmpty(message = "Danh sách trường chọn không được rỗng")
    @Valid
    private List<FieldSelectionDto> selectedFields;
}
