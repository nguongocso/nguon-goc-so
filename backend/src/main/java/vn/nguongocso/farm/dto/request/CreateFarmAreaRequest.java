package vn.nguongocso.farm.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.enums.AreaUnit;

/**
 * DTO yêu cầu tạo mới vùng trồng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateFarmAreaRequest {
	@NotBlank(message = "Tên vùng trồng không được để trống")
	@Size(max = 255, message = "Tên vùng trồng không được vượt quá 255 ký tự")
	private String name;

	@NotNull(message = "Loại cây trồng không được để trống")
	private UUID cropType;

	@NotNull(message = "Vĩ độ không được để trống")
	private Double latitude;

	@NotNull(message = "Kinh độ không được để trống")
	private Double longitude;

	@NotNull(message = "Diện tích không được để trống")
	@DecimalMin(value = "0.01", inclusive = true, message = "Diện tích phải lớn hơn 0")
	private BigDecimal area;

	private AreaUnit areaUnit;
}