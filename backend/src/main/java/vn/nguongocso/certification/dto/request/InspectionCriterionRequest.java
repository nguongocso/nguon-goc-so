package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request DTO cho chỉ tiêu kiểm nghiệm
 */
@Getter
@Setter
public class InspectionCriterionRequest {

    @NotNull(message = "Tiêu chuẩn không được để trống")
    private UUID standardId;

    @NotBlank(message = "Mã chỉ tiêu không được để trống")
    @Size(max = 100)
    private String criterionCode;

    @NotBlank(message = "Tên chỉ tiêu không được để trống")
    @Size(max = 255)
    private String criterionName;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String note;
}