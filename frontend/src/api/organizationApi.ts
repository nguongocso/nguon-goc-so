import apiClient from "@/api/axiosConfig";

import type {
  OrganizationProfile,
  UpdateOrganizationRequest,
  CreateOrganizationRequest,
  CreateOrganizationResponse,
  Organization,
  OrganizationDetailResponse,
  OrganizationUserResponse,
  AddMemberRequest,
  CreateOrganizationMemberResponse,
  AvailableUser,
} from "@/types/organization";

export const getOrganizationProfile = async (): Promise<
  OrganizationProfile
> => {
  const response = await apiClient.get<{
    data: OrganizationProfile;
  }>("/organizations/profile");

  return response.data.data;
};

export const updateOrganizationProfile = async (
  data: UpdateOrganizationRequest
): Promise<OrganizationProfile> => {
  const response = await apiClient.put<{
    data: OrganizationProfile;
  }>("/organizations/profile", data);

  return response.data.data;
};

export const createOrganization = async (
  data: CreateOrganizationRequest
): Promise<CreateOrganizationResponse> => {
  const response = await apiClient.post<CreateOrganizationResponse>(
    "/admin/organizations",
    data
  );

  return response.data;
};

export const getOrganizations = async (): Promise<Organization[]> => {
  const response = await apiClient.get<{
    data: Organization[];
  }>("/admin/organizations");

  return response.data.data;
};

export const getOrganizationDetail = async (
  id: string
): Promise<OrganizationDetailResponse> => {
  const response = await apiClient.get<{
    data: OrganizationDetailResponse;
  }>(`/admin/organizations/${id}`);

  return response.data.data;
};

export const createOrganizationMember = async (
  organizationId: string,
  data: AddMemberRequest
): Promise<CreateOrganizationMemberResponse> => {
  const response = await apiClient.post<{
    data: CreateOrganizationMemberResponse;
  }>(`/admin/organizations/${organizationId}/members`, data);

  return response.data.data;
};

export interface AddExistingUserRequest {
  userId: string;
  roleId?: number;
}

export const getAvailableUsers = async (
  organizationId: string
): Promise<AvailableUser[]> => {
  const response = await apiClient.get<{
    data: AvailableUser[];
  }>(`/admin/organizations/${organizationId}/available-users`);

  return response.data.data;
};

export const addExistingUser = async (
  organizationId: string,
  data: AddExistingUserRequest
): Promise<OrganizationUserResponse> => {
  const response = await apiClient.post<{
    data: OrganizationUserResponse;
  }>(`/admin/organizations/${organizationId}/add-existing-user`, data);

  return response.data.data;
};

export const assignRole = async (data: {
  userId: string;
  roleId: number;
}): Promise<OrganizationUserResponse> => {
  const response = await apiClient.put<{
    data: OrganizationUserResponse;
  }>("/admin/organizations/current/members/role", data);

  return response.data.data;
};