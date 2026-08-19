import type {
  ApiResult,
  LoginRequest,
  LoginResponse,
  SelectOrganizationRequest,
  SelectOrganizationResponse,
} from "@/types/auth";

import type { OrganizationSelection } from "@/types/organization";

import apiClient from "./axiosConfig";

/**
 * Bước 1:
 * Xác thực username/password.
 *
 * Backend trả về Selection JWT.
 */
export const login = async (
  data: LoginRequest
): Promise<ApiResult<LoginResponse>> => {
  const response = await apiClient.post<ApiResult<LoginResponse>>(
    "/auth/login",
    data
  );

  return response.data;
};

/**
 * Bước 2:
 * Lấy danh sách organization mà user được phép truy cập.
 *
 * API này sử dụng Selection JWT.
 */
export const getOrganizations = async (
  selectionToken?: string
): Promise<
  ApiResult<OrganizationSelection[]>
> => {
  const response = await apiClient.get<
    ApiResult<OrganizationSelection[]>
  >("/auth/organizations", {
    headers: selectionToken
      ? { Authorization: `Bearer ${selectionToken}` }
      : undefined,
  });

  return response.data;
};

/**
 * Bước 3:
 * User chọn organization.
 *
 * Backend kiểm tra Selection JWT,
 * organizationId và membership,
 * sau đó cấp Access JWT.
 */
export const selectOrganization = async (
  data: SelectOrganizationRequest,
  selectionToken?: string
): Promise<ApiResult<SelectOrganizationResponse>> => {
  const response = await apiClient.post<
    ApiResult<SelectOrganizationResponse>
  >("/auth/select-organization", data, {
    headers: selectionToken
      ? { Authorization: `Bearer ${selectionToken}` }
      : undefined,
  });

  return response.data;
};

export const getMyOrganizations = async (): Promise<
  ApiResult<OrganizationSelection[]>
> => {
  const response = await apiClient.get<
    ApiResult<OrganizationSelection[]>
  >("/auth/my-organizations");

  return response.data;
};

export const switchOrganization = async (
  data: SelectOrganizationRequest
): Promise<ApiResult<SelectOrganizationResponse>> => {
  const response = await apiClient.post<
    ApiResult<SelectOrganizationResponse>
  >("/auth/switch-organization", data);

  return response.data;
};

/**
 * Lấy thông tin user hiện tại.
 *
 * API này yêu cầu Access JWT.
 */
export const getCurrent = async (): Promise<
  ApiResult<SelectOrganizationResponse["user"]>
> => {
  const response = await apiClient.get<
    ApiResult<SelectOrganizationResponse["user"]>
  >("/auth/me");

  return response.data;
};