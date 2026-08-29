package vn.nguongocso.certification.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO tạo mới đơn vị kiểm nghiệm trong danh mục dùng chung.
 */
@Getter
@Setter
public class CreateTestingUnitRequest {

    @NotBlank(message = "Tên đơn vị kiểm nghiệm không được để trống.")
    @Size(max = 255, message = "Tên đơn vị kiểm nghiệm không được vượt quá 255 ký tự.")
    private String name;

    @NotBlank(message = "Mã công nhận không được để trống.")
    @Size(max = 100, message = "Mã công nhận không được vượt quá 100 ký tự.")
    private String accreditationCode;

    @Size(max = 500, message = "Thông tin liên hệ không được vượt quá 500 ký tự.")
    private String contactInfo;

    /**
     * Ngày hết hạn công nhận (tuỳ chọn, định dạng YYYY-MM-DD).
     */
    private LocalDate accreditationExpiryDate;

    /**
     * Trạng thái hiệu lực; mặc định true khi tạo mới nếu không truyền.
     */
    private Boolean isActive;
}