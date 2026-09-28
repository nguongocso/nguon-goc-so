package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO request bật/tắt chế độ kiểm nghiệm bắt buộc cho một loại nông sản Story: NCL-09-CN-009
 */
@Getter
@Setter
public class MandatoryInspectionRequest {
    @NotNull(message = "Trường 'required' là bắt buộc")
    private Boolean required;
}
