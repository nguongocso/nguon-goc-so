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
     * @param keyword  từ khoá tìm theo fullName/username (không phân biệt hoa thường, có thể rỗng)
     * @param pageable phân trang
     * @return danh sách phân trang cán bộ quản lý ngành
     */
    PageResponse<RegulatorUserResponse> listRegulators(String keyword, Pageable pageable);

    /**
     * Địa bàn đã gán của một tài khoản bất kỳ (VT-01 xem).
     *
     * @param userId ID người dùng
     * @return danh sách địa bàn đã gán
     */
    List<AssignedAreaResponse> getAssignedAreas(UUID userId);

    /**
     * Địa bàn đã gán của người dùng hiện tại (VT-05 tự xem).
     *
     * @param currentUser thông tin người dùng hiện tại
     * @return danh sách địa bàn đã gán của người dùng hiện tại
     */
    List<AssignedAreaResponse> getMyAreas(CustomUserDetails currentUser);

    /**
     * Gán hàng loạt địa bàn (all-or-nothing), validate đúng thứ tự V1→V5 theo NCL-739 §3.4.
     *
     * @param operator    người thực hiện thao tác
     * @param userId      ID người dùng được gán
     * @param request     danh sách địa bàn cần gán
     * @return kết quả gán địa bàn
     */
    AssignAreasResult assignAreas(CustomUserDetails operator, UUID userId, AssignAreasRequest request);

    /**
     * Gỡ một địa bàn khỏi tài khoản.
     *
     * @param operator người thực hiện thao tác
     * @param userId   ID người dùng
     * @param unitId   ID đơn vị hành chính cần gỡ
     * @return kết quả gỡ địa bàn
     */
    UnassignAreaResult unassignArea(CustomUserDetails operator, UUID userId, UUID unitId);

    /**
     * Cập nhật mapping tổ chức → đơn vị hành chính (phục vụ lọc báo cáo).
     *
     * @param operator       người thực hiện thao tác
     * @param organizationId ID tổ chức
     * @param request        thông tin cập nhật
     */
    void updateOrganizationDivisions(
            CustomUserDetails operator,
            UUID organizationId,
            UpdateOrganizationDivisionsRequest request);
}
