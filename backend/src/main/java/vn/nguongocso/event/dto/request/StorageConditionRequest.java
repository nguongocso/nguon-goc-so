package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO yêu cầu ghi mốc điều kiện bảo quản khi vận chuyển.
 */
@Getter
@Setter
public class StorageConditionRequest {

    @NotBlank(message = "Mã truy xuất không được để trống")
    private String codeValue;

    @NotNull(message = "Nhiệt độ không được để trống")
    private Double temperature;

    @NotNull(message = "Độ ẩm không được để trống")
    @Min(value = 0, message = "Độ ẩm phải từ 0 đến 100%")
    @Max(value = 100, message = "Độ ẩm phải từ 0 đến 100%")
    private Double humidity;

    private LocalDateTime recordedAt;
}