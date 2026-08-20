import apiClient from '@/api/axiosConfig';
import type {
  AddMemberRequest,
  ApiResult,
  AssignRoleRequest,
  OrganizationMember,
  RoleOption,
} from '@/types/member';

const MEMBER_ENDPOINT = '/organization/members';

export const getOrganizationMembers =
  async (): Promise<OrganizationMember[]> => {
    const response = await apiClient.get<
      ApiResult<OrganizationMember[]>
    >(MEMBER_ENDPOINT);

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