export interface PermissionItem {
  permissionId: number;
  /** CREATE, READ, UPDATE, DELETE, ... */
  action: string;
  description?: string;
  /** Trạng thái hiện tại (sau khi áp dụng) */
  isEnabled: boolean;
  /** true = đang dùng mặc định hệ thống */
  isDefault: boolean;
}

export interface PermissionGroup {
  /** 'production_lot', 'chain_event', ... */
  resource: string;
  /** 'Lô sản xuất', 'Sự kiện chuỗi', ... */
  resourceLabel: string;
  permissions: PermissionItem[];
}

export interface RolePermissionResponse {
  organizationId: string;
  roleId: number;
  /** 'VT-03' */
  roleCode: string;
  /** 'Người ghi sự kiện' */
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

/**
 * Các mã quyền chuẩn cho nhóm ghi nhận sự kiện chuỗi cung ứng.
 */
export const EVENT_PERMISSIONS = {
  /** Ghi nhật ký canh tác */
  EVENT_FARM_LOG: 'EVENT_FARM_LOG',
  /** Ghi sự kiện thu hoạch */
  EVENT_HARVEST: 'EVENT_HARVEST',
  /** Ghi sự kiện sơ chế & phân loại */
  EVENT_PREPROCESSING: 'EVENT_PREPROCESSING',
  /** Ghi sự kiện đóng gói */
  EVENT_PACKAGING: 'EVENT_PACKAGING',
  /** Ghi sự kiện vận chuyển */
  EVENT_TRANSPORT: 'EVENT_TRANSPORT',
} as const;

export type EventPermissionCode = keyof typeof EVENT_PERMISSIONS;

export interface EventPermissionOption {
  code: string;
  label: string;
  description: string;
  defaultEnabled: boolean;
}
