package vn.nguongocso.alert.enums;

import lombok.Getter;

/**
 * Danh mục các loại nguồn cảnh báo tổng hợp (NCL-08-CN-016).
 */
@Getter
public enum AggregateAlertType {
    SCAN_ANOMALY("Tem quét bất thường"),
    CERT_EXPIRING("Chứng nhận sắp hết hạn"),
    CERT_EXPIRED("Chứng nhận đã hết hạn"),
    INSPECTION_EXPIRING("Kết quả kiểm nghiệm sắp hết hiệu lực"),
    INSPECTION_EXPIRED("Kết quả kiểm nghiệm đã hết hiệu lực"),
    UNPROCESSED_FEEDBACK("Phản ánh chưa xử lý"),
    CODE_RANGE_QUOTA("Hạn mức dải mã sắp hết"),
    OVERDUE_MILESTONE("Mốc canh tác quá hạn"),
    OPEN_RECALL_CASE("Vụ việc thu hồi đang mở");

    private final String displayName;

    AggregateAlertType(String displayName) {
        this.displayName = displayName;
    }
}
