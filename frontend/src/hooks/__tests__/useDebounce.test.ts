import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useDebounce } from '../useDebounce';

describe('useDebounce hook', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('trả về giá trị khởi tạo ngay lập tức', () => {
    const { result } = renderHook(() => useDebounce('initial', 400));
    expect(result.current).toBe('initial');
  });

  it('chưa cập nhật giá trị nếu chưa đủ thời gian 400ms', () => {
    const { result, rerender } = renderHook(({ value }) => useDebounce(value, 400), {
      initialProps: { value: 'first' },
    });

    rerender({ value: 'second' });
    expect(result.current).toBe('first');

    act(() => {
      vi.advanceTimersByTime(200);
    });
    expect(result.current).toBe('first');

    act(() => {
      vi.advanceTimersByTime(200);
    });
    expect(result.current).toBe('second');
  });

  it('chỉ cập nhật 1 lần với giá trị cuối khi người dùng gõ nhanh liên tục', () => {
    const { result, rerender } = renderHook(({ value }) => useDebounce(value, 400), {
      initialProps: { value: '2' },
    });

    rerender({ value: '20' });
    act(() => {
      vi.advanceTimersByTime(100);
    });

    rerender({ value: '202' });
    act(() => {
      vi.advanceTimersByTime(100);
    });

    rerender({ value: '2026' });
    act(() => {
      vi.advanceTimersByTime(100);
    });

    rerender({ value: '2026-09' });
    act(() => {
      vi.advanceTimersByTime(100);
    });

    rerender({ value: '2026-09-24' });
    expect(result.current).toBe('2');

    act(() => {
      vi.advanceTimersByTime(400);
    });
    expect(result.current).toBe('2026-09-24');
  });
});
