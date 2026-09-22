import { describe, it, expect } from 'vitest';
import { ROLE_ACCESS, hasAnyRole } from '@/config/roleAccess';

describe('CreateTestPartnerApiKey Route Protection (NCL-12-CN-004 / TC-04)', () => {
  it('allows VT-01 (Admin) and VT-02 (Cooperative Manager) to access apiKeyManagement routes', () => {
    // VT-01: Admin
    expect(hasAnyRole('VT-01', ROLE_ACCESS.apiKeyManagement)).toBe(true);

    // VT-02: Quản lý Hợp tác xã
    expect(hasAnyRole('VT-02', ROLE_ACCESS.apiKeyManagement)).toBe(true);
  });

  it('blocks VT-03 (Event Recorder) from accessing apiKeyManagement routes', () => {
    // VT-03: Người ghi sự kiện
    expect(hasAnyRole('VT-03', ROLE_ACCESS.apiKeyManagement)).toBe(false);
  });

  it('blocks VT-04 (Purchasing Enterprise / Buyer) from accessing apiKeyManagement routes', () => {
    // VT-04: Doanh nghiệp thu mua
    expect(hasAnyRole('VT-04', ROLE_ACCESS.apiKeyManagement)).toBe(false);
  });

  it('blocks VT-05 (Inspector) and VT-06 (Consumer) from accessing apiKeyManagement routes', () => {
    // VT-05: Đơn vị kiểm nghiệm
    expect(hasAnyRole('VT-05', ROLE_ACCESS.apiKeyManagement)).toBe(false);

    // VT-06: Người tiêu dùng
    expect(hasAnyRole('VT-06', ROLE_ACCESS.apiKeyManagement)).toBe(false);
  });
});
