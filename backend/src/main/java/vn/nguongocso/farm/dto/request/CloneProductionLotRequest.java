package vn.nguongocso.farm.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

/**
 * Yêu cầu tạo lô sản xuất mới từ mẫu vụ trước.
*/
@Getter
@Setter
public class CloneProductionLotRequest {
    @NotBlank(message = "Tên lô không được để trống")
    private String name;

    @NotNull(message = "Vui lòng nhập sản lượng dự kiến")
    @Positive(message = "Sản lượng dự kiến phải lớn hơn 0")
    private Double expectedQuantity;

    @NotBlank(message = "Vui lòng chọn đơn vị sản lượng")
    private String expectedQuantityUnit;

    private LocalDate plantingDate;
}
