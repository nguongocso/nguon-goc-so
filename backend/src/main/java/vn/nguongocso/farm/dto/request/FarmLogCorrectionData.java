package vn.nguongocso.farm.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * NCL-03-CN-006: dữ liệu đính chính cho một nhật ký canh tác.
 *
 * <p>Tất cả các trường đều tùy chọn; trường nào được gửi sẽ thay thế giá trị
 * hiệu lực hiện tại của bản ghi. Ít nhất một trường phải khác giá trị bản gốc.</p>
 */
@Getter
@Setter
public class FarmLogCorrectionData {

    @Size(max = 255, message = "Tên vật tư không được vượt quá 255 ký tự")
    private String material;

    @Positive(message = "Số lượng phải lớn hơn 0")
    private Double quantity;

    @Size(max = 50, message = "Đơn vị không được vượt quá 50 ký tự")
    private String unit;

    private LocalDate executedDate;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;

    private FarmActivityType activityType;
}
