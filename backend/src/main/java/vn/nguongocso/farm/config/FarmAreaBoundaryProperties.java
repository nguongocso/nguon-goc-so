package vn.nguongocso.farm.config;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình nghiệp vụ khi cập nhật ranh giới vùng trồng.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.farm-area")
public class FarmAreaBoundaryProperties {

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal boundaryDeviationThresholdPercent = new BigDecimal("30.0");
}
