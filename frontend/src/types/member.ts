export type MemberStatus = 'ACTIVE' | 'INACTIVE';

export interface RoleOption {
  roleId: number;
  code: string;
  name: string;
}

export interface OrganizationMember {
  id: string;
  organizationId: string;
  userId: string;
  username: string;
  fullName: string;
  roleId: number;
  roleCode: string | null;
  roleName: string | null;
  status: MemberStatus;
  joinedAt: string;
  email?: string | null;
  phone?: string | null;
}

export interface AssignRoleRequest {
  userId: string;
  roleId: number;
}

export interface AddMemberRequest {
  username: string;
  password: string;
  fullName: string;
  phone?: string | null;
  email?: string | null;
  roleId: number;
}

export interface ApiResult<T> {
  success: boolean;
  status: number;
  message?: string;
  data: T;
  errors?: unknown;
  path?: string;
  timestamp?: string;
}