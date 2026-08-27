package vn.nguongocso.farm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.farm.enums.AreaUnit;

/**
 * DTO phản hồi sau khi tạo vùng trồng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmAreaResponse {
	private UUID id;

	private String name;

	private UUID organizationId;

	private String organizationName;

	private UUID cropTypeId;

	private String cropTypeName;

	private Double latitude;

	private Double longitude;

	private BigDecimal area;

	private AreaUnit areaUnit;

	private Boolean isActive;

	private Long associatedLotsCount;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}