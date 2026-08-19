package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

/**
 * Request để tạo một dải mã truy xuất mới.
 */
@Data
public class CreateCodeRangeRequest {
    @NotNull(message = "ID tổ chức không được để trống")
    private UUID organizationId;

    @NotBlank(message = "Tiền tố mã không được để trống")
    private String prefix;

    @Positive(message = "Hạn mức phải lớn hơn 0")
    private Long totalLimit;
}
