import { describe, expect, it } from 'vitest';
import {
  formatActionType,
  getActionColor,
} from '@/utils/activityLogFormatter';

/**
 * Activity History (Lịch sử hoạt động — VT02) dùng chung `formatActionType`
 * cho cả bảng danh sách (ActivityLogTable) và màn hình chi tiết
 * (ActivityLogDetailDialog). Do đó chỉ cần một mapping tại đây là đủ cho
 * cả hai vị trí.
 */
describe('utils/activityLogFormatter — formatActionType', () => {
  describe('DISPOSE → Loại bỏ (Lịch sử hoạt động VT02)', () => {
    it('bảng Lịch sử hoạt động: hiển thị "Loại bỏ" thay vì raw DISPOSE', () => {
      // Column "Hành động" của ActivityLogTable gọi formatActionType(actionVal)
      expect(formatActionType('DISPOSE')).toBe('Loại bỏ');
    });

    it('màn hình Chi tiết hoạt động: dùng cùng mapping → "Loại bỏ"', () => {
      // Trường "Hành động" của ActivityLogDetailDialog gọi formatActionType(getActionValue(log))
      expect(formatActionType('dispose')).toBe('Loại bỏ');
    });

    it('không bao giờ trả về raw enum DISPOSE cho người dùng cuống', () => {
      expect(formatActionType('DISPOSE')).not.toBe('DISPOSE');
    });
  });

  describe('Regression — các action khác không bị ảnh hưởng', () => {
    it('CREATE → Tạo mới', () => {
      expect(formatActionType('CREATE')).toBe('Tạo mới');
    });

    it('UPDATE → Cập nhật', () => {
      expect(formatActionType('UPDATE')).toBe('Cập nhật');
    });

    it('DELETE → Xóa', () => {
      expect(formatActionType('DELETE')).toBe('Xóa');
    });

    it('RECALL → Thu hồi lô', () => {
      expect(formatActionType('RECALL')).toBe('Thu hồi lô');
    });

    it('LOGIN → Đăng nhập hệ thống', () => {
      expect(formatActionType('LOGIN')).toBe('Đăng nhập hệ thống');
    });

    it('REVOKE_API_KEY → Thu hồi API key', () => {
      expect(formatActionType('REVOKE_API_KEY')).toBe('Thu hồi API key');
    });
  });

  describe('formatActionType — hành vi mặc định', () => {
    it('chuỗi rỗng trả về "—"', () => {
      expect(formatActionType('')).toBe('—');
    });

    it('action chưa map trả về nguyên chuỗi gốc', () => {
      expect(formatActionType('UNKNOWN_ACTION')).toBe('UNKNOWN_ACTION');
    });
  });
});

describe('utils/activityLogFormatter — getActionColor', () => {
  it('DISPOSE được gán màu rose (hành động loại bỏ/nguy hiểm)', () => {
    expect(getActionColor('DISPOSE')).toContain('rose');
  });
});
