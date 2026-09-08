package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloseProductFeedbackRequest {

    @Size(max = 4000, message = "Nội dung xử lý không được vượt quá 4000 ký tự")
    private String processingContent;

    @Size(max = 2000, message = "Phản hồi công khai không được vượt quá 2000 ký tự")
    private String publicResponse;

    @NotBlank(message = "Vui lòng nhập lý do đóng phản ánh")
    @Size(max = 1000, message = "Lý do đóng không được vượt quá 1000 ký tự")
    private String closeReason;
}
