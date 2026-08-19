package vn.nguongocso.trace.enums;

/**
 * Trạng thái của một mã truy xuất.
 */
public enum TraceCodeStatus {
    INACTIVE, // Chưa kích hoạt

    ACTIVE, // Đã kích hoạt

    SUSPECT, // Nghi vấn

    LOCKED, // Đã khóa

    RECALLED // Đã thu hồi
}