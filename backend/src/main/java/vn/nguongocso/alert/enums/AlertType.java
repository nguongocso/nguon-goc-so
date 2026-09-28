package vn.nguongocso.alert.enums;

/** Loại sự kiện kích hoạt cảnh báo. */
public enum AlertType {
    SCAN_ANOMALY, // Bất thường khi quét

    CERT_EXPIRING, // Chứng nhận sắp hết hạn

    CERT_EXPIRED, // Chứng nhận đã hết hạn

    INSPECTION_EXPIRING, // Kết quả kiểm nghiệm sắp hết hiệu lực (NCL-11-CN-004)

    INSPECTION_EXPIRED // Kết quả kiểm nghiệm đã hết hiệu lực (NCL-11-CN-004)
}