package vn.nguongocso.farm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả quét mốc canh tác quá hạn.
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneScanResult {
    private int scannedLotsCount;

    private int remindersCreatedCount;

    private String message;
}
