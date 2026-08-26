package vn.nguongocso.farm.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.enums.MaterialGroup;

/**
 * DTO phản hồi thông tin chi tiết vật tư đầu vào.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputMaterialResponse {

	private UUID id;

	private String name;

	private MaterialGroup materialGroup;

	private String materialGroupDisplayName;

	private String activeIngredient;

	private String unit;

	private Integer quarantineDays;

	private Boolean applyToAllCrops;

	private Set<ProductCategoryResponse> applicableCropTypes;

	private String referenceSource;

	private Boolean isActive;

	private UUID createdBy;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
