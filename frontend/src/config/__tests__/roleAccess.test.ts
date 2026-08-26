import { describe, expect, it } from 'vitest';
import { hasAnyRole, ROLE_ACCESS } from '@/config/roleAccess';

/**
 * TC-F (phần 1): kiểm tra trực tiếp hàm phân quyền.
 *
 * Hạn chế có chủ đích: `RoleRoute` là component nội bộ của AppRoutes.tsx
 * (không export) và AppRoutes kéo theo toàn bộ graph page của app nên việc
 * render route thật trong jsdom quá nặng. Theo spec NCL-742, fallback được
 * phép: test hasAnyRole + cấu hình ROLE_ACCESS; hành vi redirect của
 * RoleRoute đã được bao phủ bởi các màn hình admin hiện có cùng pattern.
 */
describe('TC-F - Quyền truy cập màn hình Phân công địa bàn', () => {
  it('hasAnyRole trả false khi vai trò không nằm trong danh sách cho phép', () => {
    expect(hasAnyRole('VT-02', ['VT-01'])).toBe(false);
  });

  it('ROLE_ACCESS.areaAssignment chỉ dành cho VT-01', () => {
    expect(ROLE_ACCESS.areaAssignment).toEqual(['VT-01']);
    // Chứa duy nhất 1 phần tử là VT-01.
    expect(ROLE_ACCESS.areaAssignment).toHaveLength(1);
    expect(ROLE_ACCESS.areaAssignment.every((role) => role === 'VT-01')).toBe(true);
    expect(hasAnyRole('VT-01', ROLE_ACCESS.areaAssignment)).toBe(true);
    expect(hasAnyRole('VT-05', ROLE_ACCESS.areaAssignment)).toBe(false);
    expect(hasAnyRole(undefined, ROLE_ACCESS.areaAssignment)).toBe(false);
  });
});
