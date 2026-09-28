import { useAuth } from './useAuth';
import { hasAnyRole, type AuthenticatedRoleCode } from '@/config/roleAccess';

/** Kiểm tra người dùng hiện tại có vai trò nằm trong danh sách cho phép hay không. */
export const usePermission = (allowedRoles: readonly AuthenticatedRoleCode[]): boolean => {
  const { user } = useAuth();
  return hasAnyRole(user?.roleCode, allowedRoles);
};
