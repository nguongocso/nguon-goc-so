package vn.nguongocso.farm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi thông tin nhắc việc theo mốc canh tác bắt buộc (NCL-03-CN-007).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneReminderResponse {

    /** ID của nhắc việc. */
    private UUID id;

    /** ID của lô sản xuất. */
    private UUID lotId;

    /** Tên lô sản xuất. */
    private String lotName;

    /** ID của mốc canh tác. */
    private Long milestoneId;

    /** Tên mốc canh tác. */
    private String milestoneName;

    /** Loại hoạt động canh tác (FERTILIZING, WATERING, PESTICIDE, ...). */
    private String activityType;

    /** Số ngày quá hạn. */
    private Integer overdueDays;

    /** Ngày dự kiến hoàn thành mốc. */
    private LocalDate expectedDate;

    /** Trạng thái nhắc việc (OPEN / COMPLETED). */
    private MilestoneReminderStatus status;

    /** Ngày phát nhắc việc. */
    private LocalDate reminderDate;

    /** Thời điểm tự đóng khi đã ghi nhật ký. */
    private LocalDateTime completedAt;

    /** Thời điểm tạo nhắc việc. */
    private LocalDateTime createdAt;
}
