package vn.nguongocso.organization.dto.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật mapping tổ chức → đơn vị hành chính (phục vụ lọc báo cáo
 * theo địa bàn). Cả hai trường đều có thể null để bỏ mapping.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationDivisionsRequest {

	private UUID provinceId;

	private UUID communeId;
}
