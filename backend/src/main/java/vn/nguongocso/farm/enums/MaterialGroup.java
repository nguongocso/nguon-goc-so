package vn.nguongocso.farm.enums;

import lombok.Getter;

/** Phân loại nhóm vật tư đầu vào. */
@Getter
public enum MaterialGroup {
    FERTILIZER("Phân bón"), // Phân bón

    PESTICIDE("Thuốc bảo vệ thực vật"), // Thuốc bảo vệ thực vật

    BIOLOGICAL("Chế phẩm sinh học"), // Chế phẩm sinh học

    OTHER("Khác"); // Khác

    private final String displayName;

    /** Khởi tạo nhóm vật tư đầu vào. */
    MaterialGroup(String displayName) {
        this.displayName = displayName;
    }
}
