package vn.nguongocso.organization.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.dto.request.AddMemberRequest;
import vn.nguongocso.auth.dto.request.AssignRoleRequest;
import vn.nguongocso.auth.dto.response.OrganizationUserResponse;
import vn.nguongocso.organization.dto.request.CreateOrganizationRequest;
import vn.nguongocso.organization.dto.request.OrganizationUpdateRequest;
import vn.nguongocso.organization.dto.response.AvailableUserResponse;
import vn.nguongocso.organization.dto.response.CreateOrganizationMemberResponse;
import vn.nguongocso.organization.dto.response.OrganizationDetailResponse;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.dto.response.OrganizationResponse;
import vn.nguongocso.organization.dto.response.RecipientOrganizationResponse;

/**
 * Quản lý tổ chức và thông tin thành viên liên quan.
 */
public interface OrganizationService {

    /**
     * Tạo tổ chức mới.
     *
     * @param request thông tin tạo tổ chức
     * @return thông tin tổ chức vừa tạo
     */
    OrganizationResponse createOrganization(CreateOrganizationRequest request);

    /**
     * Lấy hồ sơ tổ chức hiện tại.
     *
     * @return hồ sơ tổ chức hiện tại
     */
    OrganizationProfileResponse getCurrentOrganizationProfile();

    /**
     * Cập nhật hồ sơ tổ chức hiện tại.
     *
     * @param request thông tin cập nhật
     * @return hồ sơ tổ chức sau cập nhật
     */
    OrganizationProfileResponse updateCurrentOrganization(OrganizationUpdateRequest request);

    /**
     * Cập nhật tổ chức theo ID.
     *
     * @param orgId   ID tổ chức
     * @param request thông tin cập nhật
     * @return hồ sơ tổ chức sau cập nhật
     */
    OrganizationProfileResponse updateOrganizationById(UUID orgId, OrganizationUpdateRequest request);

    /**
     * Lấy danh sách tất cả tổ chức.
     *
     * @return danh sách các tổ chức
     */
    List<OrganizationResponse> getAllOrganizations();

    /**
     * Lấy danh sách tổ chức ACTIVE trừ tổ chức hiện tại cho phiếu bàn giao.
     *
     * @return danh sách các tổ chức nhận
     */
    List<RecipientOrganizationResponse> getRecipientOrganizations();

    /**
     * Lấy chi tiết tổ chức.
     *
     * @param organizationId ID tổ chức
     * @return chi tiết tổ chức và danh sách thành viên
     */
    OrganizationDetailResponse getOrganizationDetail(UUID organizationId);

    /**
     * Thêm thành viên vào tổ chức.
     *
     * @param organizationId ID tổ chức
     * @param request        thông tin thành viên
     * @return kết quả thêm thành viên
     */
    CreateOrganizationMemberResponse addMember(
            UUID organizationId,
            AddMemberRequest request);

    /**
     * Gán vai trò cho thành viên.
     *
     * @param request thông tin gán vai trò
     * @return thông tin thành viên sau khi gán vai trò
     */
    OrganizationUserResponse assignRole(AssignRoleRequest request);

    /**
     * Lấy danh sách thành viên của tổ chức hiện tại.
     *
     * @return danh sách thành viên
     */
    List<OrganizationUserResponse> getMembersOfCurrentOrganization();

    /**
     * Lấy danh sách người dùng có thể thêm vào tổ chức.
     *
     * @param organizationId ID tổ chức
     * @return danh sách người dùng có sẵn
     */
    List<AvailableUserResponse> getAvailableUsersForOrganization(UUID organizationId);

    /**
     * Thêm người dùng hiện có vào tổ chức.
     *
     * @param organizationId ID tổ chức
     * @param userId         ID người dùng
     * @param roleId         ID vai trò (tùy chọn)
     * @return thông tin thành viên sau khi thêm
     */
    OrganizationUserResponse addExistingUserToOrganization(UUID organizationId, UUID userId, Integer roleId);
}
