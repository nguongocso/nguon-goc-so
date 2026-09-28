package vn.nguongocso.farm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import vn.nguongocso.farm.enums.MilestoneReminderStatus;

/**
 * Thông tin nhắc việc theo mốc canh tác.
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneReminderResponse {
    private UUID id;

    private UUID lotId;

    private String lotName;

    private Long milestoneId;

    private String milestoneName;

    private String activityType;

    private Integer overdueDays;

    private LocalDate expectedDate;

    private MilestoneReminderStatus status;

    private LocalDate reminderDate;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
}
