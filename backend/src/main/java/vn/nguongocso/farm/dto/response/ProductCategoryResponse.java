package vn.nguongocso.farm.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi thông tin loại nông sản.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryResponse {
	private UUID id;

	private String name;

	private String group;

	private String description;

	private Boolean isActive;

	// ===== Ngưỡng bảo quản (NCL-05-CN-007) =====
	private Double tempMin;

	private Double tempMax;

	private Double humidityMin;

	private Double humidityMax;
}
