import apiClient from '@/api/axiosConfig';
import type {
  AddMemberRequest,
  ApiResult,
  AssignRoleRequest,
  DeactivateMemberRequest,
  MemberStatus,
  OrganizationMember,
  ReactivateMemberRequest,
  RoleOption,
} from '@/types/member';

const MEMBER_ENDPOINT = '/organization/members';

/**
 * Lấy danh sách thành viên của tổ chức hiện tại theo trạng thái membership.
 *
 * - `undefined` → backend trả về danh sách ACTIVE (hành vi cũ, giữ tương thích).
 * - `''` (chuỗi rỗng) → trả về tất cả (ACTIVE + INACTIVE) — dùng cho màn
 *   hình vô hiệu hóa/kích hoạt lại thành viên.
 * - `'ACTIVE'` | `'INACTIVE'` → lọc theo trạng thái membership.
 */
export const getOrganizationMembers = async (
  status?: '' | MemberStatus,
): Promise<OrganizationMember[]> => {
  const response = await apiClient.get<ApiResult<OrganizationMember[]>>(
    MEMBER_ENDPOINT,
    { params: status === undefined ? undefined : { status } },
  );

  return response.data.data;
};

/**
 * Vô hiệu hóa thành viên: thu hồi quyền, chấm dứt phiên và ghi audit log
 * (QTN-32). Luồng chuyển giao lô (replacement) đã được gỡ bỏ vì hệ thống
 * chưa có phân quyền ghi sự kiện theo lô (D-4).
 */
export const deactivateMember = async (
  userId: string,
  request: DeactivateMemberRequest,
): Promise<OrganizationMember> => {
  const response = await apiClient.patch<ApiResult<OrganizationMember>>(
    `${MEMBER_ENDPOINT}/${userId}/deactivate`,
    request,
  );

  return response.data.data;
};

/** Kích hoạt lại thành viên đã ngừng hoạt động (bắt buộc lý do). */
export const reactivateMember = async (
  userId: string,
  request: ReactivateMemberRequest,
): Promise<OrganizationMember> => {
  const response = await apiClient.patch<ApiResult<OrganizationMember>>(
    `${MEMBER_ENDPOINT}/${userId}/reactivate`,
    request,
  );

  return response.data.data;
};

export const getRoles = async (): Promise<RoleOption[]> => {
  const response = await apiClient.get<RoleOption[]>('/roles');
  return response.data;
};

export const assignMemberRole = async (
  request: AssignRoleRequest,
): Promise<OrganizationMember> => {
  const response = await apiClient.put<
    ApiResult<OrganizationMember>
  >(`${MEMBER_ENDPOINT}/roles`, request);

  return response.data.data;
};

export const addMember = async (request: AddMemberRequest): Promise<OrganizationMember> => {
  const response = await apiClient.post<ApiResult<OrganizationMember>>(MEMBER_ENDPOINT, request);
  return response.data.data
};