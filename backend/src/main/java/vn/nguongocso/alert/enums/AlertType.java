package vn.nguongocso.alert.enums;

/** Loại sự kiện kích hoạt cảnh báo. */
public enum AlertType {
    SCAN_ANOMALY, // Bất thường khi quét

    CERT_EXPIRING, // Chứng nhận sắp hết hạn

    CERT_EXPIRED // Chứng nhận đã hết hạn
}