package vn.nguongocso.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO dùng để loại bỏ lô sản xuất (NCL-11-CN-005, QTN-30).
 *
 * <p>
 * Lô có kết quả kiểm nghiệm Không đạt phải được xử lý theo một trong hai
 * hướng: loại bỏ lô hoặc kiểm nghiệm lại. Khi Quản lý hợp tác xã (VT-02)
 * chọn loại bỏ, lý do và biện pháp xử lý là bắt buộc (TC-03).
 * </p>
 */
@Getter
@Setter
public class DisposeProductionLotRequest {

    /**
     * Lý do loại bỏ — bắt buộc (TC-03), tối đa 100 ký tự
     * (đồng bộ {@link CancelProductionLotRequest}).
     */
    @NotBlank(message = "Lý do loại bỏ không được để trống")
    @Size(max = 100, message = "Lý do loại bỏ không được vượt quá 100 ký tự")
    private String reason;

    /**
     * Biện pháp xử lý lô — bắt buộc (TC-03), tối đa 1000 ký tự.
     */
    @NotBlank(message = "Biện pháp xử lý không được để trống")
    @Size(max = 1000, message = "Biện pháp xử lý không được vượt quá 1000 ký tự")
    private String handlingMeasure;

    /**
     * Diễn giải chi tiết thêm — không bắt buộc, tối đa 1000 ký tự.
     */
    @Size(max = 1000, message = "Diễn giải không được vượt quá 1000 ký tự")
    private String note;
}
