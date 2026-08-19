import apiClient from './axiosConfig';
import type {
  PermissionGroup,
  RolePermissionResponse,
  UpdateRolePermissionRequest,
  RoleInfo,
} from '@/types/permission';
import { getRoleLabel } from '@/config/roleAccess';

/**
 * Lấy danh sách toàn bộ quyền hệ thống (nhóm theo resource)
 * GET /api/v1/permissions
 * Chỉ VT-02 mới gọi được
 */
export const getSystemPermissions = async (): Promise<PermissionGroup[]> => {
  const response = await apiClient.get<{ data: PermissionGroup[] }>('/permissions');
  return response.data.data;
};

/**
 * Lấy cấu hình quyền hiện tại của một vai trò trong tổ chức
 * GET /api/v1/organizations/{organizationId}/roles/{roleId}/permissions
 */
export const getRolePermissions = async (
  organizationId: string,
  roleId: number
): Promise<RolePermissionResponse> => {
  const response = await apiClient.get<{ data: RolePermissionResponse }>(
    `/organizations/${organizationId}/roles/${roleId}/permissions`
  );
  return response.data.data;
};

/**
 * Cập nhật cấu hình quyền cho vai trò trong tổ chức
 * PUT /api/v1/organizations/{organizationId}/roles/{roleId}/permissions
 */
export const updateRolePermissions = async (
  organizationId: string,
  roleId: number,
  data: UpdateRolePermissionRequest
): Promise<RolePermissionResponse> => {
  const response = await apiClient.put<{ data: RolePermissionResponse }>(
    `/organizations/${organizationId}/roles/${roleId}/permissions`,
    data
  );
  return response.data.data;
};

/** Danh sách vai trò tĩnh dùng làm fallback cuối cùng khi tất cả API đều thất bại */
const STATIC_ROLES: RoleInfo[] = [
  { roleId: 2, roleCode: 'VT-02', roleName: getRoleLabel('VT-02') },
  { roleId: 3, roleCode: 'VT-03', roleName: getRoleLabel('VT-03') },
  { roleId: 4, roleCode: 'VT-04', roleName: getRoleLabel('VT-04') },
  { roleId: 5, roleCode: 'VT-05', roleName: getRoleLabel('VT-05') },
];

interface RoleOption {
  roleId: number;
  code: string;
  name: string;
}

/**
 * Lấy danh sách vai trò khả dụng với 3 cấp fallback:
 * 1. GET /organizations/{organizationId}/roles  (dành riêng cho tổ chức)
 * 2. GET /roles                                (danh sách hệ thống)
 * 3. Danh sách tĩnh                            (fallback cuối cùng)
 */
export const getOrganizationRoles = async (
  organizationId: string
): Promise<RoleInfo[]> => {
  // Cấp 1: endpoint của tổ chức
  try {
    const response = await apiClient.get<{
      data: Array<{ roleId: number; code: string; name: string }>;
    }>(`/organizations/${organizationId}/roles`);
    return response.data.data.map((r) => ({
      roleId: r.roleId,
      roleCode: r.code,
      roleName: getRoleLabel(r.code),
    }));
  } catch (error: any) {
    if (error.response?.status !== 404) throw error;
  }

  // Cấp 2: endpoint danh sách vai trò hệ thống
  try {
    const response = await apiClient.get<RoleOption[]>('/roles');
    return response.data.map((r) => ({
      roleId: r.roleId,
      roleCode: r.code,
      roleName: getRoleLabel(r.code),
    }));
  } catch {
    console.warn('Sử dụng danh sách vai trò tĩnh (fallback)');
  }

  // Cấp 3: danh sách tĩnh
  return STATIC_ROLES;
};
