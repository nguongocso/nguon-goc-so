package vn.nguongocso.backup.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Yêu cầu tạo hoặc cập nhật lịch sao lưu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupScheduleRequest {
    @NotBlank(message = "Lịch chạy cron không được để trống")
    @Size(max = 100, message = "Lịch chạy cron tối đa 100 ký tự")
    private String cronExpression;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @NotNull(message = "Trạng thái kích hoạt không được để trống")
    @JsonProperty("isActive")
    private Boolean isActive;
}
