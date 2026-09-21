import apiClient from '@/api/axiosConfig';

import type {
  AddMemberRequest,
  AvailableUser,
  CreateOrganizationMemberResponse,
  CreateOrganizationRequest,
  CreateOrganizationResponse,
  Organization,
  OrganizationDetailResponse,
  OrganizationProfile,
  OrganizationUserResponse,
  UpdateOrganizationRequest,
} from '@/types/organization';

interface RequestOptions {
  signal?: AbortSignal;
}

export interface AddExistingUserRequest {
  userId: string;
  roleId?: number;
}

/**
 * Lấy hồ sơ thông tin tổ chức của người dùng hiện tại.
 * GET /api/v1/organizations/profile
 */
export async function getOrganizationProfile(
  { signal }: RequestOptions = {},
): Promise<OrganizationProfile> {
  const response = await apiClient.get<{
    data: OrganizationProfile;
  }>('/organizations/profile', { signal });

  return response.data.data;
}

/**
 * Cập nhật hồ sơ thông tin tổ chức.
 * PUT /api/v1/organizations/profile
 */
export async function updateOrganizationProfile(
  data: UpdateOrganizationRequest,
): Promise<OrganizationProfile> {
  const response = await apiClient.put<{
    data: OrganizationProfile;
  }>('/organizations/profile', data);

  return response.data.data;
}

/**
 * Tạo tổ chức mới (dành cho Quản trị viên hệ thống).
 * POST /api/v1/admin/organizations
 */
export async function createOrganization(
  data: CreateOrganizationRequest,
): Promise<CreateOrganizationResponse> {
  const response = await apiClient.post<CreateOrganizationResponse>(
    '/admin/organizations',
    data,
  );

  return response.data;
}

/**
 * Lấy danh sách toàn bộ tổ chức trong hệ thống (dành cho Admin).
 * GET /api/v1/admin/organizations
 */
export async function getOrganizations(
  { signal }: RequestOptions = {},
): Promise<Organization[]> {
  const response = await apiClient.get<{
    data: Organization[];
  }>('/admin/organizations', { signal });

  return response.data.data;
}

/**
 * Danh sách tổ chức nhận cho dropdown phiếu bàn giao.
 * Chỉ gồm các tổ chức Doanh nghiệp thu mua (VT-04 / ENTERPRISE), ACTIVE và khác
 * tổ chức hiện tại nên VT-02 dùng được, thay cho GET /admin/organizations (chỉ VT-01).
 * GET /api/v1/organizations/recipient-organizations
 */
export async function getRecipientOrganizations(
  { signal }: RequestOptions = {},
): Promise<Organization[]> {
  const response = await apiClient.get<{
    data: Organization[];
  }>('/organizations/recipient-organizations', { signal });

  return response.data.data;
}

/**
 * Lấy chi tiết thông tin tổ chức và danh sách thành viên.
 * GET /api/v1/admin/organizations/{id}
 */
export async function getOrganizationDetail(
  id: string,
  { signal }: RequestOptions = {},
): Promise<OrganizationDetailResponse> {
  const response = await apiClient.get<{
    data: OrganizationDetailResponse;
  }>(`/admin/organizations/${id}`, { signal });

  return response.data.data;
}

/**
 * Tạo thành viên mới trực thuộc tổ chức.
 * POST /api/v1/admin/organizations/{organizationId}/members
 */
export async function createOrganizationMember(
  organizationId: string,
  data: AddMemberRequest,
): Promise<CreateOrganizationMemberResponse> {
  const response = await apiClient.post<{
    data: CreateOrganizationMemberResponse;
  }>(`/admin/organizations/${organizationId}/members`, data);

  return response.data.data;
}

/**
 * Lấy danh sách user khả dụng chưa thuộc tổ chức để thêm vào.
 * GET /api/v1/admin/organizations/{organizationId}/available-users
 */
export async function getAvailableUsers(
  organizationId: string,
  { signal }: RequestOptions = {},
): Promise<AvailableUser[]> {
  const response = await apiClient.get<{
    data: AvailableUser[];
  }>(`/admin/organizations/${organizationId}/available-users`, { signal });

  return response.data.data;
}

/**
 * Thêm người dùng đã tồn tại vào tổ chức.
 * POST /api/v1/admin/organizations/{organizationId}/add-existing-user
 */
export async function addExistingUser(
  organizationId: string,
  data: AddExistingUserRequest,
): Promise<OrganizationUserResponse> {
  const response = await apiClient.post<{
    data: OrganizationUserResponse;
  }>(`/admin/organizations/${organizationId}/add-existing-user`, data);

  return response.data.data;
}

/**
 * Gán vai trò cho thành viên trong tổ chức.
 * PUT /api/v1/admin/organizations/current/members/role
 */
export async function assignRole(data: {
  userId: string;
  roleId: number;
}): Promise<OrganizationUserResponse> {
  const response = await apiClient.put<{
    data: OrganizationUserResponse;
  }>('/admin/organizations/current/members/role', data);

  return response.data.data;
}
