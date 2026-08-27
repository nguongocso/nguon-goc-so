import apiClient from '@/api/axiosConfig';
import type {
  AddMemberRequest,
  ApiResult,
  AssignRoleRequest,
  DeactivateMemberRequest,
  MemberStatus,
  OrganizationMember,
  ReactivateMemberRequest,
  ReplacementCandidate,
  RoleOption,
  UnfinishedLotsResponse,
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
 * Precheck các lô chưa hoàn thành đang phân công cho thành viên
 * trước khi vô hiệu hóa (QTN-32).
 */
export const getUnfinishedLots = async (
  userId: string,
): Promise<UnfinishedLotsResponse> => {
  const response = await apiClient.get<ApiResult<UnfinishedLotsResponse>>(
    `${MEMBER_ENDPOINT}/${userId}/unfinished-lots`,
  );

  return response.data.data;
};

/**
 * Danh sách thành viên đủ điều kiện thay thế (membership ACTIVE cùng
 * tổ chức, đủ quyền ghi sự kiện, tài khoản không bị khóa).
 * Danh sách rỗng là response 200 hợp lệ — không phải lỗi.
 */
export const getReplacementCandidates = async (
  userId: string,
  params?: { lotId?: string; keyword?: string },
): Promise<ReplacementCandidate[]> => {
  const response = await apiClient.get<ApiResult<ReplacementCandidate[]>>(
    `${MEMBER_ENDPOINT}/${userId}/replacement-candidates`,
    { params },
  );

  return response.data.data;
};

/**
 * Vô hiệu hóa thành viên: thu hồi quyền, chấm dứt phiên, chuyển giao
 * lô cho người thay thế (nếu bắt buộc) và ghi audit log (QTN-32).
 *
 * Khi thành viên còn lô chưa hoàn thành mà chưa chọn người thay thế,
 * backend trả 409 kèm `errors` dạng ReplacementRequiredError.
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