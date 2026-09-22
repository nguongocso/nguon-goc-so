package vn.nguongocso.report.enums;

/** Trạng thái của từng chỉ số giám sát riêng lẻ. */
public enum MetricStatus {
    NORMAL, // Bình thường

    WARNING, // Cảnh báo

    CRITICAL, // Nghiêm trọng

    INSUFFICIENT_DATA // Thiếu dữ liệu
}
