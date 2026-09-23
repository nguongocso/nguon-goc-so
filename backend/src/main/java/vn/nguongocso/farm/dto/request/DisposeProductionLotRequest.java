package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu loại bỏ lô sản xuất.
*/
@Getter
@Setter
public class DisposeProductionLotRequest {
    @NotBlank(message = "Lý do loại bỏ không được để trống")
    @Size(max = 100, message = "Lý do loại bỏ không được vượt quá 100 ký tự")
    private String reason;

    @NotBlank(message = "Biện pháp xử lý không được để trống")
    @Size(max = 1000, message = "Biện pháp xử lý không được vượt quá 1000 ký tự")
    private String handlingMeasure;

    @Size(max = 1000, message = "Diễn giải không được vượt quá 1000 ký tự")
    private String note;
}
