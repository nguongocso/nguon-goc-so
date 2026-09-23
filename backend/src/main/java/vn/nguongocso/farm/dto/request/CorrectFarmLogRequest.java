package vn.nguongocso.farm.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu đính chính nhật ký canh tác.
*/
@Getter
@Setter
public class CorrectFarmLogRequest {
    @NotBlank(message = "Lý do đính chính không được để trống")
    @Size(max = 500, message = "Lý do đính chính không được vượt quá 500 ký tự")
    private String reason;

    @NotNull(message = "Vui lòng nhập thông tin cần đính chính")
    @Valid
    private FarmLogCorrectionData correctionData;
}
