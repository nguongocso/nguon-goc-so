package vn.nguongocso.organization.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Kết quả gỡ một địa bàn khỏi tài khoản (chứa thông báo cho toast).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UnassignAreaResult {

	private String message;
}
