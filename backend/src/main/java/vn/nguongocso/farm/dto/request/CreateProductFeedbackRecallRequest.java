package vn.nguongocso.farm.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductFeedbackRecallRequest {

    private UUID shipmentId;

    @NotBlank(message = "Lý do thu hồi không được để trống")
    @Size(max = 1000, message = "Lý do thu hồi không được vượt quá 1000 ký tự")
    private String reason;

    @Size(max = 2000, message = "Bằng chứng không được vượt quá 2000 ký tự")
    private String evidence;
}
