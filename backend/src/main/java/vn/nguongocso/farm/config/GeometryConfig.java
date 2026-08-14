package vn.nguongocso.farm.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình GeometryFactory cho module vùng trồng.
 */
@Configuration
public class GeometryConfig {

	/**
	 * Khởi tạo GeometryFactory với SRID 4326.
	 *
	 * @return GeometryFactory
	 */
	@Bean
	public GeometryFactory geometryFactory() {
		return new GeometryFactory(new PrecisionModel(), 4326);
	}
}