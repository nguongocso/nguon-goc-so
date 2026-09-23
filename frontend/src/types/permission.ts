export interface PermissionItem {
  permissionId: number;
  action: string;
  description?: string;
  isEnabled: boolean;
  isDefault: boolean;
}

export interface PermissionGroup {
  resource: string;
  resourceLabel: string;
  permissions: PermissionItem[];
}

export interface RolePermissionResponse {
  organizationId: string;
  roleId: number;
  roleCode: string;
  roleName: string;
  groups: PermissionGroup[];
}

export interface PermissionToggle {
  permissionId: number;
  isEnabled: boolean;
}

export interface UpdateRolePermissionRequest {
  permissions: PermissionToggle[];
}

export interface RoleInfo {
  roleId: number;
  roleCode: string;
  roleName: string;
}

/** Mã quyền chuẩn cho các sự kiện chuỗi cung ứng. */
export const EVENT_PERMISSIONS = {
  EVENT_FARM_LOG: 'EVENT_FARM_LOG',
  EVENT_HARVEST: 'EVENT_HARVEST',
  EVENT_PREPROCESSING: 'EVENT_PREPROCESSING',
  EVENT_PACKAGING: 'EVENT_PACKAGING',
  EVENT_TRANSPORT: 'EVENT_TRANSPORT',
} as const;

export type EventPermissionCode = keyof typeof EVENT_PERMISSIONS;

export interface EventPermissionOption {
  code: string;
  label: string;
  description: string;
  defaultEnabled: boolean;
}
