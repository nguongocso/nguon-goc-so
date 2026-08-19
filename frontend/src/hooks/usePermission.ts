import { useAuth } from './useAuth';
import { hasAnyRole, type AuthenticatedRoleCode } from '@/config/roleAccess';

/**
 * Custom hook for permission checking at the UI level.
 * Returns a boolean indicating whether the current user has any of the allowed roles.
 * 
 * Usage:
 *   const canCreate = usePermission(['VT-02']);
 *   {canCreate && <Button>Thêm thành viên</Button>}
 */
export const usePermission = (allowedRoles: readonly AuthenticatedRoleCode[]): boolean => {
  const { user } = useAuth();
  return hasAnyRole(user?.roleCode, allowedRoles);
};