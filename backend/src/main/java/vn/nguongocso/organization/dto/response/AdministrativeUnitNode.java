package vn.nguongocso.organization.dto.response;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node cây đơn vị hành chính trả về cho màn hình gán địa bàn và bộ lọc báo cáo.
 *
 * <p>
 * Chỉ có 1 mức lồng (tỉnh chứa xã/phường); {@code children} luôn khác null,
 * rỗng khi không có con.
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AdministrativeUnitNode {

	private UUID id;

	private String code;

	private String name;

	/** Cấp đơn vị: PROVINCE hoặc COMMUNE. */
	private String level;

	@Builder.Default
	private List<AdministrativeUnitNode> children = List.of();
}
