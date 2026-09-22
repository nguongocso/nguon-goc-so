package vn.nguongocso.farm.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Danh mục đơn vị diện tích vùng trồng. */
public enum AreaUnit {
    HA(BigDecimal.ONE), // Héc-ta

    KM2(BigDecimal.valueOf(0.01)); // Ki-lô-mét vuông

    private final BigDecimal unitsPerHectare;

    /** Khởi tạo đơn vị diện tích với hệ số quy đổi. */
    AreaUnit(BigDecimal unitsPerHectare) {
        this.unitsPerHectare = unitsPerHectare;
    }

    /** Quy đổi giá trị diện tích sang héc-ta. */
    public BigDecimal toHectares(BigDecimal value) {
        return value.divide(unitsPerHectare, 4, RoundingMode.HALF_UP);
    }
}
