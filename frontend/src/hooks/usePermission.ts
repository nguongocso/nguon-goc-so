import { hasAnyRole, type AuthenticatedRoleCode } from '@/config/roleAccess';
import { useAuth } from './useAuth';

/**
 * Custom hook kiểm tra quyền hạn người dùng theo danh sách vai trò cho phép.
 * Trả về boolean xác định xem người dùng hiện tại có vai trò hợp lệ hay không.
 *
 * @example
 *   const canCreate = usePermission(['VT-02']);
 *   {canCreate && <Button>Thêm thành viên</Button>}
 */
export function usePermission(allowedRoles: readonly AuthenticatedRoleCode[]): boolean {
  const { user } = useAuth();
  return hasAnyRole(user?.roleCode, allowedRoles);
}
