package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu tạo một đợt thu hồi.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecallRequest {
    @NotBlank(message = "Lý do thu hồi không được để trống.")
    @Size(max = 1000, message = "Lý do thu hồi không được vượt quá 1000 ký tự.")
    private String reason;
}