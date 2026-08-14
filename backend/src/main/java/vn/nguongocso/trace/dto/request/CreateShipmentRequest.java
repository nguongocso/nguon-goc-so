package vn.nguongocso.trace.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Yêu cầu tạo lô hàng.
 */
@Data
public class CreateShipmentRequest {
    @NotNull(message = "Vui lòng chọn lô sản xuất")
    private UUID productionLotId;

    @NotBlank(message = "Tên lô hàng không được để trống")
    private String name;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private long totalQuantity;

    private String packagingInfo;
}