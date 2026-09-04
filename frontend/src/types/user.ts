import type { OrganizationType } from "./auth";

/**
 * Thông tin hồ sơ người dùng (NCL-01-CN-010).
 */
export interface UserProfile {
  id: string;
  userId: string;
  username: string;
  fullName: string;
  phone?: string | null;
  email?: string | null;
  avatarUrl?: string | null;
  roleCode: string;
  roleName: string;
  organizationId: string;
  organizationCode: string;
  organizationName: string;
  organizationType: OrganizationType;
  permissions?: string[];
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Payload cập nhật hồ sơ cá nhân.
 */
export interface UpdateUserProfilePayload {
  fullName?: string;
  phone?: string | null;
  email?: string | null;
  avatarUrl?: string | null;
}

/**
 * Payload đổi mật khẩu chủ động.
 */
export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

/**
 * Kết quả tải lên ảnh đại diện.
 */
export interface AvatarUploadResponse {
  avatarUrl: string;
}
