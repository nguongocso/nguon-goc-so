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

/** Gán, gỡ và xem địa bàn quản lý của tài khoản. */
public interface AreaAssignmentService {
    /** Danh sách tài khoản cán bộ quản lý ngành (VT-05) phục vụ màn hình gán. */
    PageResponse<RegulatorUserResponse> listRegulators(String keyword, Pageable pageable);

    /** Địa bàn đã gán của một tài khoản bất kỳ. */
    List<AssignedAreaResponse> getAssignedAreas(UUID userId);

    /** Địa bàn đã gán của người dùng hiện tại. */
    List<AssignedAreaResponse> getMyAreas(CustomUserDetails currentUser);

    /** Gán hàng loạt địa bàn cho tài khoản. */
    AssignAreasResult assignAreas(CustomUserDetails operator, UUID userId, AssignAreasRequest request);

    /** Gỡ một địa bàn khỏi tài khoản. */
    UnassignAreaResult unassignArea(CustomUserDetails operator, UUID userId, UUID unitId);

    /** Cập nhật mapping tổ chức và đơn vị hành chính. */
    void updateOrganizationDivisions(
            CustomUserDetails operator,
            UUID organizationId,
            UpdateOrganizationDivisionsRequest request);
}
