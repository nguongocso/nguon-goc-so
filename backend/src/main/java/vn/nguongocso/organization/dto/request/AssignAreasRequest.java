package vn.nguongocso.organization.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request gán hàng loạt địa bàn cho một tài khoản (all-or-nothing).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssignAreasRequest {

	@NotEmpty(message = "Danh sách địa bàn không được để trống.")
	private List<UUID> unitIds;
}
