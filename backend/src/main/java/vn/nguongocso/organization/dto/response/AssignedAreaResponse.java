package vn.nguongocso.organization.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Một địa bàn đã gán cho tài khoản.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedAreaResponse {

	/** ID bản ghi user_area_assignments. */
	private UUID assignmentId;

	/** ID đơn vị hành chính. */
	private UUID unitId;

	private String unitCode;

	private String unitName;

	/** PROVINCE hoặc COMMUNE. */
	private String unitLevel;

	/** Đơn vị gốc cấp tỉnh tương ứng (chính nó nếu đơn vị là cấp tỉnh). */
	private UUID provinceId;

	private String provinceName;

	/** Thời điểm gán. */
	private LocalDateTime assignedAt;
}
