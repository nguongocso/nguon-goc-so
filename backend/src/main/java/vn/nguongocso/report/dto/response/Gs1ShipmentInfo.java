package vn.nguongocso.report.dto.response;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thông tin lô hàng (Shipment) được xuất theo lược đồ GS1 mô phỏng.
 *
 * <p>
 * Một số trường trong ví dụ tham chiếu không tồn tại trong domain hiện tại
 * ({@code codeValue}, {@code unit}, {@code productCategory} trực tiếp trên
 * Shipment). Trong implementation này:
 * </p>
 *
 * <ul>
 * <li>{@code productCategory} được lấy từ {@code ProductionLot.productCategory}.</li>
 * <li>{@code unit} được lấy từ {@code ProductionLot.expectedQuantityUnit}.</li>
 * <li>{@code codeValues} là danh sách mã truy xuất của Shipment
 * ({@code TraceCode.codeValue}), thay cho {@code codeValue} đơn lẻ.</li>
 * </ul>
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gs1ShipmentInfo {

    /** ID lô hàng. */
    @JacksonXmlProperty(localName = "id")
    private UUID id;

    /** Tên lô hàng (Shipment.name). */
    @JacksonXmlProperty(localName = "name")
    private String name;

    /** Danh sách mã truy xuất (TraceCode.codeValue) – best effort. */
    @JacksonXmlElementWrapper(localName = "codeValues")
    @JacksonXmlProperty(localName = "codeValue")
    private List<String> codeValues;

    /** Danh mục sản phẩm (ProductionLot.productCategory.name). */
    @JacksonXmlProperty(localName = "productCategory")
    private String productCategory;

    /** Số lượng khai báo (Shipment.totalQuantity). */
    @JacksonXmlProperty(localName = "totalQuantity")
    private Long totalQuantity;

    /** Đơn vị (ProductionLot.expectedQuantityUnit). */
    @JacksonXmlProperty(localName = "unit")
    private String unit;

    /** Trạng thái lô hàng (Shipment.status). */
    @JacksonXmlProperty(localName = "status")
    private String status;

    /** Đơn vị tổ chức sở hữu lô hàng. */
    @JacksonXmlProperty(localName = "organization")
    private OrganizationInfo organization;

    /**
     * Thông tin đơn vị tổ chức (Organization).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizationInfo {

        /** ID tổ chức. */
        @JacksonXmlProperty(localName = "id")
        private UUID id;

        /** Tên tổ chức. */
        @JacksonXmlProperty(localName = "name")
        private String name;

        /** Mã tổ chức. */
        @JacksonXmlProperty(localName = "code")
        private String code;
    }
}