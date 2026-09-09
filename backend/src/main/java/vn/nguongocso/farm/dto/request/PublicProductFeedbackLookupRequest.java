package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicProductFeedbackLookupRequest {

    @NotBlank(message = "Vui lòng nhập mã tra cứu phản ánh")
    @Size(max = 64, message = "Mã tra cứu phản ánh không hợp lệ")
    private String lookupCode;
}
