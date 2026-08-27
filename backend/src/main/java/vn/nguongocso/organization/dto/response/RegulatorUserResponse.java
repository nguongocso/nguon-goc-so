package vn.nguongocso.organization.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tùy chọn tài khoản cán bộ quản lý ngành (VT-05) hiển thị trên màn hình gán
 * địa bàn.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegulatorUserResponse {

	private UUID userId;

	private String username;

	private String fullName;

	private String email;

	private String phone;

	private String organizationName;
}
