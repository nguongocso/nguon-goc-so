package vn.nguongocso.farm.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Yêu cầu tạo nhật ký canh tác.
 */
@Getter
@Setter
public class CreateFarmLogRequest {
    @NotNull(message = "Vui lòng chọn lô sản xuất")
    private UUID productionLotId;

    @NotNull(message = "Vui lòng chọn loại hoạt động")
    private FarmActivityType activityType;

    @Size(max = 255, message = "Tên vật tư không được vượt quá 255 ký tự")
    private String material;

    @Positive(message = "Số lượng phải lớn hơn 0")
    private Double quantity;

    @Size(max = 50, message = "Đơn vị không được vượt quá 50 ký tự")
    private String unit;

    @NotNull(message = "Vui lòng chọn ngày thực hiện")
    private LocalDate executedDate;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;
}