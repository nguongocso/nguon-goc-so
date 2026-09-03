package vn.nguongocso.organization.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.organization.dto.request.AssignAreasRequest;
import vn.nguongocso.organization.dto.request.UpdateOrganizationDivisionsRequest;
import vn.nguongocso.organization.dto.response.AssignAreasResult;
import vn.nguongocso.organization.dto.response.AssignedAreaResponse;
import vn.nguongocso.organization.dto.response.RegulatorUserResponse;
import vn.nguongocso.organization.dto.response.UnassignAreaResult;

/**
 * Gán / gỡ / xem địa bàn quản lý của tài khoản (NCL-670 / NCL-743).
 */
public interface AreaAssignmentService {

	/**
	 * Danh sách tài khoản cán bộ quản lý ngành (VT-05) phục vụ màn hình gán.
	 *
	 * @param keyword từ khoá tìm theo fullName/username (không phân biệt hoa
	 *                thường, có thể rỗng)
	 * @param pageable phân trang
	 */
	PageResponse<RegulatorUserResponse> listRegulators(String keyword, Pageable pageable);

	/** Địa bàn đã gán của một tài khoản bất kỳ (VT-01 xem). */
	List<AssignedAreaResponse> getAssignedAreas(UUID userId);

	/** Địa bàn đã gán của người dùng hiện tại (VT-05 tự xem). */
	List<AssignedAreaResponse> getMyAreas(CustomUserDetails currentUser);

	/**
	 * Gán hàng loạt địa bàn (all-or-nothing), validate đúng thứ tự V1→V5 theo
	 * NCL-739 §3.4.
	 */
	AssignAreasResult assignAreas(CustomUserDetails operator, UUID userId, AssignAreasRequest request);

	/**
	 * Gỡ một địa bàn khỏi tài khoản.
	 */
	UnassignAreaResult unassignArea(CustomUserDetails operator, UUID userId, UUID unitId);

	/**
	 * Cập nhật mapping tổ chức → đơn vị hành chính (phục vụ lọc báo cáo).
	 */
	void updateOrganizationDivisions(
			CustomUserDetails operator,
			UUID organizationId,
			UpdateOrganizationDivisionsRequest request);
}
