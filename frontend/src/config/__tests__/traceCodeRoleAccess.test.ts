import { describe, expect, it } from 'vitest';
import { hasAnyRole, ROLE_ACCESS } from '@/config/roleAccess';

describe('NCL-04-CN-008 - Quyền truy cập màn hình Xem trạng thái mã tem (ROLE_ACCESS.traceCodeView)', () => {
  it('Chỉ cho phép vai trò VT-02 (Quản lý hợp tác xã) truy cập', () => {
    expect(ROLE_ACCESS.traceCodeView).toEqual(['VT-02']);
    expect(hasAnyRole('VT-02', ROLE_ACCESS.traceCodeView)).toBe(true);
    expect(hasAnyRole('VT-01', ROLE_ACCESS.traceCodeView)).toBe(false);
    expect(hasAnyRole('VT-03', ROLE_ACCESS.traceCodeView)).toBe(false);
    expect(hasAnyRole('VT-04', ROLE_ACCESS.traceCodeView)).toBe(false);
    expect(hasAnyRole('VT-05', ROLE_ACCESS.traceCodeView)).toBe(false);
    expect(hasAnyRole(undefined, ROLE_ACCESS.traceCodeView)).toBe(false);
  });
});
