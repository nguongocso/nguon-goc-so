package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * Request DTO for creating/updating an inspection criterion catalog entry.
 */
@Getter
@Setter
public class InspectionCriterionCatalogRequest {

    @NotBlank(message = "Tên chỉ tiêu không được để trống")
    @Size(max = 150, message = "Tên chỉ tiêu tối đa 150 ký tự")
    private String name;

    @NotBlank(message = "Đơn vị tính không được để trống")
    @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự")
    private String unit;

    @NotNull(message = "Ngưỡng tối đa không được để trống")
    @DecimalMin(value = "0.0001", message = "Ngưỡng tối đa phải lớn hơn 0")
    private BigDecimal maxThreshold;

    @Size(max = 150, message = "Tiêu chuẩn tham chiếu tối đa 150 ký tự")
    private String referenceStandard;
}
