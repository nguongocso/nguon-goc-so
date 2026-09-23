package vn.nguongocso.export.enums;

import lombok.Getter;

/** Nhóm trường dữ liệu trong hồ sơ truy xuất nguồn gốc. */
@Getter
public enum ProfileFieldGroup {
    ORGANIZATION("Thông tin tổ chức / Hợp tác xã"),

    FARM_AREA("Thông tin vùng trồng"),

    PRODUCTION_LOT("Thông tin lô sản xuất"),

    SHIPMENT("Thông tin lô hàng vận chuyển"),

    FARM_LOG("Nhật ký canh tác"),

    INSPECTION("Kết quả kiểm nghiệm"),

    CERTIFICATION("Chứng nhận chất lượng"),

    CHAIN_EVENT("Dòng sự kiện chuỗi cung ứng");

    private final String label;

    ProfileFieldGroup(String label) {
        this.label = label;
    }
}
