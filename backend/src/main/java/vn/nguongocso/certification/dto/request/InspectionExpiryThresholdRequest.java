package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu cập nhật ngưỡng cảnh báo hết hiệu lực kiểm nghiệm (NCL-11-CN-004).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionExpiryThresholdRequest {

    @NotNull(message = "Ngưỡng cảnh báo không được để trống")
    @Min(value = 1, message = "Ngưỡng cảnh báo phải lớn hơn hoặc bằng 1 ngày")
    @Max(value = 365, message = "Ngưỡng cảnh báo không được vượt quá 365 ngày")
    private Integer warningThresholdDays;
}
