package vn.nguongocso.trace.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Yêu cầu tạo mới dải mã truy xuất. */
@Data
public class CreateCodeRangeRequest {
    @NotNull(message = "ID tổ chức không được để trống")
    private UUID organizationId;

    @NotBlank(message = "Tiền tố mã không được để trống")
    private String prefix;

    @Positive(message = "Hạn mức phải lớn hơn 0")
    private Long totalLimit;
}
