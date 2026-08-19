package vn.nguongocso.organization.service;

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

import java.util.List;
import java.util.UUID;

/** Quản lý tổ chức và thông tin thành viên liên quan. */
public interface OrganizationService {
    /** Tạo tổ chức mới. */
    OrganizationResponse createOrganization(CreateOrganizationRequest request);

    /** Lấy hồ sơ tổ chức hiện tại. */
    OrganizationProfileResponse getCurrentOrganizationProfile();

    /** Cập nhật hồ sơ tổ chức hiện tại. */
    OrganizationProfileResponse updateCurrentOrganization(OrganizationUpdateRequest request);

    /** Cập nhật tổ chức theo ID. */
    OrganizationProfileResponse updateOrganizationById(UUID orgId, OrganizationUpdateRequest request);

    /** Lấy danh sách tất cả tổ chức. */
    List<OrganizationResponse> getAllOrganizations();

    /** Lấy chi tiết tổ chức. */
    OrganizationDetailResponse getOrganizationDetail(UUID organizationId);

    /** Thêm thành viên vào tổ chức. */
    CreateOrganizationMemberResponse addMember(
            UUID organizationId,
            AddMemberRequest request);

    /** Gán vai trò cho thành viên. */
    OrganizationUserResponse assignRole(AssignRoleRequest request);

    /** Lấy danh sách thành viên của tổ chức hiện tại. */
    List<OrganizationUserResponse> getMembersOfCurrentOrganization();

    /** Lấy danh sách người dùng có thể thêm vào tổ chức. */
    List<AvailableUserResponse> getAvailableUsersForOrganization(UUID organizationId);

    /** Thêm người dùng hiện có vào tổ chức. */
    OrganizationUserResponse addExistingUserToOrganization(UUID organizationId, UUID userId, Integer roleId);
}
