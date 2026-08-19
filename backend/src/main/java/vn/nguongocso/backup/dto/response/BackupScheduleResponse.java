package vn.nguongocso.backup.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import vn.nguongocso.backup.entity.BackupSchedule;

import java.time.LocalDateTime;

/**
 * Phản hồi chi tiết lịch trình sao lưu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupScheduleResponse {
    private Integer id;

    private String cronExpression;

    private String description;

    @JsonProperty("isActive")
    private boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String updatedBy;

    /**
     * Chuyển đổi từ thực thể BackupSchedule sang BackupScheduleResponse.
     *
     * @param schedule Thực thể BackupSchedule cần chuyển đổi.
     * @return Đối tượng BackupScheduleResponse tương ứng.
     */
    public static BackupScheduleResponse fromEntity(BackupSchedule schedule) {
        if (schedule == null)
            return null;
        return BackupScheduleResponse.builder()
                .id(schedule.getId())
                .cronExpression(schedule.getCronExpression())
                .description(schedule.getDescription())
                .isActive(schedule.isActive())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .updatedBy(schedule.getUpdatedBy() != null ? schedule.getUpdatedBy().getFullName() : "Hệ thống")
                .build();
    }
}
