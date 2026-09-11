package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO yêu cầu tạo lô sản xuất mới từ mẫu vụ trước (NCL-02-CN-007).
 *
 * <p>
 * Chỉ nhận các trường người dùng được phép chỉnh sửa của vụ mới. Vùng trồng
 * và loại nông sản được kế thừa từ lô mẫu nên không nhận từ client; tổ chức
 * và người tạo luôn lấy từ tài khoản đăng nhập.
 * </p>
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
