package vn.nguongocso.farm.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.nguongocso.farm.entity.ProductionLot;

/**
 * Đại diện cho một dòng dữ liệu hợp lệ đã được xác thực từ tệp nhập.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidImportRow {
    private ProductionLot productionLot;

    private ProductionLotImportRow row;
}