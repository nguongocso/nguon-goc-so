package vn.nguongocso.farm.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Danh mục đơn vị diện tích được phép sử dụng khi tạo vùng trồng.
 * Diện tích luôn được lưu trong hệ thống theo đơn vị héc-ta (HA),
 * nên mỗi đơn vị cần khai báo hệ số quy đổi về héc-ta.
 */
public enum AreaUnit {

	HA(BigDecimal.ONE), // Héc-ta
	KM2(BigDecimal.valueOf(0.01)); // Kilômét vuông

	private final BigDecimal unitsPerHectare;

	/**
	 * Khởi tạo đơn vị diện tích với hệ số quy đổi về héc-ta.
	 *
	 * @param unitsPerHectare Hệ số quy đổi từ đơn vị hiện tại sang héc-ta.
	 */
	AreaUnit(BigDecimal unitsPerHectare) {
		this.unitsPerHectare = unitsPerHectare;
	}

	/**
	 * Quy đổi một giá trị diện tích theo đơn vị hiện tại sang héc-ta.
	 */
	public BigDecimal toHectares(BigDecimal value) {
		return value.divide(unitsPerHectare, 4, RoundingMode.HALF_UP);
	}
}