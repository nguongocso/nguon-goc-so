package vn.nguongocso.farm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả quét mốc canh tác quá hạn (NCL-03-CN-007).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneScanResult {

    /** Số lượng lô sản xuất đã được quét. */
    private int scannedLotsCount;

    /** Số lượng nhắc việc mới được tạo. */
    private int remindersCreatedCount;

    /** Thông điệp tóm tắt kết quả. */
    private String message;
}
