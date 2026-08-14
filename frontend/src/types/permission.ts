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