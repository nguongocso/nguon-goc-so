import apiClient from "./axiosConfig";
import type { ApiResult } from "@/types/auth";
import type {
  AvatarUploadResponse,
  ChangePasswordPayload,
  UpdateUserProfilePayload,
  UserProfile,
} from "@/types/user";

/**
 * Lấy thông tin hồ sơ cá nhân của người dùng hiện tại (NCL-01-CN-010).
 */
export const getProfile = async (): Promise<ApiResult<UserProfile>> => {
  const response = await apiClient.get<ApiResult<UserProfile>>("/users/profile");
  return response.data;
};

/**
 * Cập nhật thông tin hồ sơ cá nhân (NCL-01-CN-010).
 */
export const updateProfile = async (
  payload: UpdateUserProfilePayload
): Promise<ApiResult<UserProfile>> => {
  const response = await apiClient.put<ApiResult<UserProfile>>(
    "/users/profile",
    payload
  );
  return response.data;
};

/**
 * Chủ động đổi mật khẩu (NCL-01-CN-010).
 */
export const changePassword = async (
  payload: ChangePasswordPayload
): Promise<ApiResult<void>> => {
  const response = await apiClient.post<ApiResult<void>>(
    "/users/change-password",
    payload
  );
  return response.data;
};

/**
 * Tải lên ảnh đại diện cá nhân (NCL-01-CN-010).
 */
export const uploadAvatar = async (
  file: File
): Promise<ApiResult<AvatarUploadResponse>> => {
  const formData = new FormData();
  formData.append("file", file);

  const response = await apiClient.post<ApiResult<AvatarUploadResponse>>(
    "/users/avatar",
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    }
  );
  return response.data;
};
