package vn.nguongocso.farm.enums;

/** Trạng thái vòng đời xử lý phản ánh của người tiêu dùng. */
public enum ProductFeedbackStatus {
    NEW, // Mới

    IN_PROGRESS, // Đang xử lý

    CLOSED, // Đã đóng

    ESCALATED_TO_RECALL // Đã nâng cấp thành thu hồi
}
