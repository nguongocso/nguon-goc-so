package vn.nguongocso.farm.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import vn.nguongocso.farm.entity.ProductionLot;

/**
 * Dòng nhập lô sản xuất đã qua xác thực.
*/
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidImportRow {
    private ProductionLot productionLot;

    private ProductionLotImportRow row;
}