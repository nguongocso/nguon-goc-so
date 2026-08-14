package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO yêu cầu tạo phản ánh sản phẩm.
 */
@Getter
@Setter
public class CreateProductFeedbackRequest {
    @NotBlank(message = "Nội dung phản ánh không được để trống")
    @Size(max = 1000, message = "Nội dung phản ánh không được vượt quá 1000 ký tự")
    private String content;
}
