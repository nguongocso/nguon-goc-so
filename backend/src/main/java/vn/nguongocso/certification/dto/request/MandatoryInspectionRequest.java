package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for enabling/disabling mandatory inspection on a product category.
 * Story: NCL-09-CN-009
 */
@Getter
@Setter
public class MandatoryInspectionRequest {

    @NotNull(message = "Trường 'required' là bắt buộc")
    private Boolean required;
}
