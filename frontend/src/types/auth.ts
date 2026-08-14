export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  selectionToken: string;
  tokenType: string;
  expiresIn: number;
  user: LoginUserInfo;
}

export interface LoginUserInfo {
  userId: string;
  username: string;
  fullName: string;
}

export interface SelectOrganizationRequest {
  organizationId: string;
}

export interface SelectOrganizationResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUserInfo;
}

export interface AuthUserInfo {
  userId: string;
  username: string;
  fullName: string;

  phone?: string | null;
  email?: string | null;

  roleCode: string;
  roleName: string;

  organizationId: string;
  organizationCode: string;
  organizationName: string;
  organizationType: OrganizationType;

  permissions?: string[];
}

export type OrganizationType =
  | "SYSTEM"
  | "COOPERATIVE"
  | "ENTERPRISE"
  | "GOVERNMENT";

export interface AuthState {
  user: AuthUserInfo | null;
  token: string | null;
  selectionToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

export interface ApiResult<T> {
  success: boolean;
  status: number;
  message?: string;
  data: T;
  errors?: unknown;
  path?: string;
  timestamp: string;
}