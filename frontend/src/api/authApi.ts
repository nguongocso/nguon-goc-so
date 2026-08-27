import type {
  ApiResult,
  AuthUserInfo,
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  ResetPasswordRequest,
  SelectOrganizationRequest,
  SelectOrganizationResponse,
  UpdateUserProfileRequest,
  ValidateResetTokenResponse,
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

/**
 * Gửi yêu cầu đặt lại mật khẩu khi quên (NCL-01-CN-008).
 */
export const forgotPassword = async (
  data: ForgotPasswordRequest
): Promise<ApiResult<void>> => {
  const response = await apiClient.post<ApiResult<void>>(
    "/auth/forgot-password",
    data
  );

  return response.data;
};

/**
 * Kiểm tra token đặt lại mật khẩu hợp lệ (NCL-01-CN-008).
 */
export const validateResetToken = async (
  token: string
): Promise<ApiResult<ValidateResetTokenResponse>> => {
  const response = await apiClient.get<ApiResult<ValidateResetTokenResponse>>(
    `/auth/reset-password/validate?token=${encodeURIComponent(token)}`
  );

  return response.data;
};

/**
 * Đặt lại mật khẩu mới (NCL-01-CN-008).
 */
export const resetPassword = async (
  data: ResetPasswordRequest
): Promise<ApiResult<void>> => {
  const response = await apiClient.post<ApiResult<void>>(
    "/auth/reset-password",
    data
  );

  return response.data;
};

/**
 * Cập nhật thông tin hồ sơ cá nhân (SĐT, Email).
 */
export const updateUserProfile = async (
  data: UpdateUserProfileRequest
): Promise<ApiResult<AuthUserInfo>> => {
  const response = await apiClient.put<ApiResult<AuthUserInfo>>(
    "/auth/profile",
    data
  );

  return response.data;
};