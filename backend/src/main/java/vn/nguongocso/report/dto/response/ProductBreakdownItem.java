package vn.nguongocso.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Thống kê sản lượng theo loại nông sản. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBreakdownItem {

    private String productCategoryName;

    private Long shipmentCount;

    private Long totalQuantity;
}