package vn.nguongocso.organization.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Kết quả gán hàng loạt địa bàn.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignAreasResult {

	private long assignedCount;

	private List<AssignedAreaResponse> assigned;

	private String message;
}
