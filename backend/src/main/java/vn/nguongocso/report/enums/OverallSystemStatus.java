package vn.nguongocso.report.enums;

/** Trạng thái tổng thể của hệ thống giám sát. */
public enum OverallSystemStatus {
    HEALTHY, // Hoạt động tốt

    WARNING, // Cảnh báo

    CRITICAL, // Nghiêm trọng

    INSUFFICIENT_DATA // Thiếu dữ liệu
}
