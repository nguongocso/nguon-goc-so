import apiClient from "./axiosConfig";
import type { ApiResult } from "@/types/auth";
import type {
  AvatarUploadResponse,
  ChangePasswordPayload,
  UpdateUserProfilePayload,
  UserProfile,
} from "@/types/user";

export const getProfile = async (): Promise<ApiResult<UserProfile>> => {
  const response = await apiClient.get<ApiResult<UserProfile>>("/users/profile");
  return response.data;
};

export const updateProfile = async (
  payload: UpdateUserProfilePayload
): Promise<ApiResult<UserProfile>> => {
  const response = await apiClient.put<ApiResult<UserProfile>>(
    "/users/profile",
    payload
  );
  return response.data;
};

export const changePassword = async (
  payload: ChangePasswordPayload
): Promise<ApiResult<void>> => {
  const response = await apiClient.post<ApiResult<void>>(
    "/users/change-password",
    payload
  );
  return response.data;
};

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
