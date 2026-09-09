import { describe, expect, it } from 'vitest';
import {
  formatActionType,
  formatTargetType,
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

/**
 * Việt hóa giá trị hiển thị của thu hồi hàng loạt (NCL-08-CN-011).
 * Backend vẫn lưu/trả về raw code — mapping chỉ diễn ra ở presentation layer.
 */
describe('utils/activityLogFormatter — Thu hồi hàng loạt (NCL-08-CN-011)', () => {
  it.each([
    ['CREATE_BULK_RECALL_REQUEST', 'Tạo yêu cầu thu hồi hàng loạt'],
    ['APPROVE_BULK_RECALL_REQUEST', 'Phê duyệt yêu cầu thu hồi hàng loạt'],
    ['REJECT_BULK_RECALL_REQUEST', 'Từ chối yêu cầu thu hồi hàng loạt'],
  ])('TC-01/02/03: %s → "%s" thay vì raw code', (action, expected) => {
    expect(formatActionType(action)).toBe(expected);
    // Nguyên tắc UI language: không bao giờ hiển thị technical code cho người dùng cuối
    expect(formatActionType(action)).not.toBe(action);
  });

  it('TC-04: objectType "bulk_recall_request" → "Yêu cầu thu hồi hàng loạt"', () => {
    // Giá trị DB lưu in thường, hàm chuẩn hóa toUpperCase() trước khi so khớp
    expect(formatTargetType('bulk_recall_request')).toBe(
      'Yêu cầu thu hồi hàng loạt'
    );
  });

  it('TC-05: dữ liệu lịch sử DB chứa raw code vẫn dịch được (backward compatibility)', () => {
    // Action bị normalize qua toUpperCase() nên chữ thường/vẫn trong trường hợp bất kỳ cũng map được
    expect(formatActionType('create_bulk_recall_request')).toBe(
      'Tạo yêu cầu thu hồi hàng loạt'
    );
    expect(formatTargetType('BULK_RECALL_REQUEST')).toBe(
      'Yêu cầu thu hồi hàng loạt'
    );
  });

  it('TC-06: regression — các action/objectType cũ không bị ảnh hưởng', () => {
    // Recall thường (không phải bulk) giữ nguyên wording cũ
    expect(formatActionType('CREATE_RECALL_REQUEST')).toBe(
      'Tạo yêu cầu thu hồi'
    );
    expect(formatTargetType('RECALL_REQUEST')).toBe('Yêu cầu thu hồi');
    // Các action/objectType hiện có khác
    expect(formatActionType('CREATE')).toBe('Tạo mới');
    expect(formatActionType('RECALL')).toBe('Thu hồi lô');
    expect(formatTargetType('PRODUCTION_LOT')).toBe('Lô sản xuất');
    expect(formatTargetType('SHIPMENT')).toBe('Lô hàng');
  });

  it('TC-07: cả 3 action bulk recall không bao giờ trả về raw technical value', () => {
    const bulkActions = [
      'CREATE_BULK_RECALL_REQUEST',
      'APPROVE_BULK_RECALL_REQUEST',
      'REJECT_BULK_RECALL_REQUEST',
    ];
    for (const action of bulkActions) {
      const label = formatActionType(action);
      expect(label).not.toBe(action);
      expect(label).not.toMatch(/BULK_RECALL_REQUEST/);
    }
    expect(formatTargetType('bulk_recall_request')).not.toMatch(/bulk_recall/i);
  });

  it('Màu badge tự theo tiền tố: CREATE/APPROVE → xanh, REJECT → đỏ', () => {
    expect(getActionColor('CREATE_BULK_RECALL_REQUEST')).toContain('emerald');
    expect(getActionColor('APPROVE_BULK_RECALL_REQUEST')).toContain('emerald');
    expect(getActionColor('REJECT_BULK_RECALL_REQUEST')).toContain('rose');
  });
});
