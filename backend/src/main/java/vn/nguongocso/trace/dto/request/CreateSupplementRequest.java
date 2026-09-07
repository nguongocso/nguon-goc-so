package vn.nguongocso.trace.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload tạo yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 *
 * <p>Chỉ Quản lý hợp tác xã (VT-02) được phép. Tổ chức lấy từ người dùng
 * đang đăng nhập (QTN-01).</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateSupplementRequest {

    @NotNull(message = "Số lượng đề nghị không được để trống.")
    @Min(value = 1, message = "Số lượng đề nghị phải lớn hơn 0.")
    private Long requestedQuantity;

    @NotBlank(message = "Lý do đề nghị không được để trống.")
    @Size(max = 1000, message = "Lý do đề nghị không được vượt quá 1000 ký tự.")
    private String reason;

    @NotEmpty(message = "Phải chọn ít nhất một sự kiện thu hoạch hoặc sơ chế làm bằng chứng.")
    private List<UUID> evidenceEventIds;
}
