package vn.nguongocso.recall.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload tạo yêu cầu thu hồi lô sản xuất (NCL-08-CN-008).
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateRecallRequest {

    @NotNull(message = "lotId không được để trống.")
    private UUID lotId;

    @NotBlank(message = "Lý do thu hồi không được để trống.")
    @Size(max = 1000, message = "Lý do thu hồi không được vượt quá 1000 ký tự.")
    private String reason;

    @Size(max = 2000, message = "Bằng chứng không được vượt quá 2000 ký tự.")
    private String evidence;
}