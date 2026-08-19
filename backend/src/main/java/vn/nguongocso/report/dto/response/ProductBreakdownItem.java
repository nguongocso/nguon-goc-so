package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thống kê sản lượng và số lô hàng theo từng loại nông sản.
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductBreakdownItem {
    private String productCategoryName;

    private Long shipmentCount;

    private Long totalQuantity;
}