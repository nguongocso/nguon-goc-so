package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO yêu cầu đính chính (sửa lỗi) sự kiện sơ chế và phân loại.
 *
 * @author NGUON-GOC-SO Team
 */

@Getter
@Setter
public class CorrectPreprocessingEventRequest {

    @NotNull(message = "Vui lòng nhập khối lượng đưa vào sơ chế")
    @Positive(message = "Khối lượng vào sơ chế phải lớn hơn 0")
    private Double inputQuantity;

    @NotNull(message = "Vui lòng nhập khối lượng sau sơ chế")
    @PositiveOrZero(message = "Khối lượng sau sơ chế phải lớn hơn hoặc bằng 0")
    private Double outputQuantity;

    @Size(max = 100, message = "Hạng phân loại không được vượt quá 100 ký tự")
    private String grade;

    @Size(max = 500, message = "Mô tả cách sơ chế không được vượt quá 500 ký tự")
    private String processingMethod;

    @NotNull(message = "Vui lòng chọn ngày sơ chế đính chính")
    private LocalDate preprocessingDate;

    @NotBlank(message = "Lý do đính chính không được để trống")
    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String correctionReason;

    private Double latitude;
    private Double longitude;
}
