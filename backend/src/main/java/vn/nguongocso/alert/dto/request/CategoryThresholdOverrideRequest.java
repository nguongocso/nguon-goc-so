package vn.nguongocso.alert.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu tạo mới hoặc cập nhật cấu hình ghi đè ngưỡng theo danh mục nông sản (NCL-08-CN-014).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryThresholdOverrideRequest {

    @NotNull(message = "Danh mục nông sản không được để trống")
    private UUID productCategoryId;

    @NotNull(message = "Số lượt quét tối đa mỗi giờ không được để trống")
    @Min(value = 1, message = "Số lượt quét tối đa mỗi giờ phải lớn hơn hoặc bằng 1")
    private Integer maxScansPerHour;

    @NotNull(message = "Số lượt quét tối đa mỗi ngày không được để trống")
    @Min(value = 1, message = "Số lượt quét tối đa mỗi ngày phải lớn hơn hoặc bằng 1")
    private Integer maxScansPerDay;

    @NotNull(message = "Khoảng cách tối đa cho phép không được để trống")
    @DecimalMin(value = "0.0", message = "Khoảng cách tối đa cho phép phải không âm")
    private Double maxDistanceKmPer30Min;

    @NotNull(message = "Thời gian tối thiểu giữa các lượt quét không được để trống")
    @Min(value = 0, message = "Thời gian tối thiểu giữa các lượt quét phải không âm")
    private Integer minTimeBetweenScansMinutes;

    @NotNull(message = "Thời hạn kích hoạt bình thường không được để trống")
    @Min(value = 0, message = "Thời hạn kích hoạt bình thường phải không âm")
    private Integer activationAgeDays;
}
