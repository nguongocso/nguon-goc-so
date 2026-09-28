import { isAxiosError } from 'axios';

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

export const login = async (
  data: LoginRequest
): Promise<ApiResult<LoginResponse>> => {
  const response = await apiClient.post<ApiResult<LoginResponse>>(
    "/auth/login",
    data
  );

  return response.data;
};

export const MEMBERSHIP_INACTIVE_MESSAGE =
  'Người dùng chưa được gán vào tổ chức nào';

export const DEACTIVATED_ACCOUNT_MESSAGE =
  'Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ với Quản lý hợp tác xã của bạn để được kích hoạt lại.';

function withDeactivatedMessage<T>(error: unknown): ApiResult<T> {
  if (isAxiosError(error)) {
    const status = error.response?.status ?? 0;
    const data = error.response?.data as ApiResult<unknown> | undefined;
    const rawMessage = data?.message ?? '';
    const message =
      rawMessage === MEMBERSHIP_INACTIVE_MESSAGE
        ? DEACTIVATED_ACCOUNT_MESSAGE
        : rawMessage || DEACTIVATED_ACCOUNT_MESSAGE;
    return {
      success: false,
      status,
      message,
      data: null as unknown as T,
      errors: data?.errors,
      path: data?.path,
      timestamp: data?.timestamp ?? new Date().toISOString(),
    };
  }
  return {
    success: false,
    status: 0,
    message: DEACTIVATED_ACCOUNT_MESSAGE,
    data: null as unknown as T,
    errors: undefined,
    path: undefined,
    timestamp: new Date().toISOString(),
  };
}

export const getOrganizations = async (
  selectionToken?: string
): Promise<ApiResult<OrganizationSelection[]>> => {
  try {
    const response = await apiClient.get<
      ApiResult<OrganizationSelection[]>
    >('/auth/organizations', {
      headers: selectionToken
        ? { Authorization: `Bearer ${selectionToken}` }
        : undefined,
    });

    return response.data;
  } catch (error: unknown) {
    return withDeactivatedMessage<OrganizationSelection[]>(error);
  }
};

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

export const getCurrent = async (): Promise<
  ApiResult<SelectOrganizationResponse["user"]>
> => {
  const response = await apiClient.get<
    ApiResult<SelectOrganizationResponse["user"]>
  >("/auth/me");

  return response.data;
};

export const forgotPassword = async (
  data: ForgotPasswordRequest
): Promise<ApiResult<void>> => {
  const response = await apiClient.post<ApiResult<void>>(
    "/auth/forgot-password",
    data
  );

  return response.data;
};

export const validateResetToken = async (
  token: string
): Promise<ApiResult<ValidateResetTokenResponse>> => {
  const response = await apiClient.get<ApiResult<ValidateResetTokenResponse>>(
    `/auth/reset-password/validate?token=${encodeURIComponent(token)}`
  );

  return response.data;
};

export const resetPassword = async (
  data: ResetPasswordRequest
): Promise<ApiResult<void>> => {
  const response = await apiClient.post<ApiResult<void>>(
    "/auth/reset-password",
    data
  );

  return response.data;
};

export const updateUserProfile = async (
  data: UpdateUserProfileRequest
): Promise<ApiResult<AuthUserInfo>> => {
  const response = await apiClient.put<ApiResult<AuthUserInfo>>(
    "/auth/profile",
    data
  );

  return response.data;
};