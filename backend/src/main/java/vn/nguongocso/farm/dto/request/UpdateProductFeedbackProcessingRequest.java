package vn.nguongocso.farm.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;

@Getter
@Setter
public class UpdateProductFeedbackProcessingRequest {

    @NotNull(message = "Mức độ phản ánh không được để trống")
    private ProductFeedbackSeverity severity;

    private UUID traceCodeId;

    @Size(max = 4000, message = "Nội dung xử lý không được vượt quá 4000 ký tự")
    private String processingContent;

    @Size(max = 2000, message = "Phản hồi công khai không được vượt quá 2000 ký tự")
    private String publicResponse;
}
