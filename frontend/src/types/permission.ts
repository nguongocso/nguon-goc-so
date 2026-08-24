export interface PermissionItem {
  permissionId: number;
  action: string;           // CREATE, READ, UPDATE, DELETE, ...
  description?: string;
  isEnabled: boolean;       // Trạng thái hiện tại (sau khi áp dụng)
  isDefault: boolean;       // true = đang dùng mặc định hệ thống
}

export interface PermissionGroup {
  resource: string;         // 'production_lot', 'chain_event', ...
  resourceLabel: string;    // 'Lô sản xuất', 'Sự kiện chuỗi', ...
  permissions: PermissionItem[];
}

export interface RolePermissionResponse {
  organizationId: string;
  roleId: number;
  roleCode: string;         // 'VT-03'
  roleName: string;         // 'Người ghi sự kiện'
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
  EVENT_FARM_LOG: 'EVENT_FARM_LOG',           // Ghi nhật ký canh tác
  EVENT_HARVEST: 'EVENT_HARVEST',             // Ghi sự kiện thu hoạch
  EVENT_PREPROCESSING: 'EVENT_PREPROCESSING', // Ghi sự kiện sơ chế & phân loại
  EVENT_PACKAGING: 'EVENT_PACKAGING',         // Ghi sự kiện đóng gói
  EVENT_TRANSPORT: 'EVENT_TRANSPORT',         // Ghi sự kiện vận chuyển
} as const;

export type EventPermissionCode = keyof typeof EVENT_PERMISSIONS;

export interface EventPermissionOption {
  code: string;
  label: string;
  description: string;
  defaultEnabled: boolean;
}