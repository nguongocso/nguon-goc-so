package vn.nguongocso.export.enums;

import lombok.Getter;

/** Nhóm trường dữ liệu trong hồ sơ truy xuất nguồn gốc. */
@Getter
public enum ProfileFieldGroup {
    /** Nhóm thông tin tổ chức / Hợp tác xã */
    ORGANIZATION("Thông tin tổ chức / Hợp tác xã"),

    /** Nhóm thông tin vùng trồng */
    FARM_AREA("Thông tin vùng trồng"),

    /** Nhóm thông tin lô sản xuất */
    PRODUCTION_LOT("Thông tin lô sản xuất"),

    /** Nhóm thông tin lô hàng vận chuyển */
    SHIPMENT("Thông tin lô hàng vận chuyển"),

    /** Nhóm thông tin nhật ký canh tác */
    FARM_LOG("Nhật ký canh tác"),

    /** Nhóm kết quả kiểm nghiệm chất lượng */
    INSPECTION("Kết quả kiểm nghiệm"),

    /** Nhóm chứng nhận tiêu chuẩn */
    CERTIFICATION("Chứng nhận chất lượng"),

    /** Nhóm dòng sự kiện chuỗi cung ứng */
    CHAIN_EVENT("Dòng sự kiện chuỗi cung ứng");

    private final String label;

    ProfileFieldGroup(String label) {
        this.label = label;
    }
}
