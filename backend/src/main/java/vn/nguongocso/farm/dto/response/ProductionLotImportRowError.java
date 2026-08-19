package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Thông tin lỗi của một dòng dữ liệu khi nhập tệp.
 */
@Getter
@Builder
public class ProductionLotImportRowError {
    private Integer rowNumber; // Số dòng trong tệp xảy ra lỗi.

    private String reason; // Lý do dòng dữ liệu không hợp lệ.

}