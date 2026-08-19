package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO yêu cầu đính chính (sửa lỗi) sự kiện đóng gói.
 *
 * @author Team WEB 1
 */

@Getter
@Setter
public class CorrectPackagingEventRequest {
    @NotBlank(message = "Quy cách đóng gói đính chính không được để trống")
    @Size(max = 255, message = "Quy cách đóng gói không được vượt quá 255 ký tự")
    private String packagingSpecification;

    @NotNull(message = "Vui lòng chọn ngày đóng gói đính chính")
    private LocalDate packagingDate;

    @NotBlank(message = "Lý do đính chính không được để trống")
    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String correctionReason;

    private Double latitude;
    private Double longitude;
}
