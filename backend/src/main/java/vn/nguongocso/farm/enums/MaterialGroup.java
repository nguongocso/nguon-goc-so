package vn.nguongocso.farm.enums;

import lombok.Getter;

/**
 * Phân loại nhóm vật tư đầu vào.
 */
@Getter
public enum MaterialGroup {
	FERTILIZER("Phân bón"),
	PESTICIDE("Thuốc bảo vệ thực vật"),
	BIOLOGICAL("Chế phẩm sinh học"),
	OTHER("Khác");

	private final String displayName;

	MaterialGroup(String displayName) {
		this.displayName = displayName;
	}
}
