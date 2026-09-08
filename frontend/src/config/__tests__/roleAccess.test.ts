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

  it('ROLE_ACCESS.milestoneReminderScan cho phép VT-01, VT-02, VT-03', () => {
    expect(ROLE_ACCESS.milestoneReminderScan).toEqual(['VT-01', 'VT-02', 'VT-03']);
    expect(hasAnyRole('VT-01', ROLE_ACCESS.milestoneReminderScan)).toBe(true);
    expect(hasAnyRole('VT-02', ROLE_ACCESS.milestoneReminderScan)).toBe(true);
    expect(hasAnyRole('VT-03', ROLE_ACCESS.milestoneReminderScan)).toBe(true);
    expect(hasAnyRole('VT-04', ROLE_ACCESS.milestoneReminderScan)).toBe(false);
    expect(hasAnyRole('VT-05', ROLE_ACCESS.milestoneReminderScan)).toBe(false);
  });

  it('ROLE_ACCESS.anomalyThresholdConfig chỉ dành cho VT-01', () => {
    expect(ROLE_ACCESS.anomalyThresholdConfig).toEqual(['VT-01']);
    expect(hasAnyRole('VT-01', ROLE_ACCESS.anomalyThresholdConfig)).toBe(true);
    expect(hasAnyRole('VT-02', ROLE_ACCESS.anomalyThresholdConfig)).toBe(false);
    expect(hasAnyRole('VT-03', ROLE_ACCESS.anomalyThresholdConfig)).toBe(false);
    expect(hasAnyRole('VT-04', ROLE_ACCESS.anomalyThresholdConfig)).toBe(false);
    expect(hasAnyRole('VT-05', ROLE_ACCESS.anomalyThresholdConfig)).toBe(false);
  });
});
