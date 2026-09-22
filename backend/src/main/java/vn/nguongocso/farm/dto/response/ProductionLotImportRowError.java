package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Thông tin lỗi của một dòng dữ liệu khi nhập tệp.
*/
@Getter
@Builder
public class ProductionLotImportRowError {
    private Integer rowNumber;

    private String reason;
}