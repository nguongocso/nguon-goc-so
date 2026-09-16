import { renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { laThietBiDiDong, useIsMobileDevice } from '@/hooks/useIsMobileDevice';

const UA_IPHONE =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1';
const UA_ANDROID =
  'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36';
const UA_DESKTOP =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';

describe('laThietBiDiDong', () => {
  it('nhận diện iPhone và Android là mobile', () => {
    expect(laThietBiDiDong({ userAgent: UA_IPHONE })).toBe(true);
    expect(laThietBiDiDong({ userAgent: UA_ANDROID })).toBe(true);
  });

  it('tính desktop (kể cả màn hình cảm ứng) là không phải mobile', () => {
    expect(laThietBiDiDong({ userAgent: UA_DESKTOP })).toBe(false);
    expect(laThietBiDiDong({ userAgent: UA_DESKTOP, maxTouchPoints: 10 })).toBe(false);
  });

  it('ưu tiên userAgentData.mobile khi có', () => {
    expect(
      laThietBiDiDong({ userAgent: UA_DESKTOP, userAgentData: { mobile: true } }),
    ).toBe(true);
    expect(
      laThietBiDiDong({ userAgent: UA_IPHONE, userAgentData: { mobile: false } }),
    ).toBe(false);
  });

  it('trả về false khi không có thông tin trình duyệt', () => {
    expect(laThietBiDiDong({})).toBe(false);
  });
});

describe('useIsMobileDevice', () => {
  it('trả về boolean và không crash trong jsdom', () => {
    const { result } = renderHook(() => useIsMobileDevice());
    expect(typeof result.current).toBe('boolean');
  });
});
