export type AuthenticatedRoleCode = 'VT-01' | 'VT-02' | 'VT-03' | 'VT-04' | 'VT-05';

export const AUTHENTICATED_ROLE_CODES: AuthenticatedRoleCode[] = ['VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05'];

export const ROLE_ACCESS = {
  dashboard: AUTHENTICATED_ROLE_CODES,
  organizationCreate: ['VT-01'],
  organizationList: ['VT-01'],
  organizationProfile: ['VT-01', 'VT-02'],
  farmAreaCreate: ['VT-02'],
  productionLotList: ['VT-02', 'VT-03'],
  productionLotEdit: ['VT-02'],
  productionLotApprove: ['VT-02'],
  farmLogCreate: ['VT-03'],
  packagingEventCreate: ['VT-02', 'VT-03'] as const,
  packagingEventCorrect: ['VT-02', 'VT-03'] as const,
  transportEventRecord: ["VT-03"] as const,
  scanQuickEvent: ["VT-03"] as const,

  codeRangeList: ['VT-01'] as const,
  
  memberManagement: ['VT-02'] as const,

  scanAnomalyAlerts: ['VT-01', 'VT-02'] as const,

  procurementEvent: ['VT-04'] as const,

  warehouseReceipt: ['VT-04'] as const,

  storageCondition: ['VT-03', 'VT-04'] as const,

  eventChainVerification: ['VT-01', 'VT-04', 'VT-05'] as const,

  standardManagement: ['VT-01'] as const,

  // ✅ Từ file 1
  notificationInbox: AUTHENTICATED_ROLE_CODES,

  // ✅ Từ file 2
  exportOpenData: ['VT-05'] as const,

  // ✅ Từ file 1
  seasonYieldComparison: ['VT-01', 'VT-05'] as const,

  // ✅ Từ file 2
  rolePermissionConfig: ['VT-02'] as const,

  productFeedbackManagement: ['VT-01', 'VT-02'] as const,

} as const satisfies Record<string, readonly AuthenticatedRoleCode[]>;

export function hasAnyRole(
  userRole: string | undefined,
  allowedRoles: readonly AuthenticatedRoleCode[],
): boolean {
  if (!userRole) return false;
  return allowedRoles.includes(userRole as AuthenticatedRoleCode);
}

export function getRoleLabel(roleCode?: string): string {
  const map: Record<string, string> = {
    'VT-01': 'Quản trị viên hệ thống',
    'VT-02': 'Quản lý hợp tác xã',
    'VT-03': 'Người ghi sự kiện',
    'VT-04': 'Doanh nghiệp thu mua',
    'VT-05': 'Cán bộ quản lý ngành',
    'VT-06': 'Người dùng hệ thống',
  };
  return roleCode ? map[roleCode] || 'Người dùng' : 'Người dùng';
}