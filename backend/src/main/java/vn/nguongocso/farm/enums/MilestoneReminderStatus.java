package vn.nguongocso.farm.enums;

/**
 * Trạng thái của nhắc việc ghi nhật ký theo mốc canh tác (NCL-03-CN-007).
 */
public enum MilestoneReminderStatus {
    /** Nhắc việc đang mở (chưa ghi nhật ký cho mốc). */
    OPEN,

    /** Nhắc việc đã hoàn thành (tự đóng khi mốc được ghi nhật ký). */
    COMPLETED
}
