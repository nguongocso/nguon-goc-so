package vn.nguongocso.farm.dto.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.enums.MaterialGroup;

/**
 * DTO yêu cầu cập nhật thông tin vật tư đầu vào.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInputMaterialRequest {

	@NotBlank(message = "Tên vật tư không được để trống")
	@Size(max = 255, message = "Tên vật tư không vượt quá 255 ký tự")
	private String name;

	@NotNull(message = "Nhóm vật tư không được để trống")
	private MaterialGroup materialGroup;

	@Size(max = 255, message = "Tên hoạt chất không vượt quá 255 ký tự")
	private String activeIngredient;

	@NotBlank(message = "Đơn vị tính không được để trống")
	@Size(max = 50, message = "Đơn vị tính không vượt quá 50 ký tự")
	private String unit;

	@Min(value = 0, message = "Thời gian cách ly phải là số nguyên không âm")
	private Integer quarantineDays;

	private Boolean applyToAllCrops;

	private Set<UUID> applicableCropTypeIds;

	private String referenceSource;

	private List<String> imageUrls;

	private Boolean isActive;
}
