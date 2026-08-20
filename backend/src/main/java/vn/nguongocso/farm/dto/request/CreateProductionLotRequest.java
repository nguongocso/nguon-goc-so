package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO yêu cầu tạo lô sản xuất.
 */
@Getter
@Setter
public class CreateProductionLotRequest {
    @NotBlank(message = "Tên lô không được để trống")
    private String name;

    private UUID farmAreaId;

    @NotNull(message = "Vui lòng chọn loại nông sản")
    private UUID productCategoryId;

    @NotNull(message = "Vui lòng nhập sản lượng dự kiến")
    @Positive(message = "Sản lượng dự kiến phải lớn hơn 0")
    private Double expectedQuantity;

    @NotBlank(message = "Vui lòng chọn đơn vị sản lượng")
    private String expectedQuantityUnit; // hoặc dùng enum

    private LocalDate plantingDate;
}
